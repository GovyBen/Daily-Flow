package com.mhss.app.daily.domain.model

data class DailyItemFilter(
    val range: DailyItemRange = DailyItemRange.Today,
    val status: DailyItemStatusFilter = DailyItemStatusFilter.Active,
    val includeNoDate: Boolean = false,
    val includeCompleted: Boolean = false,
    val syncState: CalendarSyncState? = null,
    val priority: DailyItemPriority? = null,
    val query: String = ""
)

enum class DailyItemStatusFilter {
    Active,
    Completed,
    ActiveAndCompleted,
    Any
}
