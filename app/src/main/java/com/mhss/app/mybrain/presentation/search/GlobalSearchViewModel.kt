package com.mhss.app.mybrain.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.model.DiaryEntry
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.use_case.SearchEntriesUseCase
import com.mhss.app.domain.use_case.SearchEventsByTitleWithinRangeUseCase
import com.mhss.app.domain.use_case.SearchNotesUseCase
import com.mhss.app.domain.use_case.SearchTasksUseCase
import com.mhss.app.tracking.domain.model.TrackingCalendarRecord
import com.mhss.app.tracking.domain.usecase.ObserveCalendarRecordsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit

@KoinViewModel
class GlobalSearchViewModel(
    private val searchTasks: SearchTasksUseCase,
    private val searchEntries: SearchEntriesUseCase,
    private val searchNotes: SearchNotesUseCase,
    private val searchEventsByTitle: SearchEventsByTitleWithinRangeUseCase,
    private val observeCalendarRecords: ObserveCalendarRecordsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var calendarRecordsJob: Job? = null

    init {
        // Preload tracking calendar records for a wide range so we can search them
        val now = Instant.now()
        val start = now.minus(365 * 5, ChronoUnit.DAYS).toEpochMilli()
        val end = now.plus(365, ChronoUnit.DAYS).toEpochMilli()
        calendarRecordsJob = viewModelScope.launch {
            observeCalendarRecords(start, end)
                .map { records -> records.distinctBy { it.id } }
                .collect { records ->
                    _uiState.update { it.copy(allCalendarRecords = records) }
                }
        }
    }

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            _uiState.update { it.copy(query = "", isLoading = false, results = emptyList()) }
            return
        }
        _uiState.update { it.copy(query = normalizedQuery, isLoading = true) }
        searchJob = viewModelScope.launch {
            try {
                val tasksDeferred = async { searchTasks(normalizedQuery).first() }
                val entriesDeferred = async { searchEntries(normalizedQuery) }
                val notesDeferred = async { searchNotes(normalizedQuery) }
                val eventsDeferred = async {
                    val now = Instant.now()
                    searchEventsByTitle(
                        startMillis = now.minus(365 * 5, ChronoUnit.DAYS).toEpochMilli(),
                        endMillis = now.plus(365, ChronoUnit.DAYS).toEpochMilli(),
                        titleQuery = normalizedQuery
                    )
                }

                val tasks = tasksDeferred.await()
                val entries = entriesDeferred.await()
                val notes = notesDeferred.await()
                val events = eventsDeferred.await()

                val calendarRecords = _uiState.value.allCalendarRecords
                val matchingRecords = calendarRecords.filter { record ->
                    record.templateName.contains(normalizedQuery, ignoreCase = true) ||
                        record.note?.contains(normalizedQuery, ignoreCase = true) == true
                }

                val results = buildList {
                    addAll(tasks.map { it.toSearchResult() })
                    addAll(events.map { it.toSearchResult() })
                    addAll(matchingRecords.map { it.toSearchResult() })
                    addAll(entries.map { it.toSearchResult() })
                    addAll(notes.map { it.toSearchResult() })
                }.sortedByDescending { it.sortPriority }

                _uiState.update { it.copy(isLoading = false, results = results) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        calendarRecordsJob?.cancel()
    }
}

data class GlobalSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val allCalendarRecords: List<TrackingCalendarRecord> = emptyList()
)

sealed class SearchResult(
    open val id: String,
    open val type: SearchResultType,
    open val title: String,
    open val subtitle: String
) {
    val sortPriority: Int
        get() = when (this) {
            is TaskResult -> 4
            is EventResult -> 3
            is DiaryResult -> 2
            is NoteResult -> 1
            is TrackingResult -> 0
        }

    data class TaskResult(
        val taskId: String,
        val taskTitle: String,
        val taskSubtitle: String
    ) : SearchResult(
        id = taskId,
        type = SearchResultType.TASKS,
        title = taskTitle,
        subtitle = taskSubtitle
    )

    data class EventResult(
        val eventId: Long,
        val eventTitle: String,
        val eventSubtitle: String
    ) : SearchResult(
        id = eventId.toString(),
        type = SearchResultType.EVENTS,
        title = eventTitle,
        subtitle = eventSubtitle
    )

    data class DiaryResult(
        val entryId: String,
        val entryTitle: String,
        val entrySubtitle: String
    ) : SearchResult(
        id = entryId,
        type = SearchResultType.DIARY,
        title = entryTitle,
        subtitle = entrySubtitle
    )

    data class NoteResult(
        val noteId: String,
        val noteTitle: String,
        val noteSubtitle: String,
        val folderId: String?
    ) : SearchResult(
        id = noteId,
        type = SearchResultType.NOTES,
        title = noteTitle,
        subtitle = noteSubtitle
    )

    data class TrackingResult(
        val recordId: String,
        val templateId: String,
        val sessionName: String,
        val sessionSubtitle: String
    ) : SearchResult(
        id = recordId,
        type = SearchResultType.RECORDS,
        title = sessionName,
        subtitle = sessionSubtitle
    )
}

enum class SearchResultType {
    TASKS, EVENTS, DIARY, NOTES, RECORDS
}

private fun Task.toSearchResult(): SearchResult.TaskResult {
    val subtitle = description.takeIf(String::isNotBlank)?.take(80) ?: "Task"
    return SearchResult.TaskResult(
        taskId = id,
        taskTitle = title,
        taskSubtitle = subtitle
    )
}

private fun CalendarEvent.toSearchResult(): SearchResult.EventResult {
    val subtitle = description?.takeIf(String::isNotBlank)?.take(80)
        ?: location?.take(80)
        ?: "Event"
    return SearchResult.EventResult(
        eventId = id,
        eventTitle = title,
        eventSubtitle = subtitle
    )
}

private fun TrackingCalendarRecord.toSearchResult(): SearchResult.TrackingResult {
    val subtitle = note?.takeIf(String::isNotBlank)?.take(80) ?: "Tracking record"
    return SearchResult.TrackingResult(
        recordId = id,
        templateId = templateId,
        sessionName = templateName,
        sessionSubtitle = subtitle
    )
}

private fun DiaryEntry.toSearchResult(): SearchResult.DiaryResult {
    val subtitle = if (content.isNotBlank()) {
        content.lineSequence().firstOrNull(String::isNotBlank)?.take(80) ?: "Diary entry"
    } else {
        "Diary entry"
    }
    return SearchResult.DiaryResult(
        entryId = id,
        entryTitle = title.ifBlank { "Untitled" },
        entrySubtitle = subtitle
    )
}

private fun Note.toSearchResult(): SearchResult.NoteResult {
    val subtitle = if (content.isNotBlank()) {
        content.lineSequence().firstOrNull(String::isNotBlank)?.take(80) ?: "Note"
    } else {
        "Note"
    }
    return SearchResult.NoteResult(
        noteId = id,
        noteTitle = title.ifBlank { "Untitled" },
        noteSubtitle = subtitle,
        folderId = folderId
    )
}
