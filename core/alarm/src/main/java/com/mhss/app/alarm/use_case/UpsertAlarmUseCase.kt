package com.mhss.app.alarm.use_case

import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.repository.AlarmRepository
import com.mhss.app.alarm.repository.AlarmScheduler
import org.koin.core.annotation.Single

@Single
class UpsertAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(currentAlarmId: Int?, dueDate: Long): Int? {
        val alarmId = alarmRepository.upsertAlarm(Alarm(currentAlarmId ?: 0, dueDate)).let {
            // -1 indicates alarm is updated so we return the original id
            if (it == -1L) currentAlarmId ?: 0 else it.toInt()
        }
        return try {
            alarmScheduler.scheduleAlarm(Alarm(id = alarmId, dueDate))
            alarmId
        } catch (_: Exception) {
            runCatching { alarmScheduler.cancelAlarm(alarmId) }
            runCatching { alarmRepository.deleteAlarm(alarmId) }
            null
        }
    }
}
