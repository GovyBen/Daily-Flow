package com.mhss.app.daily.data

import androidx.room.withTransaction
import com.mhss.app.daily.domain.model.DashboardPanel
import com.mhss.app.daily.domain.repository.DashboardPanelRepository
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.database.dao.DashboardPanelDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomDashboardPanelRepository(
    private val database: MyBrainDatabase,
    private val dashboardPanelDao: DashboardPanelDao,
    private val ioDispatcher: CoroutineDispatcher
) : DashboardPanelRepository {
    override fun observePanels(): Flow<List<DashboardPanel>> {
        return dashboardPanelDao.observeAll()
            .map { panels -> panels.map { it.toDashboardPanel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getPanels(): List<DashboardPanel> = withContext(ioDispatcher) {
        dashboardPanelDao.getAll().map { it.toDashboardPanel() }
    }

    override suspend fun isEmpty(): Boolean = withContext(ioDispatcher) {
        dashboardPanelDao.count() == 0
    }

    override suspend fun upsert(panel: DashboardPanel): DashboardPanel = withContext(ioDispatcher) {
        dashboardPanelDao.upsert(panel.toDashboardPanelEntity())
        panel
    }

    override suspend fun upsertAll(panels: List<DashboardPanel>) {
        withContext(ioDispatcher) {
            dashboardPanelDao.upsertAll(panels.map { it.toDashboardPanelEntity() })
        }
    }

    override suspend fun replaceAll(panels: List<DashboardPanel>) {
        withContext(ioDispatcher) {
            database.withTransaction {
                dashboardPanelDao.deleteAll()
                dashboardPanelDao.upsertAll(panels.map { it.toDashboardPanelEntity() })
            }
        }
    }
}
