package com.mhss.app.tracking.analytics.model

import com.mhss.app.tracking.analytics.aggregation.FixedBinSize
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import kotlinx.datetime.LocalDate

data class TrackingAnalyticsSource(
    val trackerId: String,
    val trackerName: String,
    val trackerType: TrackerType,
    val unit: String?,
    val isActive: Boolean,
    val points: List<TrackingRecordedPoint>
)

data class TrackingDailySummary(
    val trackerId: String,
    val trackerName: String,
    val trackerType: TrackerType,
    val unit: String?,
    val isTrackerActive: Boolean,
    val date: LocalDate,
    val sampleCount: Int,
    val sum: Double,
    val average: Double?,
    val minimum: Double?,
    val maximum: Double?
)

data class TrackingSeries(
    val trackerId: String,
    val trackerName: String,
    val trackerType: TrackerType,
    val unit: String?,
    val isTrackerActive: Boolean,
    val binSize: FixedBinSize,
    val points: List<TrackingSeriesPoint>
)

data class TrackingSeriesPoint(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val startEpochMilli: Long,
    val endEpochMilliExclusive: Long,
    val value: Double?,
    val sampleCount: Int,
    val label: String
)

data class TrackingOptionDistribution(
    val trackerId: String,
    val trackerName: String,
    val isTrackerActive: Boolean,
    val totalSelections: Int,
    val items: List<TrackingOptionDistributionItem>
)

data class TrackingOptionDistributionItem(
    val label: String,
    val count: Int,
    val fraction: Double
)

data class TrackingStreak(
    val trackerId: String,
    val trackerName: String,
    val isTrackerActive: Boolean,
    val length: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)
