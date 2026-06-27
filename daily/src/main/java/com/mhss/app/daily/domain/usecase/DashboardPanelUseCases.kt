package com.mhss.app.daily.domain.usecase

import com.mhss.app.daily.domain.model.DashboardPanel
import com.mhss.app.daily.domain.model.DashboardPanelConfig
import com.mhss.app.daily.domain.model.DashboardPanelSize
import com.mhss.app.daily.domain.model.DashboardPanelType
import com.mhss.app.daily.domain.model.defaultConfig
import com.mhss.app.daily.domain.repository.DashboardPanelRepository
import kotlinx.coroutines.flow.Flow

class ObserveDashboardPanelsUseCase(
    private val repository: DashboardPanelRepository
) {
    operator fun invoke(): Flow<List<DashboardPanel>> = repository.observePanels()
}

class EnsureDefaultDashboardPanelsUseCase(
    private val repository: DashboardPanelRepository
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()) {
        if (repository.isEmpty()) {
            repository.upsertAll(defaultDashboardPanels(now))
        }
    }
}

class ResetDashboardPanelsUseCase(
    private val repository: DashboardPanelRepository
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()) {
        repository.replaceAll(defaultDashboardPanels(now))
    }
}

class UpdateDashboardPanelUseCase(
    private val repository: DashboardPanelRepository
) {
    suspend operator fun invoke(panel: DashboardPanel): DashboardPanel {
        return repository.upsert(panel.copy(updatedAtEpochMilli = System.currentTimeMillis()))
    }
}

class MoveDashboardPanelUseCase(
    private val repository: DashboardPanelRepository
) {
    suspend operator fun invoke(panelId: String, delta: Int) {
        val panels = repository.getPanels()
        val index = panels.indexOfFirst { it.id == panelId }
        if (index == -1) return
        val targetIndex = (index + delta).coerceIn(panels.indices)
        if (targetIndex == index) return
        val reordered = panels.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }.mapIndexed { order, panel ->
            panel.copy(displayOrder = order, updatedAtEpochMilli = System.currentTimeMillis())
        }
        repository.upsertAll(reordered)
    }
}

class SaveDashboardPanelConfigUseCase(
    private val repository: DashboardPanelRepository
) {
    suspend operator fun invoke(
        panel: DashboardPanel,
        config: DashboardPanelConfig
    ): DashboardPanel {
        return repository.upsert(
            panel.copy(
                config = config,
                updatedAtEpochMilli = System.currentTimeMillis()
            )
        )
    }
}

fun defaultDashboardPanels(now: Long): List<DashboardPanel> {
    val defaults = listOf(
        DashboardPanelType.QUICK_RECORD to DashboardPanelSize.MEDIUM,
        DashboardPanelType.DAILY_ITEMS to DashboardPanelSize.LARGE,
        DashboardPanelType.OVERDUE_ITEMS to DashboardPanelSize.COMPACT,
        DashboardPanelType.TRACKING_SUMMARY to DashboardPanelSize.MEDIUM,
        DashboardPanelType.PENDING_REMINDERS to DashboardPanelSize.MEDIUM,
        DashboardPanelType.POMODORO to DashboardPanelSize.COMPACT
    )
    return defaults.mapIndexed { index, (type, size) ->
        DashboardPanel(
            id = type.name.lowercase(),
            type = type,
            enabled = true,
            displayOrder = index,
            size = size,
            config = type.defaultConfig(),
            createdAtEpochMilli = now,
            updatedAtEpochMilli = now
        )
    }
}
