package com.mhss.app.tracking.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity

@Dao
interface TrackingTrackerDao {

    @Query("SELECT * FROM tracking_trackers WHERE id = :id")
    suspend fun getTracker(id: String): TrackerEntity?

    @Query(
        """
        SELECT * FROM tracking_tracker_options
        WHERE tracker_id = :trackerId AND is_active = 1
        ORDER BY display_order, id
        """
    )
    suspend fun getActiveOptions(trackerId: String): List<TrackerOptionEntity>

    @Insert
    suspend fun insertTracker(tracker: TrackerEntity)

    @Insert
    suspend fun insertOptions(options: List<TrackerOptionEntity>)

    @Update
    suspend fun updateTracker(tracker: TrackerEntity)

    @Query(
        """
        UPDATE tracking_trackers
        SET is_active = 0, updated_at_epoch_milli = :updatedAtEpochMilli
        WHERE id = :id
        """
    )
    suspend fun deactivateTracker(id: String, updatedAtEpochMilli: Long): Int

    @Query("UPDATE tracking_tracker_options SET is_active = 0 WHERE id = :id")
    suspend fun deactivateOption(id: String): Int
}
