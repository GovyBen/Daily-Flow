package com.mhss.app.tracking.analytics.usecase

import com.mhss.app.tracking.analytics.aggregation.AggregationFactory
import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.aggregation.AggregationPreferences
import com.mhss.app.tracking.analytics.aggregation.FixedBinSize
import com.mhss.app.tracking.analytics.aggregation.TimeBin
import com.mhss.app.tracking.analytics.aggregation.TimeBinHelper
import com.mhss.app.tracking.analytics.model.TrackingAnalyticsSource
import com.mhss.app.tracking.analytics.model.TrackingDailySummary
import com.mhss.app.tracking.analytics.model.TrackingOptionDistribution
import com.mhss.app.tracking.analytics.model.TrackingOptionDistributionItem
import com.mhss.app.tracking.analytics.model.TrackingSeries
import com.mhss.app.tracking.analytics.model.TrackingSeriesPoint
import com.mhss.app.tracking.analytics.model.TrackingStreak
import com.mhss.app.tracking.analytics.repository.TrackingAnalyticsRepository
import com.mhss.app.tracking.analytics.sampling.BooleanSampleMode
import com.mhss.app.tracking.analytics.sampling.IDataPoint
import com.mhss.app.tracking.analytics.sampling.TrackingDataSampleAdapter
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Factory
import java.util.SortedSet
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Factory
class TrackingAnalyticsQueryEngine(
    private val repository: TrackingAnalyticsRepository,
    private val adapter: TrackingDataSampleAdapter
) {

    suspend fun dailySummary(
        trackerId: String,
        date: LocalDate,
        preferences: AggregationPreferences,
        booleanMode: BooleanSampleMode
    ): TrackingDailySummary? {
        val range = dayRange(date, preferences)
        val source = repository.getTrackerData(
            trackerId,
            range.startInclusive.toEpochMilliseconds(),
            range.endExclusive.toEpochMilliseconds()
        ) ?: return null
        val values = source.sample(booleanMode).map(IDataPoint::value).toList()
        return TrackingDailySummary(
            trackerId = source.trackerId,
            trackerName = source.trackerName,
            trackerType = source.trackerType,
            unit = source.unit,
            isTrackerActive = source.isActive,
            date = date,
            sampleCount = values.size,
            sum = values.sum(),
            average = if (values.isEmpty()) null else values.average(),
            minimum = values.minOrNull(),
            maximum = values.maxOrNull()
        )
    }

    suspend fun series(
        trackerId: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        operation: AggregationOperation,
        binSize: FixedBinSize,
        preferences: AggregationPreferences,
        booleanMode: BooleanSampleMode
    ): TrackingSeries? {
        require(startDate < endDateExclusive) {
            "Series date range must have positive duration"
        }
        val timeBinHelper = TimeBinHelper(preferences)
        val requestedStart = localBoundary(startDate, preferences)
        val requestedEnd = localBoundary(endDateExclusive, preferences)
        val firstBin = timeBinHelper.binContaining(requestedStart, binSize)
        val finalBin = timeBinHelper.binContaining(
            requestedEnd - 1.milliseconds,
            binSize
        )
        val bins = buildBins(firstBin, finalBin, timeBinHelper, binSize)
        val source = repository.getTrackerData(
            trackerId,
            firstBin.startInclusive.toEpochMilliseconds(),
            finalBin.endExclusive.toEpochMilliseconds()
        ) ?: return null
        val sample = source.sample(booleanMode)
        val valueByStart = AggregationFactory.fixed(
            binSize = binSize,
            operation = operation,
            preferences = preferences
        ).mapSample(sample).associateByBinStart(timeBinHelper, binSize)
        val countByStart = AggregationFactory.fixed(
            binSize = binSize,
            operation = AggregationOperation.COUNT,
            preferences = preferences
        ).mapSample(sample).associateByBinStart(timeBinHelper, binSize)

        return TrackingSeries(
            trackerId = source.trackerId,
            trackerName = source.trackerName,
            trackerType = source.trackerType,
            unit = source.unit,
            isTrackerActive = source.isActive,
            binSize = binSize,
            points = bins.map { bin ->
                val key = bin.startInclusive.toEpochMilliseconds()
                val aggregate = valueByStart[key]
                TrackingSeriesPoint(
                    startDate = bin.startInclusive
                        .toLocalDateTime(preferences.timeZone)
                        .date,
                    endDateExclusive = bin.endExclusive
                        .toLocalDateTime(preferences.timeZone)
                        .date,
                    startEpochMilli = key,
                    endEpochMilliExclusive = bin.endExclusive.toEpochMilliseconds(),
                    value = aggregate?.value
                        ?.takeUnless(Double::isNaN)
                        ?: operation.emptyValue(),
                    sampleCount = countByStart[key]?.value?.toInt() ?: 0,
                    label = aggregate?.label.orEmpty()
                )
            }
        )
    }

    suspend fun optionDistribution(
        trackerId: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        preferences: AggregationPreferences
    ): TrackingOptionDistribution? {
        require(startDate < endDateExclusive) {
            "Distribution date range must have positive duration"
        }
        val source = repository.getTrackerData(
            trackerId,
            localBoundary(startDate, preferences).toEpochMilliseconds(),
            localBoundary(endDateExclusive, preferences).toEpochMilliseconds()
        ) ?: return null
        val grouped = adapter.adapt(
            trackerId = source.trackerId,
            trackerType = source.trackerType,
            points = source.points
        ).samplesByLabel
        val counts = grouped.mapValues { (_, sample) -> sample.count() }
        val total = counts.values.sum()
        return TrackingOptionDistribution(
            trackerId = source.trackerId,
            trackerName = source.trackerName,
            isTrackerActive = source.isActive,
            totalSelections = total,
            items = counts.map { (label, count) ->
                TrackingOptionDistributionItem(
                    label = label,
                    count = count,
                    fraction = if (total == 0) 0.0 else count.toDouble() / total
                )
            }.sortedWith(
                compareByDescending<TrackingOptionDistributionItem> { it.count }
                    .thenBy(TrackingOptionDistributionItem::label)
            )
        )
    }

    suspend fun currentStreak(
        trackerId: String,
        asOfDate: LocalDate,
        preferences: AggregationPreferences
    ): TrackingStreak? {
        val source = streakSource(trackerId, asOfDate, preferences) ?: return null
        val dates = source.activeDates(asOfDate, preferences)
        val latest = dates.lastOrNull() ?: return source.streak()
        val yesterday = asOfDate.minus(1, DateTimeUnit.DAY)
        if (latest != asOfDate && latest != yesterday) {
            return source.streak()
        }
        var start = latest
        var length = 1
        while (start.minus(1, DateTimeUnit.DAY) in dates) {
            start = start.minus(1, DateTimeUnit.DAY)
            length += 1
        }
        return source.streak(length, start, latest)
    }

    suspend fun longestStreak(
        trackerId: String,
        asOfDate: LocalDate,
        preferences: AggregationPreferences
    ): TrackingStreak? {
        val source = streakSource(trackerId, asOfDate, preferences) ?: return null
        val dates = source.activeDates(asOfDate, preferences).toList()
        if (dates.isEmpty()) return source.streak()

        var bestStart = dates.first()
        var bestEnd = dates.first()
        var bestLength = 1
        var runStart = dates.first()
        var runLength = 1
        var previous = dates.first()
        dates.drop(1).forEach { date ->
            if (date == previous.plus(1, DateTimeUnit.DAY)) {
                previous = date
                runLength += 1
            } else {
                if (runLength > bestLength) {
                    bestStart = runStart
                    bestEnd = previous
                    bestLength = runLength
                }
                runStart = date
                previous = date
                runLength = 1
            }
        }
        if (runLength > bestLength) {
            bestStart = runStart
            bestEnd = previous
            bestLength = runLength
        }
        return source.streak(
            length = bestLength,
            startDate = bestStart,
            endDate = bestEnd
        )
    }

    private suspend fun streakSource(
        trackerId: String,
        asOfDate: LocalDate,
        preferences: AggregationPreferences
    ) = repository.getTrackerData(
        trackerId = trackerId,
        startInclusive = Long.MIN_VALUE,
        endExclusive = localBoundary(
            asOfDate.plus(1, DateTimeUnit.DAY),
            preferences
        ).toEpochMilliseconds()
    )

    private fun TrackingAnalyticsSource.sample(booleanMode: BooleanSampleMode) =
        adapter.adapt(
            trackerId = trackerId,
            trackerType = trackerType,
            points = points,
            booleanMode = booleanMode
        ).sample

    private fun TrackingAnalyticsSource.activeDates(
        asOfDate: LocalDate,
        preferences: AggregationPreferences
    ): SortedSet<LocalDate> {
        val booleanMode = if (trackerType == TrackerType.BOOLEAN) {
            BooleanSampleMode.TRUE_COUNT
        } else {
            BooleanSampleMode.TRUE_RATIO
        }
        return sample(booleanMode)
            .map { point -> point.timestamp.effectiveDate(preferences) }
            .filter { it <= asOfDate }
            .toSortedSet()
    }

    private fun TrackingAnalyticsSource.streak(
        length: Int = 0,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) = TrackingStreak(
        trackerId = trackerId,
        trackerName = trackerName,
        isTrackerActive = isActive,
        length = length,
        startDate = startDate,
        endDate = endDate
    )
}

