package com.mhss.app.daily.domain.usecase

import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.use_case.RescheduleTargetRemindersUseCase
import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemCalendarSync
import com.mhss.app.daily.domain.model.DailyItemFilter
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.daily.domain.validation.DailyItemValidator
import com.mhss.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveDailyItemsUseCase(
    private val repository: DailyItemRepository
) {
    operator fun invoke(filter: DailyItemFilter = DailyItemFilter()): Flow<List<DailyItem>> {
        return repository.observeItems(filter)
    }
}

class GetDailyItemUseCase(
    private val repository: DailyItemRepository
) {
    suspend operator fun invoke(id: String): DailyItem? = repository.getItem(id)
}

class SearchDailyItemsUseCase(
    private val repository: DailyItemRepository
) {
    suspend operator fun invoke(query: String): List<DailyItem> = repository.searchItems(query)
}

class CreateDailyItemUseCase(
    private val repository: DailyItemRepository,
    private val syncDailyItemToCalendar: SyncDailyItemToCalendarUseCase,
    private val widgetUpdater: WidgetUpdater,
    private val validator: DailyItemValidator
) {
    suspend operator fun invoke(item: DailyItem): DailyItem {
        val now = System.currentTimeMillis()
        val withId = item.copy(
            id = item.id.ifBlank { UUID.randomUUID().toString() },
            createdAtEpochMilli = item.createdAtEpochMilli.takeIf { it > 0 } ?: now,
            updatedAtEpochMilli = maxOf(item.updatedAtEpochMilli, now)
        )
        validator.requireValid(withId)
        val saved = repository.upsert(withId)
        if (saved.calendarSync.enabled) syncDailyItemToCalendar(saved.id)
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
        return repository.getItem(saved.id) ?: saved
    }
}

class UpdateDailyItemUseCase(
    private val repository: DailyItemRepository,
    private val syncDailyItemToCalendar: SyncDailyItemToCalendarUseCase,
    private val rescheduleTargetReminders: RescheduleTargetRemindersUseCase,
    private val widgetUpdater: WidgetUpdater,
    private val validator: DailyItemValidator
) {
    suspend operator fun invoke(item: DailyItem): DailyItem {
        val old = repository.getItem(item.id) ?: error("Daily item ${item.id} not found")
        val now = System.currentTimeMillis()
        val sync = when {
            !item.calendarSync.enabled -> item.calendarSync.copy(
                state = CalendarSyncState.NOT_SYNCED,
                lastError = null
            )
            old.calendarSync.state == CalendarSyncState.SYNCED &&
                old.copy(calendarSync = item.calendarSync) != item -> item.calendarSync.copy(
                    state = CalendarSyncState.DIRTY,
                    lastError = null
                )
            else -> item.calendarSync
        }
        val updated = item.copy(
            calendarSync = sync,
            updatedAtEpochMilli = maxOf(item.createdAtEpochMilli, now)
        )
        validator.requireValid(updated)
        repository.upsert(updated)
        rescheduleTargetReminders(ReminderTargetType.DAILY_ITEM, updated.id, now)
        if (updated.calendarSync.enabled) syncDailyItemToCalendar(updated.id)
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
        return repository.getItem(updated.id) ?: updated
    }
}

class CompleteDailyItemUseCase(
    private val repository: DailyItemRepository,
    private val reminderRepository: ReminderRepository,
    private val cancelReminder: CancelReminderUseCase,
    private val syncDailyItemToCalendar: SyncDailyItemToCalendarUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(
        id: String,
        completedAt: Long = System.currentTimeMillis()
    ): DailyItem? {
        val completed = repository.markCompleted(id, completedAt) ?: return null
        reminderRepository.getByTarget(ReminderTargetType.DAILY_ITEM, id)
            .filterNot { it.status.isTerminal() }
            .forEach { cancelReminder(it.id, completedAt) }
        if (completed.calendarSync.enabled) syncDailyItemToCalendar(id)
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
        return repository.getItem(id) ?: completed
    }
}

class ReopenDailyItemUseCase(
    private val repository: DailyItemRepository,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(id: String): DailyItem? {
        val updated = repository.reopen(id, System.currentTimeMillis())
        if (updated != null) widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
        return updated
    }
}

class ArchiveDailyItemUseCase(
    private val repository: DailyItemRepository,
    private val reminderRepository: ReminderRepository,
    private val cancelReminder: CancelReminderUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(id: String): DailyItem? {
        val now = System.currentTimeMillis()
        val updated = repository.archive(id, now)
        reminderRepository.getByTarget(ReminderTargetType.DAILY_ITEM, id)
            .filterNot { it.status.isTerminal() }
            .forEach { cancelReminder(it.id, now) }
        if (updated != null) widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
        return updated
    }
}

class DeleteDailyItemUseCase(
    private val repository: DailyItemRepository,
    private val reminderRepository: ReminderRepository,
    private val cancelReminder: CancelReminderUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(id: String) {
        val now = System.currentTimeMillis()
        reminderRepository.getByTarget(ReminderTargetType.DAILY_ITEM, id)
            .filterNot { it.status.isTerminal() }
            .forEach { cancelReminder(it.id, now) }
        repository.delete(id)
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
    }
}

class UpdateDailyItemCalendarSyncUseCase(
    private val repository: DailyItemRepository
) {
    suspend operator fun invoke(
        id: String,
        sync: DailyItemCalendarSync
    ): DailyItem? = repository.updateCalendarSync(id, sync)
}

private fun ReminderStatus.isTerminal(): Boolean {
    return this == ReminderStatus.DELIVERED ||
        this == ReminderStatus.CANCELLED ||
        this == ReminderStatus.MISSED
}
