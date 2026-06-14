package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.Calendar
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMonthEventsUseCaseTest {

    @Test
    fun timedEventSpanningDstAndMidnightAppearsOnEveryLocalDate() = runBlocking {
        val timeZone = TimeZone.of("America/New_York")
        val event = CalendarEvent(
            id = 1,
            title = "Long event",
            start = LocalDateTime(2026, 3, 7, 23, 30).toInstant(timeZone).toEpochMilliseconds(),
            end = LocalDateTime(2026, 3, 9, 0, 30).toInstant(timeZone).toEpochMilliseconds(),
            calendarId = 1
        )
        val days = useCase(listOf(event))(
            month = YearMonth(2026, 3),
            excludedCalendars = emptyList(),
            firstDayOfWeek = DayOfWeek.SUNDAY,
            timeZone = timeZone
        )

        assertEquals(
            listOf(
                LocalDate(2026, 3, 7),
                LocalDate(2026, 3, 8),
                LocalDate(2026, 3, 9)
            ),
            days.filter { day -> day.events.any { it.id == event.id } }.map { it.date }
        )
    }

    @Test
    fun allDayEventUsesUtcCalendarDates() = runBlocking {
        val event = CalendarEvent(
            id = 2,
            title = "All day",
            start = LocalDateTime(2026, 3, 10, 0, 0)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds(),
            end = LocalDateTime(2026, 3, 12, 0, 0)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds(),
            allDay = true,
            calendarId = 1
        )
        val days = useCase(listOf(event))(
            month = YearMonth(2026, 3),
            excludedCalendars = emptyList(),
            firstDayOfWeek = DayOfWeek.SUNDAY,
            timeZone = TimeZone.of("America/Los_Angeles")
        )

        assertEquals(
            listOf(LocalDate(2026, 3, 10), LocalDate(2026, 3, 11)),
            days.filter { day -> day.events.any { it.id == event.id } }.map { it.date }
        )
    }

    @Test
    fun calendarProviderIsNotQueriedWithoutPermission() = runBlocking {
        val repository = FakeCalendarRepository(emptyList())
        val useCase = GetMonthEventsUseCase(
            GetEventsWithinRangeUseCase(repository),
            Dispatchers.Unconfined
        )

        val days = useCase(
            month = YearMonth(2026, 3),
            excludedCalendars = emptyList(),
            includeCalendarEvents = false
        )

        assertEquals(42, days.size)
        assertTrue(days.all { it.events.isEmpty() })
        assertEquals(0, repository.rangeQueries)
    }

    private fun useCase(events: List<CalendarEvent>) = GetMonthEventsUseCase(
        GetEventsWithinRangeUseCase(FakeCalendarRepository(events)),
        Dispatchers.Unconfined
    )
}

private class FakeCalendarRepository(
    private val events: List<CalendarEvent>
) : CalendarRepository {
    var rangeQueries = 0

    override suspend fun getEvents(
        excludedCalendars: List<Int>,
        until: Long?
    ): List<CalendarEvent> = events

    override suspend fun getEvents(
        start: Long,
        end: Long,
        excludedCalendars: List<Int>
    ): List<CalendarEvent> {
        rangeQueries += 1
        return events
    }

    override suspend fun searchEventsByTitleWithinRange(
        start: Long,
        end: Long,
        titleQuery: String,
        excludedCalendars: List<Int>
    ): List<CalendarEvent> = emptyList()

    override suspend fun getCalendars(): List<Calendar> = emptyList()
    override suspend fun getEventById(id: Long): CalendarEvent? = null
    override suspend fun addEvent(event: CalendarEvent): Long? = null
    override suspend fun deleteEvent(event: CalendarEvent) = Unit
    override suspend fun updateEvent(event: CalendarEvent) = Unit
    override suspend fun createCalendar() = Unit
}
