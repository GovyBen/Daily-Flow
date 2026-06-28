package com.mhss.app.daily.presentation

import androidx.annotation.StringRes
import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItemKind
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.ui.R

@StringRes
internal fun DailyItemKind.labelRes(): Int = when (this) {
    DailyItemKind.TASK -> R.string.daily_item_kind_task
    DailyItemKind.EVENT -> R.string.daily_item_kind_event
    DailyItemKind.PLAN -> R.string.daily_item_kind_plan
}

@StringRes
internal fun DailyItemStatus.labelRes(): Int = when (this) {
    DailyItemStatus.ACTIVE -> R.string.daily_item_status_active
    DailyItemStatus.COMPLETED -> R.string.daily_item_status_completed
    DailyItemStatus.CANCELLED -> R.string.daily_item_status_cancelled
    DailyItemStatus.ARCHIVED -> R.string.daily_item_status_archived
}

@StringRes
internal fun DailyItemPriority.labelRes(): Int = when (this) {
    DailyItemPriority.LOW -> R.string.daily_item_priority_low
    DailyItemPriority.MEDIUM -> R.string.daily_item_priority_medium
    DailyItemPriority.HIGH -> R.string.daily_item_priority_high
}

@StringRes
internal fun CalendarSyncState.labelRes(): Int = when (this) {
    CalendarSyncState.NOT_SYNCED -> R.string.daily_item_sync_not_synced
    CalendarSyncState.SYNCED -> R.string.daily_item_sync_synced
    CalendarSyncState.DIRTY -> R.string.daily_item_sync_dirty
    CalendarSyncState.FAILED -> R.string.daily_item_sync_failed
    CalendarSyncState.UNLINKED -> R.string.daily_item_sync_unlinked
    CalendarSyncState.EXTERNAL_DRIFT -> R.string.daily_item_sync_external_drift
}
