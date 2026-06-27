package com.mhss.app.daily.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.daily.domain.model.DailyItemsPanelConfig
import com.mhss.app.daily.domain.model.DashboardPanel
import com.mhss.app.daily.domain.usecase.EnsureDefaultDashboardPanelsUseCase
import com.mhss.app.daily.domain.usecase.MoveDashboardPanelUseCase
import com.mhss.app.daily.domain.usecase.ObserveDashboardPanelsUseCase
import com.mhss.app.daily.domain.usecase.ResetDashboardPanelsUseCase
import com.mhss.app.daily.domain.usecase.UpdateDashboardPanelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardEditViewModel(
    private val observePanels: ObserveDashboardPanelsUseCase,
    private val ensureDefaultPanels: EnsureDefaultDashboardPanelsUseCase,
    private val updatePanel: UpdateDashboardPanelUseCase,
    private val movePanel: MoveDashboardPanelUseCase,
    private val resetPanels: ResetDashboardPanelsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardEditUiState())
    val uiState: StateFlow<DashboardEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultPanels()
            observePanels().collect { panels ->
                _uiState.update { it.copy(panels = panels) }
            }
        }
    }

    fun setEnabled(panel: DashboardPanel, enabled: Boolean) {
        viewModelScope.launch { updatePanel(panel.copy(enabled = enabled)) }
    }

    fun move(panel: DashboardPanel, delta: Int) {
        viewModelScope.launch { movePanel(panel.id, delta) }
    }

    fun updateDailyItemsConfig(
        panel: DashboardPanel,
        config: DailyItemsPanelConfig
    ) {
        viewModelScope.launch { updatePanel(panel.copy(config = config)) }
    }

    fun reset() {
        viewModelScope.launch { resetPanels() }
    }
}

data class DashboardEditUiState(
    val panels: List<DashboardPanel> = emptyList()
)
