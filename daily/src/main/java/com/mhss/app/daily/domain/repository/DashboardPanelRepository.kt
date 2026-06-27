package com.mhss.app.daily.domain.repository

import com.mhss.app.daily.domain.model.DashboardPanel
import kotlinx.coroutines.flow.Flow

interface DashboardPanelRepository {
    fun observePanels(): Flow<List<DashboardPanel>>
    suspend fun getPanels(): List<DashboardPanel>
    suspend fun isEmpty(): Boolean
    suspend fun upsert(panel: DashboardPanel): DashboardPanel
    suspend fun upsertAll(panels: List<DashboardPanel>)
    suspend fun replaceAll(panels: List<DashboardPanel>)
}
