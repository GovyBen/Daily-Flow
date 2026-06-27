package com.mhss.app.daily.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

sealed interface DailyItemRange {
    data object Today : DailyItemRange
    data object Tomorrow : DailyItemRange
    data object SurroundingSevenDays : DailyItemRange
    data object FutureSevenDays : DailyItemRange
    data object ThisWeek : DailyItemRange
    data object ThisMonth : DailyItemRange
    data object Overdue : DailyItemRange
    data object NoDate : DailyItemRange
    data object Completed : DailyItemRange
    data object All : DailyItemRange
    data class Custom(
        val startInclusive: LocalDate,
        val endInclusive: LocalDate
    ) : DailyItemRange
}

data class DailyItemRangeBounds(
    val startInclusiveEpochMilli: Long?,
    val endExclusiveEpochMilli: Long?,
    val includeOverdueCarry: Boolean = false
) {
    fun contains(epochMilli: Long): Boolean {
        val start = startInclusiveEpochMilli
        val end = endExclusiveEpochMilli
        return (start == null || epochMilli >= start) && (end == null || epochMilli < end)
    }
}

object DailyItemRangeResolver {
    fun bounds(
        range: DailyItemRange,
        nowEpochMilli: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
    ): DailyItemRangeBounds {
        val today = Instant.fromEpochMilliseconds(nowEpochMilli)
            .toLocalDateTime(timeZone)
            .date

        return when (range) {
            DailyItemRange.Today -> dayBounds(today, timeZone)
            DailyItemRange.Tomorrow -> dayBounds(today.plus(1, DateTimeUnit.DAY), timeZone)
            DailyItemRange.SurroundingSevenDays -> dateBounds(
                start = today.minus(7, DateTimeUnit.DAY),
                endInclusive = today.plus(7, DateTimeUnit.DAY),
                timeZone = timeZone,
                includeOverdueCarry = true
            )
            DailyItemRange.FutureSevenDays -> dateBounds(
                start = today,
                endInclusive = today.plus(7, DateTimeUnit.DAY),
                timeZone = timeZone
            )
            DailyItemRange.ThisWeek -> {
                val offset = (today.dayOfWeek.ordinal - firstDayOfWeek.ordinal + 7) % 7
                val start = today.minus(offset, DateTimeUnit.DAY)
                dateBounds(
                    start = start,
                    endInclusive = start.plus(6, DateTimeUnit.DAY),
                    timeZone = timeZone
                )
            }
            DailyItemRange.ThisMonth -> {
                val start = LocalDate(today.year, today.monthNumber, 1)
                val nextMonth = if (today.monthNumber == 12) {
                    LocalDate(today.year + 1, 1, 1)
                } else {
                    LocalDate(today.year, today.monthNumber + 1, 1)
                }
                DailyItemRangeBounds(
                    startInclusiveEpochMilli = start.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                    endExclusiveEpochMilli = nextMonth.atStartOfDayIn(timeZone).toEpochMilliseconds()
                )
            }
            DailyItemRange.Overdue,
            DailyItemRange.NoDate,
            DailyItemRange.Completed,
            DailyItemRange.All -> DailyItemRangeBounds(null, null)
            is DailyItemRange.Custom -> dateBounds(
                start = range.startInclusive,
                endInclusive = range.endInclusive,
                timeZone = timeZone
            )
        }
    }

    private fun dayBounds(date: LocalDate, timeZone: TimeZone): DailyItemRangeBounds =
        dateBounds(date, date, timeZone)

    private fun dateBounds(
        start: LocalDate,
        endInclusive: LocalDate,
        timeZone: TimeZone,
        includeOverdueCarry: Boolean = false
    ): DailyItemRangeBounds {
        return DailyItemRangeBounds(
            startInclusiveEpochMilli = start.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endExclusiveEpochMilli = endInclusive.plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds(),
            includeOverdueCarry = includeOverdueCarry
        )
    }
}
