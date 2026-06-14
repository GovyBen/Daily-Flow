package com.mhss.app.tracking.presentation.analytics

import com.mhss.app.tracking.analytics.aggregation.AggregationPreferences
import com.mhss.app.tracking.analytics.usecase.GetCurrentStreakUseCase
import com.mhss.app.tracking.analytics.usecase.GetDailySeriesUseCase
import com.mhss.app.tracking.analytics.usecase.GetDailySummaryUseCase
import com.mhss.app.tracking.analytics.usecase.GetLongestStreakUseCase
import com.mhss.app.tracking.analytics.usecase.GetMonthlySeriesUseCase
import com.mhss.app.tracking.analytics.usecase.GetOptionDistributionUseCase
import com.mhss.app.tracking.analytics.usecase.GetWeeklySeriesUseCase
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory
class TrackingAnalyticsLoader(
    private val getDailySummary: GetDailySummaryUseCase,
    private val getDailySeries: GetDailySeriesUseCase,
    private val getWeeklySeries: GetWeeklySeriesUseCase,
    private val getMonthlySeries: GetMonthlySeriesUseCase,
    private val getOptionDistribution: GetOptionDistributionUseCase,
    private val getCurrentStreak: GetCurrentStreakUseCase,
    private val getLongestStreak: GetLongestStreakUseCase,
    @Named("defaultDispatcher") private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun load(request: TrackingAnalyticsRequest): TrackingAnalyticsData =
        withContext(defaultDispatcher) {
            coroutineScope {
                val preferences = AggregationPreferences()
                val dates = request.range.dateRange(request.asOfDate)
                val summary = async {
                    getDailySummary(
                        trackerId = request.tracker.trackerId,
                        date = request.asOfDate,
                        preferences = preferences
                    )
                }
                val series = async {
                    when (request.range) {
                        TrackingAnalyticsRange.DAY -> getDailySeries(
                            trackerId = request.tracker.trackerId,
                            startDate = dates.startDate,
                            endDateExclusive = dates.endDateExclusive,
                            operation = request.aggregation,
                            preferences = preferences
                        )

                        TrackingAnalyticsRange.WEEK -> getWeeklySeries(
                            trackerId = request.tracker.trackerId,
                            startDate = dates.startDate,
                            endDateExclusive = dates.endDateExclusive,
                            operation = request.aggregation,
                            preferences = preferences
                        )

                        TrackingAnalyticsRange.MONTH -> getMonthlySeries(
                            trackerId = request.tracker.trackerId,
                            startDate = dates.startDate,
                            endDateExclusive = dates.endDateExclusive,
                            operation = request.aggregation,
                            preferences = preferences
                        )
                    }
                }
                val distribution = async {
                    if (
                        request.tracker.trackerType == TrackerType.SINGLE_SELECT ||
                        request.tracker.trackerType == TrackerType.MULTI_SELECT
                    ) {
                        getOptionDistribution(
                            trackerId = request.tracker.trackerId,
                            startDate = dates.startDate,
                            endDateExclusive = dates.endDateExclusive,
                            preferences = preferences
                        )
                    } else {
                        null
                    }
                }
                val currentStreak = async {
                    getCurrentStreak(
                        trackerId = request.tracker.trackerId,
                        asOfDate = request.asOfDate,
                        preferences = preferences
                    )
                }
                val longestStreak = async {
                    getLongestStreak(
                        trackerId = request.tracker.trackerId,
                        asOfDate = request.asOfDate,
                        preferences = preferences
                    )
                }

                TrackingAnalyticsData(
                    dailySummary = summary.await(),
                    series = series.await(),
                    distribution = distribution.await(),
                    currentStreak = currentStreak.await(),
                    longestStreak = longestStreak.await()
                )
            }
        }
}
