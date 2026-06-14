package com.mhss.app.tracking.presentation.analytics

import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.model.TrackingDailySummary
import com.mhss.app.tracking.analytics.model.TrackingOptionDistribution
import com.mhss.app.tracking.analytics.model.TrackingSeries
import com.mhss.app.tracking.analytics.model.TrackingStreak
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class TrackingAnalyticsRange {
    DAY,
    WEEK,
    MONTH;

    fun dateRange(asOfDate: LocalDate): TrackingAnalyticsDateRange = when (this) {
        DAY -> TrackingAnalyticsDateRange(
            startDate = asOfDate.minus(364, DateTimeUnit.DAY),
            endDateExclusive = asOfDate.plus(1, DateTimeUnit.DAY)
        )

        WEEK -> TrackingAnalyticsDateRange(
            startDate = asOfDate.minus(364, DateTimeUnit.DAY),
            endDateExclusive = asOfDate.plus(1, DateTimeUnit.DAY)
        )

        MONTH -> {
            val currentMonth = LocalDate(asOfDate.year, asOfDate.month, 1)
            TrackingAnalyticsDateRange(
                startDate = currentMonth.minus(11, DateTimeUnit.MONTH),
                endDateExclusive = currentMonth.plus(1, DateTimeUnit.MONTH)
            )
        }
    }
}

enum class TrackingAnalyticsChartType {
    LINE,
    BAR
}

data class TrackingAnalyticsDateRange(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate
)

data class TrackingAnalyticsTrackerOption(
    val trackerId: String,
    val templateId: String,
    val templateName: String,
    val trackerName: String,
    val trackerType: TrackerType,
    val unit: String?
) {
    val displayName: String
        get() = "$templateName - $trackerName"
}

data class TrackingAnalyticsRequest(
    val tracker: TrackingAnalyticsTrackerOption,
    val range: TrackingAnalyticsRange,
    val aggregation: AggregationOperation,
    val asOfDate: LocalDate
)

data class TrackingAnalyticsData(
    val dailySummary: TrackingDailySummary?,
    val series: TrackingSeries?,
    val distribution: TrackingOptionDistribution?,
    val currentStreak: TrackingStreak?,
    val longestStreak: TrackingStreak?
)

data class TrackingAnalyticsUiState(
    val isLoading: Boolean = true,
    val trackerOptions: List<TrackingAnalyticsTrackerOption> = emptyList(),
    val selectedTracker: TrackingAnalyticsTrackerOption? = null,
    val range: TrackingAnalyticsRange = TrackingAnalyticsRange.DAY,
    val aggregation: AggregationOperation = AggregationOperation.SUM,
    val chartType: TrackingAnalyticsChartType = TrackingAnalyticsChartType.LINE,
    val data: TrackingAnalyticsData? = null,
    val loadFailed: Boolean = false
)

internal sealed interface TrackingAnalyticsLoadState {
    data object Idle : TrackingAnalyticsLoadState
    data object Loading : TrackingAnalyticsLoadState
    data class Ready(val data: TrackingAnalyticsData) : TrackingAnalyticsLoadState
    data object Failed : TrackingAnalyticsLoadState
}
