package com.mhss.app.daily.domain.usecase

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.repository.ReminderScheduler
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemCalendarSync
import com.mhss.app.daily.domain.model.DailyItemFilter
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.domain.model.Calendar
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DailyItemUseCasesTest {

    @Test
    fun noDateItemDoesNotWriteCalendarProviderAndMarksSyncFailed() = runBlocking {
        val repository = FakeDailyItemRepository(
            testItem(
                calendarSync = DailyItemCalendarSync(enabled = true)
            )
        )
        val calendarRepository = FakeCalendarRepository()

        val result = SyncDailyItemToCalendarUseCase(repository, calendarRepository)("item")

        assertEquals(CalendarSyncState.FAILED, result?.calendarSync?.state)
        assertEquals("Calendar sync requires a start time or due time", result?.calendarSync?.lastError)
        assertEquals(0, calendarRepository.addEventCalls)
        assertEquals(0, calendarRepository.updateEventCalls)
    }

    @Test
    fun archiveCancelsNonTerminalDailyItemReminders() = runBlocking {
        val repository = FakeDailyItemRepository(testItem())
        val reminders = FakeReminderRepository(
            Reminder(
                id = 1,
                targetType = ReminderTargetType.DAILY_ITEM,
                targetId = "item",
                absoluteTriggerAt = 10_000,
                enabled = true,
                status = ReminderStatus.SCHEDULED,
                createdAt = 1,
                updatedAt = 1
            ),
            Reminder(
                id = 2,
                targetType = ReminderTargetType.DAILY_ITEM,
                targetId = "item",
                absoluteTriggerAt = 20_000,
                enabled = false,
                status = ReminderStatus.CANCELLED,
                createdAt = 1,
                updatedAt = 1
            )
        )
        val scheduler = FakeReminderScheduler()

        ArchiveDailyItemUseCase(
            repository = repository,
            reminderRepository = reminders,
            cancelReminder = CancelReminderUseCase(reminders, scheduler),
            widgetUpdater = FakeWidgetUpdater()
        )("item")

        assertEquals(DailyItemStatus.ARCHIVED, repository.item.status)
        assertEquals(listOf(1L), scheduler.cancelledIds)
        assertFalse(reminders.byId.getValue(1).enabled)
        assertEquals(ReminderStatus.CANCELLED, reminders.byId.getValue(1).status)
    }

    private fun testItem(
        calendarSync: DailyItemCalendarSync = DailyItemCalendarSync()
    ) = DailyItem(
        id = "item",
        title = "Item",
        calendarSync = calendarSync,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1
    )
}

private class FakeDailyItemRepository(
    var item: DailyItem
) : DailyItemRepository {
    override fun observeItems(filter: DailyItemFilter): Flow<List<DailyItem>> = flowOf(listOf(item))
    override fun observeItem(id: String): Flow<DailyItem?> = flowOf(item.takeIf { it.id == id })
    override suspend fun getItem(id: String): DailyItem? = item.takeIf { it.id == id }
    override suspend fun getAllItems(): List<DailyItem> = listOf(item)
    override suspend fun searchItems(query: String): List<DailyItem> = listOf(item)
    override suspend fun upsert(item: DailyItem): DailyItem {
        this.item = item
        return item
    }
    override suspend fun upsertAll(items: List<DailyItem>) {
        item = items.first()
    }
    override suspend fun delete(id: String) = Unit
    override suspend fun markCompleted(id: String, completedAt: Long): DailyItem? = null
    override suspend fun reopen(id: String, updatedAt: Long): DailyItem? = null
    override suspend fun archive(id: String, updatedAt: Long): DailyItem? {
        item = item.copy(status = DailyItemStatus.ARCHIVED, updatedAtEpochMilli = updatedAt)
        return item
    }
    override suspend fun updateCalendarSync(id: String, sync: DailyItemCalendarSync): DailyItem? {
        item = item.copy(calendarSync = sync)
        return item
    }
    override suspend fun markSyncState(
        id: String,
        state: CalendarSyncState,
        lastError: String?,
        updatedAt: Long
    ): DailyItem? {
        return updateCalendarSync(id, item.calendarSync.copy(state = state, lastError = lastError))
    }
}

private class FakeCalendarRepository : CalendarRepository {
    var addEventCalls = 0
    var updateEventCalls = 0

    override suspend fun getEvents(excludedCalendars: List<Int>, until: Long?): List<CalendarEvent> = emptyList()
    override suspend fun getEvents(start: Long, end: Long, excludedCalendars: List<Int>): List<CalendarEvent> = emptyList()
    override suspend fun searchEventsByTitleWithinRange(
        start: Long,
        end: Long,
        titleQuery: String,
        excludedCalendars: List<Int>
    ): List<CalendarEvent> = emptyList()
    override suspend fun getCalendars(): List<Calendar> = listOf(Calendar(1, "Local", "Daily Flow", 0))
    override suspend fun getEventById(id: Long): CalendarEvent? = null
    override suspend fun addEvent(event: CalendarEvent): Long? {
        addEventCalls++
        return 10
    }
    override suspend fun deleteEvent(event: CalendarEvent) = Unit
    override suspend fun updateEvent(event: CalendarEvent) {
        updateEventCalls++
    }
    override suspend fun createCalendar() = Unit
}

private class FakeReminderRepository(
    vararg reminders: Reminder
) : ReminderRepository {
    val byId = reminders.associateBy { it.id }.toMutableMap()

    override suspend fun getAll(): List<Reminder> = byId.values.toList()
    override suspend fun getById(id: Long): Reminder? = byId[id]
    override suspend fun getByTarget(targetType: ReminderTargetType, targetId: String): List<Reminder> {
        return byId.values.filter { it.targetType == targetType && it.targetId == targetId }
    }
    override suspend fun getEnabled(): List<Reminder> = byId.values.filter { it.enabled }
    override suspend fun save(reminder: Reminder): Reminder {
        byId[reminder.id] = reminder
        return reminder
    }
    override suspend fun delete(id: Long) {
        byId.remove(id)
    }
}

private class FakeReminderScheduler : ReminderScheduler {
    val cancelledIds = mutableListOf<Long>()
    override fun scheduleReminder(reminderId: Long, triggerAtEpochMilli: Long) = Unit
    override fun cancelReminder(reminderId: Long) {
        cancelledIds += reminderId
    }
}

private class FakeWidgetUpdater : WidgetUpdater {
    override suspend fun updateAll(type: WidgetUpdater.WidgetType) = Unit
}
