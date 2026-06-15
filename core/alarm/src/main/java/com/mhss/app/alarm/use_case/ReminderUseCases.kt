package com.mhss.app.alarm.use_case

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.repository.ReminderScheduler
import com.mhss.app.alarm.repository.ReminderTargetTimeResolver
import org.koin.core.annotation.Single

enum class ReminderScheduleOutcome {
    SCHEDULED,
    CANCELLED,
    MISSED,
    TARGET_UNAVAILABLE,
    NOT_FOUND,
    FAILED
}

data class ReminderReconcileResult(
    val outcomes: Map<Long, ReminderScheduleOutcome>
) {
    val hasFailures: Boolean
        get() = outcomes.values.any { it == ReminderScheduleOutcome.FAILED }
}

@Single
class ScheduleReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val targetTimeResolver: ReminderTargetTimeResolver
) {
    suspend operator fun invoke(
        reminderId: Long,
        nowEpochMilli: Long = System.currentTimeMillis()
    ): ReminderScheduleOutcome {
        val reminder = reminderRepository.getById(reminderId)
            ?: return ReminderScheduleOutcome.NOT_FOUND

        return try {
            if (!reminder.enabled || reminder.status.isTerminal()) {
                reminderScheduler.cancelReminder(reminder.id)
                val status = if (reminder.status.isTerminal()) {
                    reminder.status
                } else {
                    ReminderStatus.CANCELLED
                }
                if (reminder.enabled || reminder.status != status) {
                    reminderRepository.save(
                        reminder.copy(
                            enabled = false,
                            status = status,
                            updatedAt = reminder.safeUpdatedAt(nowEpochMilli)
                        )
                    )
                }
                return ReminderScheduleOutcome.CANCELLED
            }

            val targetTime = if (reminder.relativeOffsetMinutes != null) {
                runCatching {
                    targetTimeResolver.resolveTargetTime(
                        reminder.targetType,
                        reminder.targetId
                    )
                }.getOrNull()
            } else {
                null
            }
            val triggerAt = reminder.resolveTriggerAt(targetTime)
            if (triggerAt == null) {
                reminderScheduler.cancelReminder(reminder.id)
                if (reminder.status != ReminderStatus.PENDING) {
                    reminderRepository.save(
                        reminder.copy(
                            status = ReminderStatus.PENDING,
                            updatedAt = reminder.safeUpdatedAt(nowEpochMilli)
                        )
                    )
                }
                return ReminderScheduleOutcome.TARGET_UNAVAILABLE
            }
            if (triggerAt <= nowEpochMilli) {
                reminderScheduler.cancelReminder(reminder.id)
                reminderRepository.save(
                    reminder.copy(
                        enabled = false,
                        status = ReminderStatus.MISSED,
                        updatedAt = reminder.safeUpdatedAt(nowEpochMilli)
                    )
                )
                return ReminderScheduleOutcome.MISSED
            }

            reminderScheduler.scheduleReminder(reminder.id, triggerAt)
            if (reminder.status != ReminderStatus.SCHEDULED) {
                reminderRepository.save(
                    reminder.copy(
                        status = ReminderStatus.SCHEDULED,
                        updatedAt = reminder.safeUpdatedAt(nowEpochMilli)
                    )
                )
            }
            ReminderScheduleOutcome.SCHEDULED
        } catch (_: Exception) {
            runCatching { reminderScheduler.cancelReminder(reminder.id) }
            if (reminder.status != ReminderStatus.PENDING) runCatching {
                reminderRepository.save(
                    reminder.copy(
                        status = ReminderStatus.PENDING,
                        updatedAt = reminder.safeUpdatedAt(nowEpochMilli)
                    )
                )
            }
            ReminderScheduleOutcome.FAILED
        }
    }
}

@Single
class CancelReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(
        reminderId: Long,
        nowEpochMilli: Long = System.currentTimeMillis()
    ): Reminder? {
        val reminder = reminderRepository.getById(reminderId)
        runCatching { reminderScheduler.cancelReminder(reminderId) }
        return reminder?.let {
            if (!it.enabled && it.status.isTerminal()) return@let it
            reminderRepository.save(
                it.copy(
                    enabled = false,
                    status = ReminderStatus.CANCELLED,
                    updatedAt = it.safeUpdatedAt(nowEpochMilli)
                )
            )
        }
    }
}

@Single
class RescheduleTargetRemindersUseCase(
    private val reminderRepository: ReminderRepository,
    private val scheduleReminder: ScheduleReminderUseCase
) {
    suspend operator fun invoke(
        targetType: ReminderTargetType,
        targetId: String,
        nowEpochMilli: Long = System.currentTimeMillis()
    ): ReminderReconcileResult {
        val outcomes = reminderRepository.getByTarget(targetType, targetId)
            .associate { reminder ->
                reminder.id to scheduleReminder(reminder.id, nowEpochMilli)
            }
        return ReminderReconcileResult(outcomes)
    }
}

@Single
class ReconcileScheduledRemindersUseCase(
    private val reminderRepository: ReminderRepository,
    private val scheduleReminder: ScheduleReminderUseCase
) {
    suspend operator fun invoke(
        nowEpochMilli: Long = System.currentTimeMillis()
    ): ReminderReconcileResult {
        val outcomes = reminderRepository.getAll().associate { reminder ->
            reminder.id to scheduleReminder(reminder.id, nowEpochMilli)
        }
        return ReminderReconcileResult(outcomes)
    }
}

@Single
class RestoreAllRemindersUseCase(
    private val reconcileScheduledReminders: ReconcileScheduledRemindersUseCase
) {
    suspend operator fun invoke(
        nowEpochMilli: Long = System.currentTimeMillis()
    ): ReminderReconcileResult = reconcileScheduledReminders(nowEpochMilli)
}

@Single
class TriggerReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val targetTimeResolver: ReminderTargetTimeResolver,
    private val scheduleReminder: ScheduleReminderUseCase
) {
    suspend operator fun invoke(
        reminderId: Long,
        expectedTriggerAtEpochMilli: Long,
        nowEpochMilli: Long = System.currentTimeMillis()
    ): Reminder? {
        val reminder = reminderRepository.getById(reminderId) ?: return null
        if (!reminder.enabled || reminder.status.isTerminal()) {
            runCatching { reminderScheduler.cancelReminder(reminderId) }
            return null
        }

        val targetTime = if (reminder.relativeOffsetMinutes != null) {
            runCatching {
                targetTimeResolver.resolveTargetTime(
                    reminder.targetType,
                    reminder.targetId
                )
            }.getOrNull()
        } else {
            null
        }
        val currentTriggerAt = reminder.resolveTriggerAt(targetTime)
        if (
            currentTriggerAt == null ||
            currentTriggerAt != expectedTriggerAtEpochMilli ||
            currentTriggerAt > nowEpochMilli
        ) {
            scheduleReminder(reminderId, nowEpochMilli)
            return null
        }

        runCatching { reminderScheduler.cancelReminder(reminderId) }
        return reminderRepository.save(
            reminder.copy(
                enabled = false,
                status = ReminderStatus.DELIVERED,
                updatedAt = reminder.safeUpdatedAt(nowEpochMilli)
            )
        )
    }
}

private fun ReminderStatus.isTerminal(): Boolean {
    return this == ReminderStatus.DELIVERED ||
        this == ReminderStatus.CANCELLED ||
        this == ReminderStatus.MISSED
}

private fun Reminder.safeUpdatedAt(nowEpochMilli: Long): Long {
    return maxOf(createdAt, nowEpochMilli)
}