private fun dayRange(
    date: LocalDate,
    preferences: AggregationPreferences
) = TimeBin(
    startInclusive = localBoundary(date, preferences),
    endExclusive = localBoundary(date.plus(1, DateTimeUnit.DAY), preferences)
)

private fun localBoundary(
    date: LocalDate,
    preferences: AggregationPreferences
): Instant = date.atTime(preferences.startTimeOfDay).toInstant(preferences.timeZone)

private fun buildBins(
    first: TimeBin,
    last: TimeBin,
    helper: TimeBinHelper,
    binSize: FixedBinSize
): List<TimeBin> = buildList {
    var current = first
    while (current.startInclusive <= last.startInclusive) {
        add(current)
        current = helper.binContaining(current.endExclusive, binSize)
    }
}

private fun com.mhss.app.tracking.analytics.sampling.DataSample.associateByBinStart(
    helper: TimeBinHelper,
    binSize: FixedBinSize
): Map<Long, IDataPoint> = associateBy { point ->
    helper.binContaining(point.timestamp, binSize)
        .startInclusive
        .toEpochMilliseconds()
}

private fun AggregationOperation.emptyValue(): Double? = when (this) {
    AggregationOperation.SUM,
    AggregationOperation.COUNT -> 0.0
    AggregationOperation.AVERAGE,
    AggregationOperation.MIN,
    AggregationOperation.MAX -> null
}

private fun Instant.effectiveDate(
    preferences: AggregationPreferences
): LocalDate {
    val local = toLocalDateTime(preferences.timeZone)
    return if (local.time < preferences.startTimeOfDay) {
        local.date.minus(1, DateTimeUnit.DAY)
    } else {
        local.date
    }
}
