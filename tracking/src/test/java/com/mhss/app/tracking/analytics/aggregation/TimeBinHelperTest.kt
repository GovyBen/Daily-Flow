package com.mhss.app.tracking.analytics.aggregation

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class TimeBinHelperTest {

    @Test
    fun dailyBinUsesCalendarDayAcrossDaylightSavingChange() {
        val timeZone = TimeZone.of("America/New_York")
        val helper = TimeBinHelper(AggregationPreferences(timeZone = timeZone))

        val bin = helper.binContaining(
            LocalDateTime(2026, 3, 8, 12, 0).toInstant(timeZone),
            FixedBinSize.DAY
        )

        assertEquals(
            LocalDateTime(2026, 3, 8, 0, 0),
            bin.startInclusive.toLocalDateTime(timeZone)
        )
        assertEquals(
            LocalDateTime(2026, 3, 9, 0, 0),
            bin.endExclusive.toLocalDateTime(timeZone)
        )
        assertEquals(23.hours, bin.endExclusive - bin.startInclusive)
    }

    @Test
    fun customStartTimeMovesEarlyMorningIntoPreviousDay() {
        val timeZone = TimeZone.UTC
        val helper = TimeBinHelper(
            AggregationPreferences(
                startTimeOfDay = LocalTime(5, 0),
                timeZone = timeZone
            )
        )

        val bin = helper.binContaining(
            LocalDateTime(2026, 11, 29, 4, 45).toInstant(timeZone),
            FixedBinSize.DAY
        )

        assertEquals(
            LocalDateTime(2026, 11, 28, 5, 0),
            bin.startInclusive.toLocalDateTime(timeZone)
        )
    }

    @Test
    fun weeklyBinUsesConfiguredFirstDayAndStartTime() {
        val timeZone = TimeZone.UTC
        val helper = TimeBinHelper(
            AggregationPreferences(
                firstDayOfWeek = DayOfWeek.WEDNESDAY,
                startTimeOfDay = LocalTime(4, 0),
                timeZone = timeZone
            )
        )

        val bin = helper.binContaining(
            LocalDateTime(2026, 12, 1, 6, 0).toInstant(timeZone),
            FixedBinSize.WEEK
        )

        assertEquals(
            LocalDateTime(2026, 11, 25, 4, 0),
            bin.startInclusive.toLocalDateTime(timeZone)
        )
        assertEquals(
            LocalDateTime(2026, 12, 2, 4, 0),
            bin.endExclusive.toLocalDateTime(timeZone)
        )
    }

    @Test
    fun monthlyBinRespectsStartTimeOnFirstDay() {
        val timeZone = TimeZone.UTC
        val helper = TimeBinHelper(
            AggregationPreferences(
                startTimeOfDay = LocalTime(5, 0),
                timeZone = timeZone
            )
        )

        val bin = helper.binContaining(
            LocalDateTime(2026, 7, 1, 4, 0).toInstant(timeZone),
            FixedBinSize.MONTH
        )

        assertEquals(
            LocalDateTime(2026, 6, 1, 5, 0),
            bin.startInclusive.toLocalDateTime(timeZone)
        )
        assertEquals(
            LocalDateTime(2026, 7, 1, 5, 0),
            bin.endExclusive.toLocalDateTime(timeZone)
        )
    }
}
