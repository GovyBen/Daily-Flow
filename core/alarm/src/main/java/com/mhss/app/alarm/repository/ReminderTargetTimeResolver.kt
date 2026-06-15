package com.mhss.app.alarm.repository

import com.mhss.app.alarm.model.ReminderTargetType

interface ReminderTargetTimeResolver {

    suspend fun resolveTargetTime(
        targetType: ReminderTargetType,
        targetId: String
    ): Long?
}
