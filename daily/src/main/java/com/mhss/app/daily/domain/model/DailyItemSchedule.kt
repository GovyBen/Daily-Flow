package com.mhss.app.daily.domain.model

import kotlinx.datetime.TimeZone

data class DailyItemSchedule(
    val startAtEpochMilli: Long? = null,
    val endAtEpochMilli: Long? = null,
    val dueAtEpochMilli: Long? = null,
    val allDay: Boolean = false,
    val timeZoneId: String = TimeZone.currentSystemDefault().id
) {
    val hasDate: Boolean
        get() = startAtEpochMilli != null || dueAtEpochMilli != null
}
