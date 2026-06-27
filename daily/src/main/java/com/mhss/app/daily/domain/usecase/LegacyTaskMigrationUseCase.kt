package com.mhss.app.daily.domain.usecase

import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemFrequency
import com.mhss.app.daily.domain.model.DailyItemKind
import com.mhss.app.daily.domain.model.DailyItemLegacySource
import com.mhss.app.daily.domain.model.DailyItemLegacySourceType
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.daily.domain.model.DailyItemRecurrence
import com.mhss.app.daily.domain.model.DailyItemSchedule
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.domain.model.Priority
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.model.TaskFrequency
import com.mhss.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MigrateLegacyTasksToDailyItemsUseCase(
    private val taskRepository: TaskRepository,
    private val dailyItemRepository: DailyItemRepository,
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(): MigrationResult {
        val now = System.currentTimeMillis()
        val timeZoneId = TimeZone.currentSystemDefault().id
        var migrated = 0
        var retargetedReminders = 0

        val existing = dailyItemRepository.getAllItems()
            .mapNotNull { it.legacySource?.takeIf { source -> source.type == DailyItemLegacySourceType.TASK }?.id }
            .toSet()

        val items = taskRepository.getAllTasks().first()
            .filterNot { it.id in existing }
            .map { task -> task.toDailyItem(timeZoneId, now) }

        if (items.isNotEmpty()) {
            dailyItemRepository.upsertAll(items)
            migrated = items.size
        }

        taskRepository.getAllTasks().first().forEach { task ->
            reminderRepository.getByTarget(ReminderTargetType.TASK, task.id)
                .forEach { reminder ->
                    reminderRepository.save(
                        reminder.copy(
                            targetType = ReminderTargetType.DAILY_ITEM,
                            updatedAt = maxOf(reminder.createdAt, now)
                        )
                    )
                    retargetedReminders++
                }
        }

        val rewrittenTimeZones = dailyItemRepository.getAllItems()
            .filter { it.schedule.timeZoneId == "SYSTEM" }
            .map {
                it.copy(
                    schedule = it.schedule.copy(timeZoneId = timeZoneId),
                    updatedAtEpochMilli = maxOf(it.updatedAtEpochMilli, now)
                )
            }
        if (rewrittenTimeZones.isNotEmpty()) {
            dailyItemRepository.upsertAll(rewrittenTimeZones)
        }

        return MigrationResult(
            migrated = migrated,
            retargetedReminders = retargetedReminders,
            rewrittenTimeZones = rewrittenTimeZones.size
        )
    }

    data class MigrationResult(
        val migrated: Int,
        val retargetedReminders: Int,
        val rewrittenTimeZones: Int
    )
}

private fun Task.toDailyItem(
    timeZoneId: String,
    now: Long
): DailyItem {
    val completedAt = if (isCompleted) updatedDate.takeIf { it > 0 } ?: now else null
    return DailyItem(
        id = id,
        title = title,
        description = description,
        kind = DailyItemKind.TASK,
        schedule = DailyItemSchedule(
            dueAtEpochMilli = dueDate.takeIf { it > 0 },
            timeZoneId = timeZoneId
        ),
        isCompletable = true,
        completedAtEpochMilli = completedAt,
        status = if (isCompleted) DailyItemStatus.COMPLETED else DailyItemStatus.ACTIVE,
        priority = priority.toDailyItemPriority(),
        recurrence = if (recurring) {
            DailyItemRecurrence(
                frequency = frequency.toDailyItemFrequency(),
                interval = frequencyAmount.coerceAtLeast(1)
            )
        } else {
            null
        },
        legacySource = DailyItemLegacySource(
            type = DailyItemLegacySourceType.TASK,
            id = id
        ),
        subTasksJson = Json.encodeToString(subTasks),
        createdAtEpochMilli = createdDate.takeIf { it > 0 } ?: now,
        updatedAtEpochMilli = updatedDate.takeIf { it > 0 } ?: now
    )
}

private fun Priority.toDailyItemPriority(): DailyItemPriority = when (this) {
    Priority.LOW -> DailyItemPriority.LOW
    Priority.MEDIUM -> DailyItemPriority.MEDIUM
    Priority.HIGH -> DailyItemPriority.HIGH
}

private fun TaskFrequency.toDailyItemFrequency(): DailyItemFrequency = when (this) {
    TaskFrequency.EVERY_MINUTES -> DailyItemFrequency.EVERY_MINUTES
    TaskFrequency.HOURLY -> DailyItemFrequency.HOURLY
    TaskFrequency.DAILY -> DailyItemFrequency.DAILY
    TaskFrequency.WEEKLY -> DailyItemFrequency.WEEKLY
    TaskFrequency.MONTHLY -> DailyItemFrequency.MONTHLY
    TaskFrequency.ANNUAL -> DailyItemFrequency.YEARLY
}
