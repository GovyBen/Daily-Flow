package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.use_case.RescheduleTargetRemindersUseCase
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.widget.WidgetUpdater
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Updates a calendar event and reschedules its reminders when the start time
 * changes (DF-406). Also handles dangling targets when the event can no longer
 * be found in the Calendar Provider.
 */
@Single
class UpdateCalendarEventUseCase(
    private val calendarRepository: CalendarRepository,
    private val reminderRepository: ReminderRepository,
    private val rescheduleTargetReminders: RescheduleTargetRemindersUseCase,
    private val cancelReminder: CancelReminderUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(event: CalendarEvent) {
        calendarRepository.updateEvent(event)
        widgetUpdater.updateAll(WidgetUpdater.WidgetType.Calendar)

        // Reschedule reminders; if the event was externally deleted,
        // the resolver returns null and reminders enter PENDING state.
        val targetId = event.id.toString()
        try {
            rescheduleTargetReminders(
                ReminderTargetType.CALENDAR_EVENT,
                targetId,
                Clock.System.now().toEpochMilliseconds()
            )
        } catch (_: Exception) { }
    }
}
