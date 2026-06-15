package com.mhss.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mhss.app.database.entity.ReminderEntity

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY id")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query(
        """
        SELECT * FROM reminders
        WHERE target_type = :targetType AND target_id = :targetId
        ORDER BY id
        """
    )
    suspend fun getByTarget(
        targetType: String,
        targetId: String
    ): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY id")
    suspend fun getEnabled(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity): Int

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
