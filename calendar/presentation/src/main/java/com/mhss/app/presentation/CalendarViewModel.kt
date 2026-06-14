package com.mhss.app.presentation

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.domain.model.Calendar
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.use_case.GetAllCalendarsUseCase
import com.mhss.app.domain.use_case.GetAllEventsUseCase
import com.mhss.app.domain.use_case.GetMonthEventsUseCase
import com.mhss.app.domain.use_case.monthGridDateRange
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.booleanPreferencesKey
import com.mhss.app.preferences.domain.model.intPreferencesKey
import com.mhss.app.preferences.domain.model.stringSetPreferencesKey
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.preferences.domain.use_case.SavePreferenceUseCase
import com.mhss.app.presentation.model.CalendarMonth
import com.mhss.app.tracking.domain.model.TrackingCalendarRecord
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.ObserveCalendarRecordsUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.ui.FirstDayOfWeekSettings
import com.mhss.app.ui.toIntList
import com.mhss.app.util.date.currentLocalDate
import com.mhss.app.util.date.formatDateForMapping
import com.mhss.app.util.date.monthName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.minusMonth
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import kotlin.time.Instant
import org.koin.android.annotation.KoinViewModel


const val CALENDAR_START_PAGE = 24000
const val CALENDAR_TOTAL_PAGES = 48000

