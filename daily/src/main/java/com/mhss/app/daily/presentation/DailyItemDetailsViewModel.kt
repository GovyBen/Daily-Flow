package com.mhss.app.daily.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.usecase.ArchiveDailyItemUseCase
import com.mhss.app.daily.domain.usecase.CompleteDailyItemUseCase
import com.mhss.app.daily.domain.usecase.DeleteDailyItemUseCase
import com.mhss.app.daily.domain.usecase.DisableDailyItemCalendarSyncUseCase
import com.mhss.app.daily.domain.usecase.GetDailyItemUseCase
import com.mhss.app.daily.domain.usecase.ReopenDailyItemUseCase
import com.mhss.app.daily.domain.usecase.SyncDailyItemToCalendarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DailyItemDetailsViewModel(
    private val itemId: String,
    private val getDailyItem: GetDailyItemUseCase,
    private val completeDailyItem: CompleteDailyItemUseCase,
    private val reopenDailyItem: ReopenDailyItemUseCase,
    private val archiveDailyItem: ArchiveDailyItemUseCase,
    private val deleteDailyItem: DeleteDailyItemUseCase,
    private val syncDailyItemToCalendar: SyncDailyItemToCalendarUseCase,
    private val disableDailyItemCalendarSync: DisableDailyItemCalendarSyncUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DailyItemDetailsUiState())
    val uiState: StateFlow<DailyItemDetailsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(item = getDailyItem(itemId), isLoading = false) }
        }
    }

    fun toggleComplete() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            if (item.isCompleted) reopenDailyItem(item.id) else completeDailyItem(item.id)
            reload()
        }
    }

    fun archive() {
        viewModelScope.launch {
            archiveDailyItem(itemId)
            _uiState.update { it.copy(navigateUp = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteDailyItem(itemId)
            _uiState.update { it.copy(navigateUp = true) }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncDailyItemToCalendar(itemId)
            reload()
        }
    }

    fun disableSync() {
        viewModelScope.launch {
            disableDailyItemCalendarSync(itemId)
            reload()
        }
    }
}

data class DailyItemDetailsUiState(
    val item: DailyItem? = null,
    val isLoading: Boolean = true,
    val navigateUp: Boolean = false
)
