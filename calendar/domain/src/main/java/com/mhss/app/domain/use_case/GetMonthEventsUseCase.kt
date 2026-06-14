package com.mhss.app.domain.use_case

import com.mhss.app.domain.MONTH_GRID_CELL_COUNT
import com.mhss.app.domain.model.CalendarDay
import com.mhss.app.domain.model.CalendarEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory
class GetMonthEventsUseCase(
    private val getEventsWithinRangeUseCase: GetEventsWithinRangeUseCase,
    @Named("defaultDispatcher") private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        month: YearMonth,
        excludedCalendars: List<Int>,
        firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
        includeCalendarEvents: Boolean = true,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<CalendarDay> {
        return withContext(defaultDispatcher) {
            val range = monthGridDateRange(month, firstDayOfWeek)
            val startMillis = range.start
                .atTime(hour = 0, minute = 0)
                .toInstant(timeZone)
                .toEpochMilliseconds()
            val endMillis = range.endExclusive
                .atTime(hour = 0, minute = 0)
                .toInstant(timeZone)
                .toEpochMilliseconds()
            val events = if (includeCalendarEvents) {
                getEventsWithinRangeUseCase(startMillis, endMillis, excludedCalendars)
            } else {
                emptyList()
            }
            val eventsByDate = events
                .flatMap { event ->
                    event.calendarDates(timeZone)
                        .filter { it >= range.start && it < range.endExclusive }
                        .map { date -> date to event }
                }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })

            (0 until MONTH_GRID_CELL_COUNT).map { index ->
                val dayDate = range.start.plus(index, DateTimeUnit.DAY)
                CalendarDay(
                    date = dayDate,
                    isCurrentMonth = dayDate.month == month.month && dayDate.year == month.year,
                    events = eventsByDate[dayDate].orEmpty()
                )
            }
        }
    }
}

data class MonthGridDateRange(
    val start: LocalDate,
    val endExclusive: LocalDate
)

fun monthGridDateRange(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek
): MonthGridDateRange {
    val firstOfMonth = month.firstDay
    val startOffset = firstOfMonth.dayOfWeek.dayNumberFrom(firstDayOfWeek)
    val start = firstOfMonth.minus(startOffset, DateTimeUnit.DAY)
    return MonthGridDateRange(
        start = start,
        endExclusive = start.plus(MONTH_GRID_CELL_COUNT, DateTimeUnit.DAY)
    )
}

private fun CalendarEvent.calendarDates(timeZone: TimeZone): List<LocalDate> {
    val eventTimeZone = if (allDay) TimeZone.UTC else timeZone
    val startDate = Instant.fromEpochMilliseconds(start)
        .toLocalDateTime(eventTimeZone)
        .date
    val lastIncludedMillis = maxOf(start, end - 1)
    val endDate = Instant.fromEpochMilliseconds(lastIncludedMillis)
        .toLocalDateTime(eventTimeZone)
        .date
    return buildList {
        var date = startDate
        while (date <= endDate) {
            add(date)
            date = date.plus(1, DateTimeUnit.DAY)
        }
    }
}

fun DayOfWeek.dayNumberFrom(firstDayOfWeek: DayOfWeek): Int {
    val thisDayOrdinal = this.ordinal
    val firstDayOrdinal = firstDayOfWeek.ordinal
    return (thisDayOrdinal - firstDayOrdinal + 7) % 7
}

