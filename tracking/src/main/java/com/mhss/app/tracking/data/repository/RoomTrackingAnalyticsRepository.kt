package com.mhss.app.tracking.data.repository

import com.mhss.app.tracking.analytics.model.TrackingAnalyticsSource
import com.mhss.app.tracking.analytics.repository.TrackingAnalyticsRepository
import com.mhss.app.tracking.data.database.dao.TrackingDataPointDao
import com.mhss.app.tracking.data.database.dao.TrackingTrackerDao
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class RoomTrackingAnalyticsRepository(
    private val trackerDao: TrackingTrackerDao,
    private val dataPointDao: TrackingDataPointDao,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : TrackingAnalyticsRepository {

    override suspend fun getTrackerData(
        trackerId: String,
        startInclusive: Long,
        endExclusive: Long
    ): TrackingAnalyticsSource? = withContext(ioDispatcher) {
        require(startInclusive < endExclusive) {
            "Analytics range must have positive duration"
        }
        val tracker = trackerDao.getTracker(trackerId) ?: return@withContext null
        TrackingAnalyticsSource(
            trackerId = tracker.id,
            trackerName = tracker.name,
            trackerType = TrackerType.valueOf(tracker.type),
            unit = tracker.unit,
            isActive = tracker.isActive,
            points = dataPointDao.getDataPointsInRange(
                trackerId = trackerId,
                startInclusive = startInclusive,
                endExclusive = endExclusive
            ).map { it.toRecordedPoint() }
        )
    }
}
