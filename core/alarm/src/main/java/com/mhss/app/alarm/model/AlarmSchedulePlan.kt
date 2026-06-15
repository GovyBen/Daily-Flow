package com.mhss.app.alarm.model

enum class AlarmDeliveryMode {
    EXACT,
    INEXACT
}

data class AlarmSchedulePlan(
    val deliveryMode: AlarmDeliveryMode,
    val fallbackDelayMillis: Long
)

object AlarmSchedulePolicy {
    private const val EXACT_FALLBACK_DELAY_MILLIS = 5 * 60 * 1000L
    private const val INEXACT_FALLBACK_DELAY_MILLIS = 15 * 60 * 1000L

    fun create(canScheduleExactAlarms: Boolean): AlarmSchedulePlan {
        return if (canScheduleExactAlarms) {
            AlarmSchedulePlan(
                deliveryMode = AlarmDeliveryMode.EXACT,
                fallbackDelayMillis = EXACT_FALLBACK_DELAY_MILLIS
            )
        } else {
            AlarmSchedulePlan(
                deliveryMode = AlarmDeliveryMode.INEXACT,
                fallbackDelayMillis = INEXACT_FALLBACK_DELAY_MILLIS
            )
        }
    }
}
