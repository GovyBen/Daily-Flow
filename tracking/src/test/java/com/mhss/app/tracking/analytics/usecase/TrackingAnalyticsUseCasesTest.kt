package com.mhss.app.tracking.analytics.usecase

import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.aggregation.AggregationPreferences
import com.mhss.app.tracking.analytics.model.TrackingAnalyticsSource
import com.mhss.app.tracking.analytics.repository.TrackingAnalyticsRepository
import com.mhss.app.tracking.analytics.sampling.TrackingDataSampleAdapter
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingAnalyticsUseCasesTest {

    @Test
    fun dailySummaryUsesDstSafeLocalDayAndReturnsAllMetrics() = runBlocking {
        val zone = TimeZone.of("America/New_York")
        val repository = FakeTrackingAnalyticsRepository(
            source(
                type = TrackerType.NUMBER,
                points = listOf(
                    point("start", at(2026, 3, 8, 0, zone), 1.0),
                    point("late", at(2026, 3, 8, 23, zone), 2.0),
                    point("next", at(2026, 3, 9, 0, zone), 100.0)
                )
            )
        )

        val summary = useCases(repository).daily(
            trackerId = TRACKER_ID,
            date = LocalDate(2026, 3, 8),
            preferences = AggregationPreferences(timeZone = zone)
        )

        requireNotNull(summary)
        assertEquals(2, summary.sampleCount)
        assertEquals(3.0, summary.sum, 0.0)
        assertEquals(1.5, summary.average!!, 0.0)
        assertEquals(1.0, summary.minimum!!, 0.0)
        assertEquals(2.0, summary.maximum!!, 0.0)
        assertEquals(
            23L * 60 * 60 * 1_000,
            repository.lastEndExclusive!! - repository.lastStartInclusive!!
        )
    }

    @Test
    fun emptyDailySummaryReturnsZeroCountAndNullableMetrics() = runBlocking {
        val summary = useCases(
            FakeTrackingAnalyticsRepository(source(points = emptyList()))
        ).daily(
            trackerId = TRACKER_ID,
            date = LocalDate(2026, 6, 14),
            preferences = utcPreferences()
        )

        requireNotNull(summary)
        assertEquals(0, summary.sampleCount)
        assertEquals(0.0, summary.sum, 0.0)
        assertNull(summary.average)
        assertNull(summary.minimum)
        assertNull(summary.maximum)
    }

    @Test
    fun weeklySeriesAlignsPeriodsFillsGapsAndKeepsInactiveTrackerHistory() = runBlocking {
        val repository = FakeTrackingAnalyticsRepository(
            source(
                isActive = false,
                points = listOf(
                    point("jan", utcAt(2026, 1, 27), 2.0),
                    point("feb", utcAt(2026, 2, 10), 5.0)
                )
            )
        )

        val result = useCases(repository).weekly(
            trackerId = TRACKER_ID,
            startDate = LocalDate(2026, 1, 26),
            endDateExclusive = LocalDate(2026, 2, 16),
            operation = AggregationOperation.SUM,
            preferences = utcPreferences()
        )

        requireNotNull(result)
        assertFalse(result.isTrackerActive)
        assertEquals(
            listOf(
                LocalDate(2026, 1, 26),
                LocalDate(2026, 2, 2),
                LocalDate(2026, 2, 9)
            ),
            result.points.map { it.startDate }
        )
        assertEquals(listOf(2.0, 0.0, 5.0), result.points.map { it.value })
        assertEquals(listOf(1, 0, 1), result.points.map { it.sampleCount })
    }

    @Test
    fun monthlySeriesHandlesSinglePointAndCrossYearEmptyBin() = runBlocking {
        val result = useCases(
            FakeTrackingAnalyticsRepository(
                source(points = listOf(point("dec", utcAt(2025, 12, 31), 4.0)))
            )
        ).monthly(
            trackerId = TRACKER_ID,
            startDate = LocalDate(2025, 12, 1),
            endDateExclusive = LocalDate(2026, 2, 1),
            operation = AggregationOperation.AVERAGE,
            preferences = utcPreferences()
        )

        requireNotNull(result)
        assertEquals(
            listOf(LocalDate(2025, 12, 1), LocalDate(2026, 1, 1)),
            result.points.map { it.startDate }
        )
        assertEquals(4.0, result.points.first().value!!, 0.0)
        assertNull(result.points.last().value)
    }

    @Test
    fun optionDistributionUsesStoredLabelsAndSelectionCount() = runBlocking {
        val result = useCases(
            FakeTrackingAnalyticsRepository(
                source(
                    type = TrackerType.MULTI_SELECT,
                    points = listOf(
                        point("back-1", utcAt(2026, 6, 1), 1.0, "Back"),
                        point("chest", utcAt(2026, 6, 2), 4.0, "Chest"),
                        point("back-2", utcAt(2026, 6, 3), 2.0, "Back")
                    )
                )
            )
        ).distribution(
            trackerId = TRACKER_ID,
            startDate = LocalDate(2026, 6, 1),
            endDateExclusive = LocalDate(2026, 7, 1),
            preferences = utcPreferences()
        )

        requireNotNull(result)
        assertEquals(3, result.totalSelections)
        assertEquals(listOf("Back", "Chest"), result.items.map { it.label })
        assertEquals(listOf(2, 1), result.items.map { it.count })
        assertEquals(2.0 / 3.0, result.items.first().fraction, 0.0)
    }

    @Test
    fun booleanCurrentStreakCountsTrueDaysAndMayEndYesterday() = runBlocking {
        val points = listOf(
            point("11", utcAt(2026, 6, 11), 1.0),
            point("12", utcAt(2026, 6, 12), 1.0),
            point("13-false", utcAt(2026, 6, 13), 0.0),
            point("13", utcAt(2026, 6, 13, 8), 1.0),
            point("14", utcAt(2026, 6, 14), 1.0)
        )
        val result = useCases(
            FakeTrackingAnalyticsRepository(
                source(type = TrackerType.BOOLEAN, points = points)
            )
        ).currentStreak(
            trackerId = TRACKER_ID,
            asOfDate = LocalDate(2026, 6, 15),
            preferences = utcPreferences()
        )

        requireNotNull(result)
        assertEquals(4, result.length)
        assertEquals(LocalDate(2026, 6, 11), result.startDate)
        assertEquals(LocalDate(2026, 6, 14), result.endDate)
    }

    @Test
    fun longestStreakUsesCustomDayBoundaryAndReturnsLongestRun() = runBlocking {
        val preferences = AggregationPreferences(
            startTimeOfDay = LocalTime(5, 0),
            timeZone = TimeZone.UTC
        )
        val points = listOf(
            point("d1", utcAt(2026, 6, 2, 4), 1.0),
            point("d2", utcAt(2026, 6, 2, 6), 1.0),
            point("d3", utcAt(2026, 6, 3, 6), 1.0),
            point("d5", utcAt(2026, 6, 6, 4), 1.0),
            point("d6", utcAt(2026, 6, 6, 6), 1.0)
        )
        val result = useCases(
            FakeTrackingAnalyticsRepository(source(points = points))
        ).longestStreak(
            trackerId = TRACKER_ID,
            asOfDate = LocalDate(2026, 6, 6),
            preferences = preferences
        )

        requireNotNull(result)
        assertEquals(3, result.length)
        assertEquals(LocalDate(2026, 6, 1), result.startDate)
        assertEquals(LocalDate(2026, 6, 3), result.endDate)
    }

    private fun useCases(repository: TrackingAnalyticsRepository): UseCases {
        val engine = TrackingAnalyticsQueryEngine(
            repository = repository,
            adapter = TrackingDataSampleAdapter()
        )
        return UseCases(
            daily = GetDailySummaryUseCase(engine),
            weekly = GetWeeklySeriesUseCase(engine),
            monthly = GetMonthlySeriesUseCase(engine),
            distribution = GetOptionDistributionUseCase(engine),
            currentStreak = GetCurrentStreakUseCase(engine),
            longestStreak = GetLongestStreakUseCase(engine)
        )
    }

    private fun source(
        type: TrackerType = TrackerType.NUMBER,
        isActive: Boolean = true,
        points: List<TrackingRecordedPoint>
    ) = TrackingAnalyticsSource(
        trackerId = TRACKER_ID,
        trackerName = "Tracker",
        trackerType = type,
        unit = null,
        isActive = isActive,
        points = points
    )

    private fun point(
        id: String,
        epochMilli: Long,
        value: Double?,
        label: String? = null
    ) = TrackingRecordedPoint(
        id = id,
        trackerId = TRACKER_ID,
        epochMilli = epochMilli,
        utcOffsetSeconds = 0,
        value = value,
        label = label,
        note = null,
        optionId = null
    )

    private fun utcPreferences() = AggregationPreferences(timeZone = TimeZone.UTC)

    private fun utcAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12
    ) = at(year, month, day, hour, TimeZone.UTC)

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        zone: TimeZone
    ) = LocalDateTime(year, month, day, hour, 0)
        .toInstant(zone)
        .toEpochMilliseconds()

    private data class UseCases(
        val daily: GetDailySummaryUseCase,
        val weekly: GetWeeklySeriesUseCase,
        val monthly: GetMonthlySeriesUseCase,
        val distribution: GetOptionDistributionUseCase,
        val currentStreak: GetCurrentStreakUseCase,
        val longestStreak: GetLongestStreakUseCase
    )

    private companion object {
        const val TRACKER_ID = "tracker"
    }
}

private class FakeTrackingAnalyticsRepository(
    private val source: TrackingAnalyticsSource?
) : TrackingAnalyticsRepository {
    var lastStartInclusive: Long? = null
    var lastEndExclusive: Long? = null

    override suspend fun getTrackerData(
        trackerId: String,
        startInclusive: Long,
        endExclusive: Long
    ): TrackingAnalyticsSource? {
        lastStartInclusive = startInclusive
        lastEndExclusive = endExclusive
        return source?.copy(
            points = source.points.filter {
                it.epochMilli >= startInclusive && it.epochMilli < endExclusive
            }
        )
    }
}
