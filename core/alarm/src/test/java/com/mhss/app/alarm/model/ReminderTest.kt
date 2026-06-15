package com.mhss.app.alarm.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReminderTest {

    @Test
    fun acceptsExactlyOneTriggerSource() {
        val absolute = reminder(absoluteTriggerAt = 2_000)
        val relative = reminder(
            absoluteTriggerAt = null,
            relativeOffsetMinutes = 15
        )

        assertEquals(2_000L, absolute.absoluteTriggerAt)
        assertEquals(15, relative.relativeOffsetMinutes)
    }

    @Test
    fun rejectsMissingOrAmbiguousTriggerSource() {
        assertThrows(IllegalArgumentException::class.java) {
            reminder(absoluteTriggerAt = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            reminder(relativeOffsetMinutes = 15)
        }
    }

    @Test
    fun rejectsInvalidTargetOffsetAndTimestamps() {
        assertThrows(IllegalArgumentException::class.java) {
            reminder(targetId = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            reminder(
                absoluteTriggerAt = null,
                relativeOffsetMinutes = -1
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            reminder(updatedAt = 999)
        }
    }

    @Test
    fun persistedIdsProduceDistinctRequestCodes() {
        val first = reminder(id = 1)
        val second = reminder(id = 2)

        assertEquals(1, first.requestCode())
        assertEquals(2, second.requestCode())
        assertNotEquals(first.requestCode(), second.requestCode())
    }

    @Test
    fun requestCodeRequiresSupportedPersistedId() {
        assertThrows(IllegalArgumentException::class.java) {
            reminder().requestCode()
        }
        assertThrows(IllegalArgumentException::class.java) {
            reminder(id = Int.MAX_VALUE.toLong() + 1).requestCode()
        }
    }

    private fun reminder(
        id: Long = 0,
        targetId: String = "task-1",
        absoluteTriggerAt: Long? = 2_000,
        relativeOffsetMinutes: Int? = null,
        createdAt: Long = 1_000,
        updatedAt: Long = createdAt
    ) = Reminder(
        id = id,
        targetType = ReminderTargetType.TASK,
        targetId = targetId,
        absoluteTriggerAt = absoluteTriggerAt,
        relativeOffsetMinutes = relativeOffsetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
