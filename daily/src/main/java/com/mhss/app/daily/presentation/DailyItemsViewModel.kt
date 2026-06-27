package com.mhss.app.daily.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemFilter
import com.mhss.app.daily.domain.model.DailyItemRange
import com.mhss.app.daily.domain.model.DailyItemStatusFilter
import com.mhss.app.daily.domain.usecase.CompleteDailyItemUseCase
import com.mhss.app.daily.domain.usecase.ObserveDailyItemsUseCase
import com.mhss.app.daily.domain.usecase.ReopenDailyItemUseCase
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DailyItemsViewModel(
    private val observeDailyItems: ObserveDailyItemsUseCase,
    private val completeDailyItem: CompleteDailyItemUseCase,
    private val reopenDailyItem: ReopenDailyItemUseCase
) : ViewModel() {
    private val filter = MutableStateFlow(DailyItemFilter())
    private val _uiState = MutableStateFlow(DailyItemsUiState())
    val uiState: StateFlow<DailyItemsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            filter.flatMapLatest { activeFilter ->
                observeDailyItems(activeFilter)
            }.collect { items ->
                _uiState.update {
                    it.copy(
                        items = items,
                        filter = filter.value,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setRange(range: DailyItemRange) {
        val status = if (range == DailyItemRange.Completed) {
            DailyItemStatusFilter.Completed
        } else {
            DailyItemStatusFilter.Active
        }
        filter.update {
            it.copy(
                range = range,
                status = status,
                includeCompleted = range == DailyItemRange.Completed
            )
        }
        _uiState.update { it.copy(isLoading = true) }
    }

    fun setCustomRange(startInclusive: LocalDate, endInclusive: LocalDate) {
        val start = minOf(startInclusive, endInclusive)
        val end = maxOf(startInclusive, endInclusive)
        setRange(DailyItemRange.Custom(start, end))
    }

    fun setQuery(query: String) {
        filter.update { it.copy(query = query) }
    }

    fun setMode(mode: DailyItemsMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun toggleCompleted(item: DailyItem) {
        viewModelScope.launch {
            if (item.isCompleted) {
                reopenDailyItem(item.id)
            } else {
                completeDailyItem(item.id)
            }
        }
    }
}

data class DailyItemsUiState(
    val items: List<DailyItem> = emptyList(),
    val filter: DailyItemFilter = DailyItemFilter(),
    val mode: DailyItemsMode = DailyItemsMode.LIST,
    val isLoading: Boolean = true
)

enum class DailyItemsMode {
    LIST,
    MONTH
}
