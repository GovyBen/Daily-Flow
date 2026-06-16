package com.mhss.app.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.mhss.app.data.llmDateTimeFormatUnicode
import com.mhss.app.data.parseDateTimeFromLLM
import com.mhss.app.domain.model.Calendar
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.model.CalendarEventFrequency
import com.mhss.app.domain.use_case.AddCalendarEventUseCase
import com.mhss.app.domain.use_case.GetAllCalendarsUseCase
import com.mhss.app.domain.use_case.GetEventsWithinRangeUseCase
import com.mhss.app.domain.use_case.SearchEventsByTitleWithinRangeUseCase
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.stringSetPreferencesKey
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Factory
import java.util.Locale

@Factory
class CalendarToolSet(
    private val getEventsWithinRangeUseCase: GetEventsWithinRangeUseCase,
    private val searchEventsByTitleWithinRangeUseCase: SearchEventsByTitleWithinRangeUseCase,
    private val addCalendarEvent: AddCalendarEventUseCase,
    private val getAllCalendarsUseCase: GetAllCalendarsUseCase,
    private val getPreference: GetPreferenceUseCase
) : ToolSet {

    @Tool(GET_EVENTS_WITHIN_RANGE_TOOL)
    @LLMDescription("Get events within date range.")
    suspend fun getEventsWithinRange(
        @LLMDescription("Format: $llmDateTimeFormatUnicode") startDateTime: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") endDateTime: String
    ): GetEventsResult {
        val s = startDateTime.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid start date")
        val e = endDateTime.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid end date")
        return GetEventsResult(getEventsWithinRangeUseCase(s, e, getExcludedCalendars()))
    }

    @Tool(SEARCH_EVENTS_BY_NAME_WITHIN_RANGE_TOOL)
    suspend fun searchEventsByNameWithinRange(eventName: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") startDateTime: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") endDateTime: String): SearchEventsResult {
        val s = startDateTime.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid start")
        val e = endDateTime.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid end")
        return SearchEventsResult(searchEventsByTitleWithinRangeUseCase(s, e, eventName.trim(), getExcludedCalendars()))
    }

    @Tool(PROPOSE_CREATE_EVENT_TOOL)
    @LLMDescription("Propose creating a calendar event. Returns a proposal ID for user confirmation.")
    suspend fun proposeCreateEvent(
        title: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") start: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") end: String,
        @LLMDescription("Use getAllCalendars to get ID") calendarId: Long,
        description: String? = null, location: String? = null, allDay: Boolean = false,
        recurring: Boolean = false, frequency: CalendarEventFrequency = CalendarEventFrequency.NEVER,
        interval: Int = 1, weekDays: List<String> = emptyList()
    ): ProposalResult {
        val s = start.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid start")
        val e = end.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid end")
        return ProposalResult(proposalId = java.util.UUID.randomUUID().toString(),
            summary = "Create event: $title", proposalJson = "")
    }

    // Legacy direct-create
    @Tool(CREATE_EVENT_TOOL) @LLMDescription("Create event directly (legacy).")
    suspend fun createEvent(title: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") start: String,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") end: String, calendarId: Long,
        description: String? = null, location: String? = null, allDay: Boolean = false,
        recurring: Boolean = false, frequency: CalendarEventFrequency = CalendarEventFrequency.NEVER,
        interval: Int = 1, weekDays: List<String> = emptyList()): CalendarEventIdResult {
        val s = start.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid start")
        val e = end.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid end")
        val event = CalendarEvent(id = 0, title = title, description = description, start = s, end = e,
            location = location, allDay = allDay, calendarId = calendarId, recurring = recurring,
            frequency = frequency, interval = interval.coerceAtLeast(1),
            weekDays = weekDays.mapNotNull { it.toDayOfWeekOrNull() }.toHashSet())
        return CalendarEventIdResult(createdEventId = addCalendarEvent(event))
    }

    @Tool(CREATE_EVENTS_TOOL)
    suspend fun createEvents(events: List<CalendarEventInput>): CalendarEventIdsResult {
        val ids = events.map { input ->
            val s = input.start.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid start")
            val e = input.end.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid end")
            val event = CalendarEvent(id = 0, title = input.title, description = input.description,
                start = s, end = e, location = input.location, allDay = input.allDay,
                calendarId = input.calendarId, recurring = input.recurring, frequency = input.frequency,
                interval = input.interval.coerceAtLeast(1),
                weekDays = input.weekDays.mapNotNull { it.toDayOfWeekOrNull() }.toHashSet())
            addCalendarEvent(event)
        }
        return CalendarEventIdsResult(createdEventIds = ids)
    }

    @Tool(GET_ALL_CALENDARS_TOOL) suspend fun getAllCalendars() =
        GetCalendarsResult(getAllCalendarsUseCase(getExcludedCalendars()))

    private suspend fun getExcludedCalendars() =
        getPreference(stringSetPreferencesKey(PrefsConstants.EXCLUDED_CALENDARS_KEY), emptySet())
            .firstOrNull().orEmpty().mapNotNull { it.toIntOrNull() }
}

private fun String.toDayOfWeekOrNull(): DayOfWeek? = when (trim().uppercase(Locale.US)) {
    "MONDAY","MON","MO" -> DayOfWeek.MONDAY; "TUESDAY","TUE","TU" -> DayOfWeek.TUESDAY
    "WEDNESDAY","WED","WE" -> DayOfWeek.WEDNESDAY; "THURSDAY","THU","TH" -> DayOfWeek.THURSDAY
    "FRIDAY","FRI","FR" -> DayOfWeek.FRIDAY; "SATURDAY","SAT","SA" -> DayOfWeek.SATURDAY
    "SUNDAY","SUN","SU" -> DayOfWeek.SUNDAY; else -> null
}

const val PROPOSE_CREATE_EVENT_TOOL = "proposeCreateEvent"

@Serializable data class CalendarEventInput(
    val title: String, @param:LLMDescription("Format: $llmDateTimeFormatUnicode") val start: String,
    @param:LLMDescription("Format: $llmDateTimeFormatUnicode") val end: String, val calendarId: Long,
    val description: String? = null, val location: String? = null, val allDay: Boolean = false,
    val recurring: Boolean = false, val frequency: CalendarEventFrequency = CalendarEventFrequency.NEVER,
    val interval: Int = 1, val weekDays: List<String> = emptyList())
@Serializable data class GetEventsResult(val events: List<CalendarEvent>)
@Serializable data class SearchEventsResult(val events: List<CalendarEvent>)
@Serializable data class GetCalendarsResult(val calendars: Map<String, List<Calendar>>)
@Serializable data class CalendarEventIdResult(val createdEventId: Long?)
@Serializable data class CalendarEventIdsResult(val createdEventIds: List<Long?>)
