package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.use_case.RescheduleTargetRemindersUseCase
import com.mhss.app.alarm.use_case.ScheduleReminderUseCase
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.widget.WidgetUpdater
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Adds a calendar event to the system Calendar Provider and creates a default
 * absolute reminder at the event start time (DF-406).
 */
@Single
class AddCalendarEventUseCase(
    private val calendarEventRepository: CalendarRepository,
    private val reminderRepository: ReminderRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(calendarEvent: CalendarEvent): Long? {
        val calendars = calendarEventRepository.getCalendars()
        val eventId = if (calendars.isNotEmpty()) {
            calendarEventRepository.addEvent(calendarEvent)
        } else {
            calendarEventRepository.createCalendar()
            val calendar = calendarEventRepository.getCalendars().first()
            calendarEventRepository.addEvent(calendarEvent.copy(calendarId = calendar.id))
        }
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Calendar)

        // Create default absolute reminder at event start
        if (eventId != null && eventId > 0) {
            ensureDefaultReminder(eventId, calendarEvent.start)
        }

        return eventId
    }

    private suspend fun ensureDefaultReminder(eventId: Long, startEpochMilli: Long) {
        val targetId = eventId.toString()
        val existing = reminderRepository.getByTarget(
            ReminderTargetType.CALENDAR_EVENT, targetId
        )
        if (existing.isNotEmpty()) return // Already has reminders

        val now = Clock.System.now().toEpochMilliseconds()
        try {
            val reminder = Reminder(
                targetType = ReminderTargetType.CALENDAR_EVENT,
                targetId = targetId,
                absoluteTriggerAt = startEpochMilli,
                enabled = true,
                status = ReminderStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
            val saved = reminderRepository.save(reminder)
            scheduleReminder(saved.id, now)
        } catch (_: Exception) { }
    }
}
