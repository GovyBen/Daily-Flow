package com.mhss.app.tracking.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDataPointDao {

    @Query(
        """
        SELECT * FROM tracking_data_points
        WHERE session_id = :sessionId
        ORDER BY epoch_milli, id
        """
    )
    suspend fun getDataPointsForSession(sessionId: String): List<DataPointEntity>

    @Query(
        """
        SELECT * FROM tracking_data_points
        WHERE tracker_id = :trackerId
          AND epoch_milli >= :startInclusive
          AND epoch_milli < :endExclusive
        ORDER BY epoch_milli, id
        """
    )
    suspend fun getDataPointsInRange(
        trackerId: String,
        startInclusive: Long,
        endExclusive: Long
    ): List<DataPointEntity>

    @Query(
        """
        SELECT point.* FROM tracking_data_points AS point
        INNER JOIN tracking_record_sessions AS session
          ON session.id = point.session_id
        WHERE session.template_id = :templateId
          AND session.occurred_at_epoch_milli >= :startInclusive
          AND session.occurred_at_epoch_milli < :endExclusive
        ORDER BY point.epoch_milli, point.id
        """
    )
    fun observeDataPointsForTemplateInRange(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<DataPointEntity>>

    @Query(
        """
        SELECT * FROM tracking_data_points
        WHERE tracker_id = :trackerId
        ORDER BY epoch_milli DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentDataPoints(
        trackerId: String,
        limit: Int
    ): List<DataPointEntity>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM tracking_data_points
            WHERE tracker_id = :trackerId
            LIMIT 1
        )
        """
    )
    suspend fun hasDataPoints(trackerId: String): Boolean

    @Insert
    suspend fun insertDataPoints(points: List<DataPointEntity>)

    @Query("DELETE FROM tracking_data_points WHERE session_id = :sessionId")
    suspend fun deleteDataPointsForSession(sessionId: String): Int
}
