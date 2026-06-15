package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.DeleteAlarmUseCase
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val cancelReminder: CancelReminderUseCase,
    private val deleteAlarm: DeleteAlarmUseCase
) {
    suspend operator fun invoke(task: Task) {
        taskRepository.deleteTask(task)

        val nowMillis = Clock.System.now().toEpochMilliseconds()

        // Cancel all reminders for this task
        val reminders = reminderRepository.getByTarget(
            ReminderTargetType.TASK,
            task.id
        )
        for (reminder in reminders) {
            if (!reminder.status.isTerminal()) {
                cancelReminder(reminder.id, nowMillis)
            }
        }

        // Clean up legacy alarm if still present (unmigrated)
        if (task.alarmId != null) {
            deleteAlarm(task.alarmId)
        }
    }

    private fun ReminderStatus.isTerminal(): Boolean {
        return this == ReminderStatus.DELIVERED ||
            this == ReminderStatus.CANCELLED ||
            this == ReminderStatus.MISSED
    }
}
