package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.AlarmRepository
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.DeleteAlarmUseCase
import com.mhss.app.alarm.use_case.ScheduleReminderUseCase
import com.mhss.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Migrates legacy My Brain alarm rows to the unified Reminder model (DF-403).
 *
 * Each task with a non-null [com.mhss.app.domain.model.Task.alarmId] and
 * future due date gets a single absolute Reminder whose trigger time equals
 * the old alarm epoch. After migration the task's alarmId is cleared and the
 * old alarm row is deleted.
 *
 * Safe to call multiple times — already-migrated tasks (alarmId == null) and
 * tasks whose alarm row no longer exists are skipped.
 */
@Single
class MigrateLegacyTaskAlarmsUseCase(
    private val taskRepository: TaskRepository,
    private val alarmRepository: AlarmRepository,
    private val reminderRepository: ReminderRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val deleteAlarm: DeleteAlarmUseCase
) {
    suspend operator fun invoke(): MigrationResult {
        val nowEpochMilli = Clock.System.now().toEpochMilliseconds()
        val allAlarms = alarmRepository.getAlarms()
        val alarmById = allAlarms.associateBy { it.id }
        val tasks = taskRepository.getAllTasks().first()
        var migrated = 0
        var skipped = 0
        var failed = 0

        for (task in tasks) {
            val alarmId = task.alarmId ?: continue
            val alarm = alarmById[alarmId]
            if (alarm == null) {
                taskRepository.updateTask(task.copy(alarmId = null))
                skipped++
                continue
            }

            try {
                val reminder = Reminder(
                    targetType = ReminderTargetType.TASK,
                    targetId = task.id,
                    absoluteTriggerAt = alarm.time,
                    enabled = true,
                    status = ReminderStatus.PENDING,
                    createdAt = nowEpochMilli,
                    updatedAt = nowEpochMilli
                )
                val savedReminder = reminderRepository.save(reminder)
                scheduleReminder(savedReminder.id, nowEpochMilli)

                taskRepository.updateTask(task.copy(alarmId = null))
                deleteAlarm(alarmId)
                migrated++
            } catch (_: Exception) {
                failed++
            }
        }

        return MigrationResult(migrated, skipped, failed)
    }

    data class MigrationResult(
        val migrated: Int,
        val skipped: Int,
        val failed: Int
    ) {
        val total: Int get() = migrated + skipped + failed
        val hasFailures: Boolean get() = failed > 0
    }
}
