package com.mhss.app.mybrain.data.repository

import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderTargetTimeResolver
import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.domain.repository.CalendarRepository
import com.mhss.app.domain.repository.TaskRepository
import org.koin.core.annotation.Single

@Single
class ReminderTargetTimeResolverImpl(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val dailyItemRepository: DailyItemRepository
) : ReminderTargetTimeResolver {

    override suspend fun resolveTargetTime(
        targetType: ReminderTargetType,
        targetId: String
    ): Long? {
        return when (targetType) {
            ReminderTargetType.TASK -> taskRepository.getTaskById(targetId)
                ?.takeUnless { it.isCompleted }
                ?.dueDate
                ?.takeIf { it > 0 }

            ReminderTargetType.DAILY_ITEM -> dailyItemRepository.getItem(targetId)
                ?.takeIf { it.status == DailyItemStatus.ACTIVE }
                ?.let { it.schedule.startAtEpochMilli ?: it.schedule.dueAtEpochMilli }

            ReminderTargetType.CALENDAR_EVENT -> targetId.toLongOrNull()
                ?.let { calendarRepository.getEventById(it)?.start }

            ReminderTargetType.RECORD_PROMPT -> null
        }
    }
}
