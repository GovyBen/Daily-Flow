package com.mhss.app.alarm.repository

interface ReminderScheduler {

    fun scheduleReminder(reminderId: Long, triggerAtEpochMilli: Long)

    fun cancelReminder(reminderId: Long)
}
