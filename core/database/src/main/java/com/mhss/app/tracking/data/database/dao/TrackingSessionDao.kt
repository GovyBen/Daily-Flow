package com.mhss.app.tracking.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingSessionDao {

    @Query("SELECT * FROM tracking_record_sessions WHERE id = :id")
    suspend fun getSession(id: String): RecordSessionEntity?

    @Query(
        """
        SELECT * FROM tracking_record_sessions
        WHERE template_id = :templateId
          AND occurred_at_epoch_milli >= :startInclusive
          AND occurred_at_epoch_milli < :endExclusive
        ORDER BY occurred_at_epoch_milli, id
        """
    )
    fun observeSessionsInRange(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<RecordSessionEntity>>

    @Query(
        """
        SELECT * FROM tracking_record_sessions
        WHERE template_id = :templateId
          AND occurred_at_epoch_milli >= :startInclusive
          AND occurred_at_epoch_milli < :endExclusive
        ORDER BY occurred_at_epoch_milli, id
        """
    )
    suspend fun getSessionsInRange(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): List<RecordSessionEntity>

    @Insert
    suspend fun insertSession(session: RecordSessionEntity)

    @Update
    suspend fun updateSession(session: RecordSessionEntity)

    @Delete
    suspend fun deleteSession(session: RecordSessionEntity)
}
