package com.mhss.app.alarm.repository

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderTargetType

interface ReminderRepository {

    suspend fun getAll(): List<Reminder>

    suspend fun getById(id: Long): Reminder?

    suspend fun getByTarget(
        targetType: ReminderTargetType,
        targetId: String
    ): List<Reminder>

    suspend fun getEnabled(): List<Reminder>

    suspend fun save(reminder: Reminder): Reminder

    suspend fun delete(id: Long)
}
