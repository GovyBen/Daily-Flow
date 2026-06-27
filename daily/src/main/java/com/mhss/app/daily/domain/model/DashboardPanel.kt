package com.mhss.app.daily.domain.model

import kotlinx.serialization.Serializable

data class DashboardPanel(
    val id: String,
    val type: DashboardPanelType,
    val enabled: Boolean,
    val displayOrder: Int,
    val size: DashboardPanelSize,
    val config: DashboardPanelConfig,
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long
)

enum class DashboardPanelType {
    QUICK_RECORD,
    DAILY_ITEMS,
    OVERDUE_ITEMS,
    PENDING_REMINDERS,
    TRACKING_SUMMARY,
    TRACKING_TRENDS,
    POMODORO,
    AI_ASSISTANT,
    CALENDAR_SYNC_STATUS
}

enum class DashboardPanelSize {
    COMPACT,
    MEDIUM,
    LARGE
}

sealed interface DashboardPanelConfig

@Serializable
data class DailyItemsPanelConfig(
    val range: DailyItemRangePreset = DailyItemRangePreset.TODAY,
    val maxItems: Int = 5,
    val showCompleted: Boolean = false,
    val showSyncState: Boolean = true
) : DashboardPanelConfig

@Serializable
data class QuickRecordPanelConfig(
    val templateIds: List<String> = emptyList(),
    val maxTemplates: Int = 4
) : DashboardPanelConfig

@Serializable
data class TrackingSummaryPanelConfig(
    val dateRange: TrackingDashboardRange = TrackingDashboardRange.TODAY,
    val maxRows: Int = 4,
    val showSparkline: Boolean = true
) : DashboardPanelConfig

@Serializable
data class PendingRemindersPanelConfig(
    val maxRows: Int = 5,
    val includeDelivered: Boolean = false
) : DashboardPanelConfig

@Serializable
data class EmptyDashboardPanelConfig(
    val version: Int = 1
) : DashboardPanelConfig

enum class DailyItemRangePreset {
    TODAY,
    SURROUNDING_SEVEN_DAYS,
    FUTURE_SEVEN_DAYS,
    THIS_WEEK,
    THIS_MONTH,
    OVERDUE,
    NO_DATE,
    COMPLETED,
    ALL
}

enum class TrackingDashboardRange {
    TODAY,
    SEVEN_DAYS,
    THIRTY_DAYS
}

fun DailyItemRangePreset.toDailyItemRange(): DailyItemRange = when (this) {
    DailyItemRangePreset.TODAY -> DailyItemRange.Today
    DailyItemRangePreset.SURROUNDING_SEVEN_DAYS -> DailyItemRange.SurroundingSevenDays
    DailyItemRangePreset.FUTURE_SEVEN_DAYS -> DailyItemRange.FutureSevenDays
    DailyItemRangePreset.THIS_WEEK -> DailyItemRange.ThisWeek
    DailyItemRangePreset.THIS_MONTH -> DailyItemRange.ThisMonth
    DailyItemRangePreset.OVERDUE -> DailyItemRange.Overdue
    DailyItemRangePreset.NO_DATE -> DailyItemRange.NoDate
    DailyItemRangePreset.COMPLETED -> DailyItemRange.Completed
    DailyItemRangePreset.ALL -> DailyItemRange.All
}

fun DashboardPanelType.defaultConfig(): DashboardPanelConfig = when (this) {
    DashboardPanelType.DAILY_ITEMS -> DailyItemsPanelConfig()
    DashboardPanelType.OVERDUE_ITEMS -> DailyItemsPanelConfig(
        range = DailyItemRangePreset.OVERDUE,
        maxItems = 4
    )
    DashboardPanelType.QUICK_RECORD -> QuickRecordPanelConfig()
    DashboardPanelType.TRACKING_SUMMARY,
    DashboardPanelType.TRACKING_TRENDS -> TrackingSummaryPanelConfig()
    DashboardPanelType.PENDING_REMINDERS -> PendingRemindersPanelConfig()
    DashboardPanelType.POMODORO,
    DashboardPanelType.AI_ASSISTANT,
    DashboardPanelType.CALENDAR_SYNC_STATUS -> EmptyDashboardPanelConfig()
}
