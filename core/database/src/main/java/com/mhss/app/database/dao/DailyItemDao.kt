package com.mhss.app.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mhss.app.database.entity.DailyItemCalendarSyncEntity
import com.mhss.app.database.entity.DailyItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyItemDao {
    @Query("SELECT * FROM daily_items")
    fun observeAll(): Flow<List<DailyItemEntity>>

    @Query("SELECT * FROM daily_items WHERE id = :id")
    fun observeItem(id: String): Flow<DailyItemEntity?>

    @Query("SELECT * FROM daily_items WHERE id = :id")
    suspend fun getItem(id: String): DailyItemEntity?

    @Query("SELECT * FROM daily_items ORDER BY updated_at DESC")
    suspend fun getAll(): List<DailyItemEntity>

    @Upsert
    suspend fun upsert(item: DailyItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<DailyItemEntity>)

    @Query("DELETE FROM daily_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM daily_item_calendar_sync")
    fun observeAllCalendarSync(): Flow<List<DailyItemCalendarSyncEntity>>

    @Query("SELECT * FROM daily_item_calendar_sync")
    suspend fun getAllCalendarSync(): List<DailyItemCalendarSyncEntity>

    @Query("SELECT * FROM daily_item_calendar_sync WHERE item_id = :itemId")
    suspend fun getCalendarSync(itemId: String): DailyItemCalendarSyncEntity?

    @Upsert
    suspend fun upsertCalendarSync(sync: DailyItemCalendarSyncEntity)

    @Upsert
    suspend fun upsertCalendarSync(sync: List<DailyItemCalendarSyncEntity>)
}
