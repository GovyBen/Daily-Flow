package com.mhss.app.tracking.analytics.usecase

import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.aggregation.AggregationPreferences
import com.mhss.app.tracking.analytics.aggregation.FixedBinSize
import com.mhss.app.tracking.analytics.sampling.BooleanSampleMode
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Factory

@Factory
class GetDailySummaryUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        date: LocalDate,
        preferences: AggregationPreferences = AggregationPreferences(),
        booleanMode: BooleanSampleMode = BooleanSampleMode.TRUE_RATIO
    ) = engine.dailySummary(trackerId, date, preferences, booleanMode)
}

@Factory
class GetDailySeriesUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        operation: AggregationOperation,
        preferences: AggregationPreferences = AggregationPreferences(),
        booleanMode: BooleanSampleMode = BooleanSampleMode.TRUE_RATIO
    ) = engine.series(
        trackerId = trackerId,
        startDate = startDate,
        endDateExclusive = endDateExclusive,
        operation = operation,
        binSize = FixedBinSize.DAY,
        preferences = preferences,
        booleanMode = booleanMode
    )
}

@Factory
class GetWeeklySeriesUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        operation: AggregationOperation,
        preferences: AggregationPreferences = AggregationPreferences(),
        booleanMode: BooleanSampleMode = BooleanSampleMode.TRUE_RATIO
    ) = engine.series(
        trackerId = trackerId,
        startDate = startDate,
        endDateExclusive = endDateExclusive,
        operation = operation,
        binSize = FixedBinSize.WEEK,
        preferences = preferences,
        booleanMode = booleanMode
    )
}

@Factory
class GetMonthlySeriesUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        operation: AggregationOperation,
        preferences: AggregationPreferences = AggregationPreferences(),
        booleanMode: BooleanSampleMode = BooleanSampleMode.TRUE_RATIO
    ) = engine.series(
        trackerId = trackerId,
        startDate = startDate,
        endDateExclusive = endDateExclusive,
        operation = operation,
        binSize = FixedBinSize.MONTH,
        preferences = preferences,
        booleanMode = booleanMode
    )
}

@Factory
class GetOptionDistributionUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        preferences: AggregationPreferences = AggregationPreferences()
    ) = engine.optionDistribution(
        trackerId,
        startDate,
        endDateExclusive,
        preferences
    )
}

@Factory
class GetCurrentStreakUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        asOfDate: LocalDate,
        preferences: AggregationPreferences = AggregationPreferences()
    ) = engine.currentStreak(trackerId, asOfDate, preferences)
}

@Factory
class GetLongestStreakUseCase(
    private val engine: TrackingAnalyticsQueryEngine
) {
    suspend operator fun invoke(
        trackerId: String,
        asOfDate: LocalDate,
        preferences: AggregationPreferences = AggregationPreferences()
    ) = engine.longestStreak(trackerId, asOfDate, preferences)
}
