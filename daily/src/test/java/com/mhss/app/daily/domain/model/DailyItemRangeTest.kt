package com.mhss.app.daily.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyItemRangeTest {
    private val zone = TimeZone.of("Asia/Shanghai")

    @Test
    fun surroundingSevenDaysUsesLocalDateBoundaries() {
        val todayNoon = LocalDate(2026, 6, 27)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds() + 12 * 60 * 60 * 1000L

        val bounds = DailyItemRangeResolver.bounds(
            range = DailyItemRange.SurroundingSevenDays,
            nowEpochMilli = todayNoon,
            timeZone = zone
        )

        assertEquals(
            LocalDate(2026, 6, 20).atStartOfDayIn(zone).toEpochMilliseconds(),
            bounds.startInclusiveEpochMilli
        )
        assertEquals(
            LocalDate(2026, 7, 5).atStartOfDayIn(zone).toEpochMilliseconds(),
            bounds.endExclusiveEpochMilli
        )
        assertTrue(bounds.includeOverdueCarry)
    }

    @Test
    fun customRangeEndIsExclusiveNextLocalDay() {
        val bounds = DailyItemRangeResolver.bounds(
            range = DailyItemRange.Custom(
                startInclusive = LocalDate(2026, 6, 1),
                endInclusive = LocalDate(2026, 6, 3)
            ),
            nowEpochMilli = LocalDate(2026, 6, 2)
                .atStartOfDayIn(zone)
                .toEpochMilliseconds(),
            timeZone = zone
        )

        assertTrue(
            bounds.contains(
                LocalDate(2026, 6, 3).atStartOfDayIn(zone).toEpochMilliseconds()
            )
        )
        assertEquals(
            LocalDate(2026, 6, 4).atStartOfDayIn(zone).toEpochMilliseconds(),
            bounds.endExclusiveEpochMilli
        )
    }
}
