package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.use_case.RescheduleTargetRemindersUseCase
import com.mhss.app.alarm.use_case.ScheduleReminderUseCase
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.model.TaskFrequency
import com.mhss.app.domain.repository.TaskRepository
import com.mhss.app.widget.WidgetUpdater
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
class UpsertTaskUseCase(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val cancelReminder: CancelReminderUseCase,
    private val rescheduleTargetReminders: RescheduleTargetRemindersUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(
        task: Task,
        previousTask: Task? = null,
        updateWidget: Boolean = true
    ): Boolean {
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        val taskWithResolvedRecurrence = task.rollRecurringDueDateIfNeeded(
            previousTask = previousTask,
            nowMillis = nowMillis
        )

        // Determine whether to cancel or schedule reminders
        val shouldHaveReminder = !taskWithResolvedRecurrence.isCompleted &&
            taskWithResolvedRecurrence.dueDate != 0L &&
            taskWithResolvedRecurrence.dueDate > nowMillis

        val dueDateChanged = previousTask != null &&
            previousTask.dueDate != taskWithResolvedRecurrence.dueDate

        val wasCompleted = previousTask != null &&
            !previousTask.isCompleted && taskWithResolvedRecurrence.isCompleted

        when {
            // Cancel reminders: task completed, dueDate removed, or dueDate expired
            !shouldHaveReminder -> {
                cancelAllRemindersForTask(taskWithResolvedRecurrence.id, nowMillis)
            }
            // Due date changed (including recurring roll-forward) — reschedule
            dueDateChanged || wasCompleted -> {
                ensureDefaultTaskReminder(taskWithResolvedRecurrence, nowMillis)
            }
            // New task with future dueDate, or unchanged task — ensure reminder exists
            previousTask == null -> {
                ensureDefaultTaskReminder(taskWithResolvedRecurrence, nowMillis)
            }
        }

        // Always clear alarmId after migration to unified reminders
        val taskToSave = taskWithResolvedRecurrence.copy(alarmId = null)
        taskRepository.upsertTask(taskToSave)
        if (updateWidget) widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)

        return true
    }

    private suspend fun ensureDefaultTaskReminder(task: Task, nowMillis: Long) {
        val existingReminders = reminderRepository.getByTarget(
            ReminderTargetType.TASK,
            task.id
        )
        // If reminders already exist, reschedule them (handles relative offsets)
        if (existingReminders.isNotEmpty()) {
            rescheduleTargetReminders(ReminderTargetType.TASK, task.id, nowMillis)
            return
        }

        // Create a default absolute reminder at the due date
        try {
            val reminder = Reminder(
                targetType = ReminderTargetType.TASK,
                targetId = task.id,
                absoluteTriggerAt = task.dueDate,
                enabled = true,
                status = ReminderStatus.PENDING,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
            val saved = reminderRepository.save(reminder)
            scheduleReminder(saved.id, nowMillis)
        } catch (_: Exception) {
            // Scheduling failed; reminder stays PENDING and will be retried
            // by the next reconciliation pass.
        }
    }

    private suspend fun cancelAllRemindersForTask(taskId: String, nowMillis: Long) {
        val existingReminders = reminderRepository.getByTarget(
            ReminderTargetType.TASK,
            taskId
        )
        for (reminder in existingReminders) {
            if (!reminder.status.isTerminal()) {
                cancelReminder(reminder.id, nowMillis)
            }
        }
    }

    private fun ReminderStatus.isTerminal(): Boolean {
        return this == ReminderStatus.DELIVERED ||
            this == ReminderStatus.CANCELLED ||
            this == ReminderStatus.MISSED
    }

    private fun Task.rollRecurringDueDateIfNeeded(
        previousTask: Task?,
        nowMillis: Long
    ): Task {
        return if (
            isCompleted &&
            previousTask?.isCompleted == false &&
            recurring &&
            dueDate != 0L
        ) {
            val timeZone = TimeZone.currentSystemDefault()
            var nextDueInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(dueDate)
            val frequencyAmount = frequencyAmount.coerceAtLeast(1)
            do {
                nextDueInstant = nextDueInstant.advance(frequency, frequencyAmount, timeZone)
            } while (nextDueInstant.toEpochMilliseconds() <= nowMillis)
            copy(
                dueDate = nextDueInstant.toEpochMilliseconds(),
                isCompleted = false
            )
        } else {
            this
        }
    }

    private fun kotlinx.datetime.Instant.advance(
        frequency: TaskFrequency,
        frequencyAmount: Int,
        timeZone: TimeZone
    ): kotlinx.datetime.Instant = when (frequency) {
        TaskFrequency.EVERY_MINUTES ->
            this.plus(frequencyAmount, DateTimeUnit.MINUTE, timeZone)
        TaskFrequency.HOURLY ->
            this.plus(frequencyAmount, DateTimeUnit.HOUR, timeZone)
        TaskFrequency.DAILY ->
            this.plus(frequencyAmount, DateTimeUnit.DAY, timeZone)
        TaskFrequency.WEEKLY ->
            this.plus(frequencyAmount, DateTimeUnit.WEEK, timeZone)
        TaskFrequency.MONTHLY ->
            this.plus(DateTimePeriod(months = frequencyAmount), timeZone)
        TaskFrequency.ANNUAL ->
            this.plus(DateTimePeriod(years = frequencyAmount), timeZone)
    }
}
