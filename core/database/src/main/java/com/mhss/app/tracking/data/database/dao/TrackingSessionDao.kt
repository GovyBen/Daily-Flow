package com.mhss.app.tracking.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import kotlinx.coroutines.flow.Flow

data class TemplateLastRecordedAt(
    @androidx.room.ColumnInfo(name = "template_id")
    val templateId: String,
    @androidx.room.ColumnInfo(name = "last_recorded_at_epoch_milli")
    val lastRecordedAtEpochMilli: Long
)

data class TrackingCalendarRecordRow(
    val id: String,
    @androidx.room.ColumnInfo(name = "template_id")
    val templateId: String,
    @androidx.room.ColumnInfo(name = "template_name")
    val templateName: String,
    @androidx.room.ColumnInfo(name = "template_color")
    val templateColor: Long,
    @androidx.room.ColumnInfo(name = "occurred_at_epoch_milli")
    val occurredAtEpochMilli: Long,
    @androidx.room.ColumnInfo(name = "zone_id")
    val zoneId: String,
    val note: String?,
    val source: String
)

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

    @Query(
        """
        SELECT sessions.id,
               sessions.template_id,
               templates.name AS template_name,
               templates.color AS template_color,
               sessions.occurred_at_epoch_milli,
               sessions.zone_id,
               sessions.note,
               sessions.source
        FROM tracking_record_sessions AS sessions
        INNER JOIN tracking_templates AS templates
            ON templates.id = sessions.template_id
        WHERE sessions.occurred_at_epoch_milli >= :startInclusive
          AND sessions.occurred_at_epoch_milli < :endExclusive
        ORDER BY sessions.occurred_at_epoch_milli, sessions.id
        """
    )
    fun observeCalendarRecords(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TrackingCalendarRecordRow>>

    @Query(
        """
        SELECT template_id, MAX(occurred_at_epoch_milli) AS last_recorded_at_epoch_milli
        FROM tracking_record_sessions
        GROUP BY template_id
        """
    )
    fun observeLastRecordedTimes(): Flow<List<TemplateLastRecordedAt>>

    @Insert
    suspend fun insertSession(session: RecordSessionEntity)

    @Update
    suspend fun updateSession(session: RecordSessionEntity)

    @Delete
    suspend fun deleteSession(session: RecordSessionEntity)
}
