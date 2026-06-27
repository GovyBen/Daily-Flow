package com.mhss.app.daily.domain.model

import kotlinx.datetime.DayOfWeek

data class DailyItem(
    val id: String,
    val title: String,
    val description: String = "",
    val kind: DailyItemKind = DailyItemKind.TASK,
    val schedule: DailyItemSchedule = DailyItemSchedule(),
    val isCompletable: Boolean = true,
    val completedAtEpochMilli: Long? = null,
    val status: DailyItemStatus = DailyItemStatus.ACTIVE,
    val priority: DailyItemPriority = DailyItemPriority.LOW,
    val recurrence: DailyItemRecurrence? = null,
    val color: Long? = null,
    val calendarSync: DailyItemCalendarSync = DailyItemCalendarSync(),
    val legacySource: DailyItemLegacySource? = null,
    val subTasksJson: String = "[]",
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long
) {
    val primaryTimeEpochMilli: Long?
        get() = schedule.startAtEpochMilli ?: schedule.dueAtEpochMilli

    val isCompleted: Boolean
        get() = status == DailyItemStatus.COMPLETED
}

enum class DailyItemKind {
    TASK,
    EVENT,
    PLAN
}

enum class DailyItemStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    ARCHIVED
}

enum class DailyItemPriority {
    LOW,
    MEDIUM,
    HIGH
}

data class DailyItemRecurrence(
    val frequency: DailyItemFrequency,
    val interval: Int,
    val weekDays: Set<DayOfWeek> = emptySet()
)

enum class DailyItemFrequency {
    EVERY_MINUTES,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class DailyItemCalendarSync(
    val enabled: Boolean = false,
    val systemCalendarId: Long? = null,
    val systemEventId: Long? = null,
    val state: CalendarSyncState = CalendarSyncState.NOT_SYNCED,
    val lastSyncedAtEpochMilli: Long? = null,
    val lastLocalFingerprint: String? = null,
    val lastProviderFingerprint: String? = null,
    val lastError: String? = null
)

enum class CalendarSyncState {
    NOT_SYNCED,
    SYNCED,
    DIRTY,
    FAILED,
    UNLINKED,
    EXTERNAL_DRIFT
}

data class DailyItemLegacySource(
    val type: DailyItemLegacySourceType,
    val id: String
)

enum class DailyItemLegacySourceType {
    TASK,
    CALENDAR_PROVIDER_EVENT,
    MANUAL
}
