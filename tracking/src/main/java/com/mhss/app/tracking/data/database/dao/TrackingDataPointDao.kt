package com.mhss.app.tracking.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mhss.app.tracking.data.database.entity.DataPointEntity

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

    @Insert
    suspend fun insertDataPoints(points: List<DataPointEntity>)

    @Query("DELETE FROM tracking_data_points WHERE session_id = :sessionId")
    suspend fun deleteDataPointsForSession(sessionId: String): Int
}
