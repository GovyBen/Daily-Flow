package com.mhss.app.alarm.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulePolicyTest {

    @Test
    fun exactCapabilityUsesExactAlarmAndFiveMinuteFallback() {
        assertEquals(
            AlarmSchedulePlan(
                deliveryMode = AlarmDeliveryMode.EXACT,
                fallbackDelayMillis = 5 * 60 * 1000L
            ),
            AlarmSchedulePolicy.create(canScheduleExactAlarms = true)
        )
    }

    @Test
    fun missingExactCapabilityUsesInexactAlarmAndFifteenMinuteFallback() {
        assertEquals(
            AlarmSchedulePlan(
                deliveryMode = AlarmDeliveryMode.INEXACT,
                fallbackDelayMillis = 15 * 60 * 1000L
            ),
            AlarmSchedulePolicy.create(canScheduleExactAlarms = false)
        )
    }
}
