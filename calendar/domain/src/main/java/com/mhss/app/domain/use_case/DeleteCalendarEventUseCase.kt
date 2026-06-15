package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.widget.WidgetUpdater
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Deletes a calendar event and cancels all its reminders (DF-406).
 */
@Single
class DeleteCalendarEventUseCase(
    private val calendarRepository: CalendarRepository,
    private val reminderRepository: ReminderRepository,
    private val cancelReminder: CancelReminderUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(event: CalendarEvent) {
        calendarRepository.deleteEvent(event)
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Calendar)

        // Cancel all reminders for this event
        val targetId = event.id.toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val reminders = reminderRepository.getByTarget(
            ReminderTargetType.CALENDAR_EVENT, targetId
        )
        for (reminder in reminders) {
            if (!reminder.status.isTerminal()) {
                cancelReminder(reminder.id, now)
            }
        }
    }

    private fun ReminderStatus.isTerminal(): Boolean =
        this == ReminderStatus.DELIVERED ||
        this == ReminderStatus.CANCELLED ||
        this == ReminderStatus.MISSED
}
