package com.mhss.app.daily.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemCalendarSync
import com.mhss.app.daily.domain.model.DailyItemKind
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.daily.domain.model.DailyItemSchedule
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.daily.domain.usecase.CreateDailyItemUseCase
import com.mhss.app.daily.domain.usecase.GetDailyItemUseCase
import com.mhss.app.daily.domain.usecase.SyncDailyItemToCalendarUseCase
import com.mhss.app.daily.domain.usecase.UpdateDailyItemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class DailyItemEditorViewModel(
    itemId: String,
    private val getDailyItem: GetDailyItemUseCase,
    private val createDailyItem: CreateDailyItemUseCase,
    private val updateDailyItem: UpdateDailyItemUseCase,
    private val syncDailyItemToCalendar: SyncDailyItemToCalendarUseCase
) : ViewModel() {
    private val editingItemId = itemId.takeIf { it.isNotBlank() }
    private val _uiState = MutableStateFlow(DailyItemEditorUiState())
    val uiState: StateFlow<DailyItemEditorUiState> = _uiState.asStateFlow()

    init {
        editingItemId?.let { id ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                _uiState.update { state ->
                    state.copy(
                        item = getDailyItem(id),
                        isLoading = false
                    )
                }
            }
        } ?: _uiState.update { it.copy(isLoading = false) }
    }

    fun save(draft: DailyItemEditorDraft) {
        if (draft.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val old = _uiState.value.item
            val item = DailyItem(
                id = old?.id.orEmpty(),
                title = draft.title.trim(),
                description = draft.description.trim(),
                kind = draft.kind,
                schedule = DailyItemSchedule(
                    startAtEpochMilli = draft.startAtEpochMilli,
                    endAtEpochMilli = draft.endAtEpochMilli,
                    dueAtEpochMilli = draft.dueAtEpochMilli,
                    allDay = draft.allDay,
                    timeZoneId = TimeZone.currentSystemDefault().id
                ),
                isCompletable = draft.isCompletable,
                completedAtEpochMilli = old?.completedAtEpochMilli,
                status = old?.status ?: DailyItemStatus.ACTIVE,
                priority = draft.priority,
                calendarSync = (old?.calendarSync ?: DailyItemCalendarSync()).copy(
                    enabled = draft.syncToCalendar,
                    state = if (draft.syncToCalendar) CalendarSyncState.DIRTY else CalendarSyncState.NOT_SYNCED,
                    lastError = null
                ),
                legacySource = old?.legacySource,
                subTasksJson = old?.subTasksJson ?: "[]",
                createdAtEpochMilli = old?.createdAtEpochMilli ?: now,
                updatedAtEpochMilli = now
            )
            val saved = if (old == null) createDailyItem(item) else updateDailyItem(item)
            if (saved.calendarSync.enabled) syncDailyItemToCalendar(saved.id)
            _uiState.update { it.copy(item = saved, saved = true, titleError = false) }
        }
    }
}

data class DailyItemEditorUiState(
    val item: DailyItem? = null,
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val titleError: Boolean = false
)

data class DailyItemEditorDraft(
    val title: String,
    val description: String,
    val kind: DailyItemKind,
    val priority: DailyItemPriority,
    val startAtEpochMilli: Long?,
    val endAtEpochMilli: Long?,
    val dueAtEpochMilli: Long?,
    val allDay: Boolean,
    val isCompletable: Boolean,
    val syncToCalendar: Boolean
)
