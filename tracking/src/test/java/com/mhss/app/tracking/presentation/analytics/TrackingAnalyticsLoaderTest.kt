package com.mhss.app.tracking.presentation.analytics

import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.model.TrackingAnalyticsSource
import com.mhss.app.tracking.analytics.repository.TrackingAnalyticsRepository
import com.mhss.app.tracking.analytics.sampling.TrackingDataSampleAdapter
import com.mhss.app.tracking.analytics.usecase.GetCurrentStreakUseCase
import com.mhss.app.tracking.analytics.usecase.GetDailySeriesUseCase
import com.mhss.app.tracking.analytics.usecase.GetDailySummaryUseCase
import com.mhss.app.tracking.analytics.usecase.GetLongestStreakUseCase
import com.mhss.app.tracking.analytics.usecase.GetMonthlySeriesUseCase
import com.mhss.app.tracking.analytics.usecase.GetOptionDistributionUseCase
import com.mhss.app.tracking.analytics.usecase.GetWeeklySeriesUseCase
import com.mhss.app.tracking.analytics.usecase.TrackingAnalyticsQueryEngine
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingAnalyticsLoaderTest {

    @Test
    fun dayRangeBuildsOneYearSeriesWithoutBlockingCaller() = runBlocking {
        val asOfDate = LocalDate(2026, 6, 14)
        val startDate = asOfDate.plus(-364, DateTimeUnit.DAY)
        val points = List(365) { index ->
            val date = startDate.plus(index, DateTimeUnit.DAY)
            TrackingRecordedPoint(
                id = "point-$index",
                trackerId = TRACKER_ID,
                epochMilli = date.atTime(LocalTime(12, 0))
                    .toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
                utcOffsetSeconds = 0,
                value = index.toDouble(),
                label = null,
                note = null,
                optionId = null
            )
        }
        val repository = ImmediateRepository(source(points))

        val result = withTimeout(3_000) {
            loader(repository).load(request(asOfDate))
        }

        assertEquals(365, result.series?.points?.size)
        assertEquals(365, result.series?.points?.sumOf { it.sampleCount })
        assertTrue(repository.threadNames.all { it != Thread.currentThread().name })
    }

    @Test
    fun cancellationStopsInFlightAnalyticsQueries() = runBlocking {
        val repository = SuspendingRepository()
        val job = launch {
            loader(repository).load(request(LocalDate(2026, 6, 14)))
        }

        repository.started.await()
        job.cancelAndJoin()

        assertTrue(repository.cancelledCalls.get() > 0)
    }

    private fun loader(repository: TrackingAnalyticsRepository): TrackingAnalyticsLoader {
        val engine = TrackingAnalyticsQueryEngine(repository, TrackingDataSampleAdapter())
        return TrackingAnalyticsLoader(
            getDailySummary = GetDailySummaryUseCase(engine),
            getDailySeries = GetDailySeriesUseCase(engine),
            getWeeklySeries = GetWeeklySeriesUseCase(engine),
            getMonthlySeries = GetMonthlySeriesUseCase(engine),
            getOptionDistribution = GetOptionDistributionUseCase(engine),
            getCurrentStreak = GetCurrentStreakUseCase(engine),
            getLongestStreak = GetLongestStreakUseCase(engine),
            defaultDispatcher = Dispatchers.Default
        )
    }

    private fun request(asOfDate: LocalDate) = TrackingAnalyticsRequest(
        tracker = TrackingAnalyticsTrackerOption(
            trackerId = TRACKER_ID,
            templateId = "template",
            templateName = "Health",
            trackerName = "Weight",
            trackerType = NumberConfig().trackerType,
            unit = "kg"
        ),
        range = TrackingAnalyticsRange.DAY,
        aggregation = AggregationOperation.SUM,
        asOfDate = asOfDate
    )

    private fun source(points: List<TrackingRecordedPoint>) = TrackingAnalyticsSource(
        trackerId = TRACKER_ID,
        trackerName = "Weight",
        trackerType = TrackerType.NUMBER,
        unit = "kg",
        isActive = true,
        points = points
    )

    private companion object {
        const val TRACKER_ID = "tracker"
    }
}

private class ImmediateRepository(
    private val source: TrackingAnalyticsSource
) : TrackingAnalyticsRepository {
    val threadNames = mutableListOf<String>()

    override suspend fun getTrackerData(
        trackerId: String,
        startInclusive: Long,
        endExclusive: Long
    ): TrackingAnalyticsSource {
        synchronized(threadNames) {
            threadNames += Thread.currentThread().name
        }
        return source.copy(
            points = source.points.filter {
                it.epochMilli >= startInclusive && it.epochMilli < endExclusive
            }
        )
    }
}

private class SuspendingRepository : TrackingAnalyticsRepository {
    val started = CompletableDeferred<Unit>()
    val cancelledCalls = AtomicInteger()

    override suspend fun getTrackerData(
        trackerId: String,
        startInclusive: Long,
        endExclusive: Long
    ): TrackingAnalyticsSource? {
        started.complete(Unit)
        try {
            awaitCancellation()
        } finally {
            cancelledCalls.incrementAndGet()
        }
    }
}