@KoinViewModel
class CalendarViewModel(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getMonthEventsUseCase: GetMonthEventsUseCase,
    private val getAllCalendarsUseCase: GetAllCalendarsUseCase,
    private val savePreference: SavePreferenceUseCase,
    private val getPreference: GetPreferenceUseCase,
    private val observeCalendarRecords: ObserveCalendarRecordsUseCase,
    private val observeTemplates: ObserveTemplatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val loadMutex = Mutex()

    private var updateEventsJob: Job? = null
    private var viewModeJob: Job? = null
    private var trackingRecordsJob: Job? = null
    private var hasReadCalendarPermission = false

    fun loadMonth(month: YearMonth, forceRefresh: Boolean = false) {
        val loadedMonths = _uiState.value.loadedMonths
        val firstDayOfWeek = _uiState.value.firstDayOfWeek
        viewModelScope.launch {
            loadMutex.withLock {
                val monthData = async {
                    if (!forceRefresh && loadedMonths.containsKey(month)) null
                    else loadCalendarMonth(month, firstDayOfWeek)
                }
                val prevMonthData = async {
                    val prevMonth = month.minusMonth()
                    if (!forceRefresh && loadedMonths.containsKey(prevMonth)) null
                    else loadCalendarMonth(prevMonth, firstDayOfWeek)
                }
                val nextMonthData = async {
                    val nextMonth = month.plusMonth()
                    if (!forceRefresh && loadedMonths.containsKey(nextMonth)) null
                    else loadCalendarMonth(nextMonth, firstDayOfWeek)
                }

                val map = _uiState.value.loadedMonths

                monthData.await()?.let { map[it.month] = it }
                prevMonthData.await()?.let { map[it.month] = it }
                nextMonthData.await()?.let { map[it.month] = it }

                // keeping max of 4 months in memory
                if (map.size > 4) {
                    map.remove(month.minus(3, DateTimeUnit.MONTH))
                    map.remove(month.plus(3, DateTimeUnit.MONTH))
                }
            }
        }
    }

    private suspend fun loadCalendarMonth(
        month: YearMonth,
        firstDayOfWeek: DayOfWeek
    ): CalendarMonth {
        val excludedCalendars = _uiState.value.excludedCalendars
        val days = try {
            getMonthEventsUseCase(
                month = month,
                excludedCalendars = excludedCalendars,
                firstDayOfWeek = firstDayOfWeek,
                includeCalendarEvents = hasReadCalendarPermission
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            getMonthEventsUseCase(
                month = month,
                excludedCalendars = excludedCalendars,
                firstDayOfWeek = firstDayOfWeek,
                includeCalendarEvents = false
            )
        }
        return CalendarMonth(month, days)
    }

    init {
        _uiState.update { it.copy(currentMonth = currentLocalDate()) }
        viewModelScope.launch {
            val value = getPreference(
                intPreferencesKey(PrefsConstants.FIRST_DAY_OF_WEEK_KEY),
                FirstDayOfWeekSettings.SUNDAY.value
            ).first()
            val firstDay = when (FirstDayOfWeekSettings.fromValue(value)) {
                FirstDayOfWeekSettings.SATURDAY -> DayOfWeek.SATURDAY
                FirstDayOfWeekSettings.SUNDAY -> DayOfWeek.SUNDAY
                FirstDayOfWeekSettings.MONDAY -> DayOfWeek.MONDAY
            }
            _uiState.update { it.copy(firstDayOfWeek = firstDay) }
            loadEvents()
            observeTrackingRecords(_uiState.value.currentMonth.yearMonth)
        }
        observeTemplates().onEach { templates ->
            _uiState.update { it.copy(trackingTemplates = templates) }
        }.launchIn(viewModelScope)
        collectViewMode()
    }

    fun onEvent(event: CalendarViewModelEvent) {
        when (event) {
            is CalendarViewModelEvent.IncludeCalendar -> updateExcludedCalendars(
                event.calendar.id.toInt(),
                event.calendar.included
            )

            is CalendarViewModelEvent.ReadPermissionChanged -> {
                hasReadCalendarPermission = event.hasPermission
                _uiState.update { it.copy(hasCalendarPermission = event.hasPermission) }
                if (event.hasPermission) {
                    collectSettings()
                } else {
                    updateEventsJob?.cancel()
                    _uiState.value.loadedMonths.clear()
                    _uiState.update {
                        it.copy(
                            events = emptyMap(),
                            calendars = emptyMap(),
                            months = emptyList()
                        )
                    }
                    loadEvents()
                }
            }

            is CalendarViewModelEvent.MonthChanged -> {
                _uiState.update { it.copy(currentMonth = event.newMonth) }
                observeTrackingRecords(event.newMonth.yearMonth)
            }

            is CalendarViewModelEvent.SelectedDateChanged -> {
                _uiState.update { it.copy(selectedDate = event.newDate) }
            }

            is CalendarViewModelEvent.ViewModeChanged -> {
                viewModelScope.launch {
                    savePreference(
                        booleanPreferencesKey(PrefsConstants.CALENDAR_VIEW_MODE_KEY),
                        event.isMonthView
                    )
                }
                _uiState.update { it.copy(isMonthView = event.isMonthView) }
            }
        }
    }

    private fun updateExcludedCalendars(id: Int, add: Boolean) {
        viewModelScope.launch {
            savePreference(
                stringSetPreferencesKey(PrefsConstants.EXCLUDED_CALENDARS_KEY),
                if (add) _uiState.value.excludedCalendars.addAndToStringSet(id)
                else _uiState.value.excludedCalendars.removeAndToStringSet(id)
            )
        }
    }

    private fun collectSettings() {
        updateEventsJob?.cancel()
        updateEventsJob = getPreference(
            stringSetPreferencesKey(PrefsConstants.EXCLUDED_CALENDARS_KEY),
            emptySet()
        ).onEach { calendarsSet ->
            val calendars = try {
                getAllCalendarsUseCase(calendarsSet.toIntList())
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                emptyMap()
            }
            _uiState.update {state ->
                state.copy(
                    excludedCalendars = calendarsSet.map { it.toInt() }.toMutableList(),
                    calendars = calendars
                )
            }
            loadEvents()
        }.launchIn(viewModelScope)
    }

    private fun collectViewMode() {
        viewModeJob?.cancel()
        viewModeJob = getPreference(
            booleanPreferencesKey(PrefsConstants.CALENDAR_VIEW_MODE_KEY),
            true
        ).onEach { isMonthView ->
            _uiState.update { it.copy(isMonthView = isMonthView) }
            loadEvents()
        }.launchIn(viewModelScope)
    }

    private fun loadEvents() {
        if (_uiState.value.isMonthView) {
            loadMonth(_uiState.value.currentMonth.yearMonth, forceRefresh = true)
            _uiState.update { it.copy(events = emptyMap()) }
        } else if (hasReadCalendarPermission) {
            loadListEvents()
            _uiState.value.loadedMonths.clear()
        } else {
            _uiState.update { it.copy(events = emptyMap(), months = emptyList()) }
        }
    }

    private fun observeTrackingRecords(month: YearMonth) {
        trackingRecordsJob?.cancel()
        val timeZone = TimeZone.currentSystemDefault()
        val range = monthGridDateRange(month, _uiState.value.firstDayOfWeek)
        val startInclusive = range.start.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        val endExclusive = range.endExclusive.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        trackingRecordsJob = observeCalendarRecords(startInclusive, endExclusive)
            .onEach { records ->
                _uiState.update {
                    it.copy(trackingRecordsByDate = records.groupByDeviceDate(timeZone))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadListEvents() {
        viewModelScope.launch {
            val events = getAllEventsUseCase(_uiState.value.excludedCalendars) {
                it.start.formatDateForMapping()
            }
            val months = events.map {
                it.value.first().start.monthName()
            }.distinct()
            _uiState.update { it.copy(events = events, months = months) }
        }
    }

    data class UiState(
        val events: Map<String, List<CalendarEvent>> = emptyMap(),
        val calendars: Map<String, List<Calendar>> = emptyMap(),
        val excludedCalendars: List<Int> = listOf(),
        val months: List<String> = emptyList(),
        val isMonthView: Boolean = false,
        val currentMonth: LocalDate = currentLocalDate(),
        val selectedDate: LocalDate = currentLocalDate(),
        val loadedMonths: SnapshotStateMap<YearMonth, CalendarMonth> = mutableStateMapOf(),
        val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
        val hasCalendarPermission: Boolean = false,
        val trackingTemplates: List<TrackingTemplateSummary> = emptyList(),
        val trackingRecordsByDate: Map<LocalDate, List<TrackingCalendarRecord>> = emptyMap()
    )

    private fun List<Int>.addAndToStringSet(id: Int): Set<String> =
        (this + id).map { it.toString() }.toHashSet()

    private fun List<Int>.removeAndToStringSet(id: Int): Set<String> =
        this.filterNot { it == id }.map { it.toString() }.toHashSet()
}

internal fun List<TrackingCalendarRecord>.groupByDeviceDate(
    timeZone: TimeZone
): Map<LocalDate, List<TrackingCalendarRecord>> = groupBy { record ->
    Instant.fromEpochMilliseconds(record.occurredAtEpochMilli)
        .toLocalDateTime(timeZone)
        .date
}
