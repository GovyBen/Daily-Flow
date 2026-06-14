package com.mhss.app.tracking.analytics.repository

import com.mhss.app.tracking.analytics.model.TrackingAnalyticsSource

interface TrackingAnalyticsRepository {
    suspend fun getTrackerData(
        trackerId: String,
        startInclusive: Long,
        endExclusive: Long
    ): TrackingAnalyticsSource?
}
