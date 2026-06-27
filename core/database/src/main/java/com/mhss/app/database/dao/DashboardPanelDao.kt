package com.mhss.app.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mhss.app.database.entity.DashboardPanelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardPanelDao {
    @Query("SELECT * FROM dashboard_panels ORDER BY display_order ASC")
    fun observeAll(): Flow<List<DashboardPanelEntity>>

    @Query("SELECT * FROM dashboard_panels ORDER BY display_order ASC")
    suspend fun getAll(): List<DashboardPanelEntity>

    @Query("SELECT COUNT(*) FROM dashboard_panels")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(panel: DashboardPanelEntity)

    @Upsert
    suspend fun upsertAll(panels: List<DashboardPanelEntity>)

    @Query("DELETE FROM dashboard_panels")
    suspend fun deleteAll()
}
