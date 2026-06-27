package com.mhss.app.daily.domain.validation

import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemSchedule
import com.mhss.app.daily.domain.model.DailyItemStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyItemValidatorTest {
    private val validator = DailyItemValidator()

    @Test
    fun noDateItemsAreValid() {
        val item = testItem(schedule = DailyItemSchedule())

        assertTrue(validator.validate(item).isEmpty())
    }

    @Test
    fun rejectsEndBeforeStart() {
        val item = testItem(
            schedule = DailyItemSchedule(
                startAtEpochMilli = 2_000,
                endAtEpochMilli = 1_000
            )
        )

        assertTrue(
            validator.validate(item).contains(DailyItemValidationError.EndBeforeStart)
        )
    }

    @Test
    fun completedStateRequiresTimestamp() {
        val item = testItem(status = DailyItemStatus.COMPLETED)

        assertTrue(
            validator.validate(item)
                .contains(DailyItemValidationError.CompletedWithoutTimestamp)
        )
    }

    @Test
    fun activeStateRejectsCompletedTimestamp() {
        val item = testItem(completedAt = 1_000)

        assertTrue(
            validator.validate(item)
                .contains(DailyItemValidationError.ActiveWithCompletedTimestamp)
        )
    }

    @Test
    fun acceptsCompletedItemWithTimestamp() {
        val item = testItem(
            status = DailyItemStatus.COMPLETED,
            completedAt = 1_000
        )

        assertFalse(validator.validate(item).isNotEmpty())
    }

    private fun testItem(
        schedule: DailyItemSchedule = DailyItemSchedule(timeZoneId = "UTC"),
        status: DailyItemStatus = DailyItemStatus.ACTIVE,
        completedAt: Long? = null
    ) = DailyItem(
        id = "item",
        title = "Item",
        schedule = schedule,
        status = status,
        completedAtEpochMilli = completedAt,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1
    )
}
