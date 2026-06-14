package com.mhss.app.tracking.analytics.sampling

import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import org.koin.core.annotation.Factory
import kotlin.time.Instant

enum class BooleanSampleMode {
    TRUE_COUNT,
    TRUE_RATIO
}

data class AdaptedTrackingDataSamples(
    val sample: DataSample,
    val samplesByLabel: Map<String, DataSample> = emptyMap()
)

/**
 * Converts persistence-independent tracking history into analytics samples.
 */
@Factory
class TrackingDataSampleAdapter {

    fun adapt(
        trackerId: String,
        trackerType: TrackerType,
        points: List<TrackingRecordedPoint>,
        booleanMode: BooleanSampleMode = BooleanSampleMode.TRUE_RATIO
    ): AdaptedTrackingDataSamples {
        require(points.all { it.trackerId == trackerId }) {
            "Every point must belong to tracker $trackerId"
        }

        val sortedPoints = points.sortedWith(
            compareByDescending<TrackingRecordedPoint> { it.epochMilli }
                .thenByDescending(TrackingRecordedPoint::id)
        )
        val adaptedPoints = when (trackerType) {
            TrackerType.MULTI_SELECT,
            TrackerType.SINGLE_SELECT -> sortedPoints.mapNotNull(::adaptSelection)

            TrackerType.COUNTER,
            TrackerType.SCALE,
            TrackerType.DURATION,
            TrackerType.NUMBER -> sortedPoints.mapNotNull(::adaptNumeric)

            TrackerType.BOOLEAN -> sortedPoints.mapNotNull { point ->
                adaptBoolean(point, booleanMode)
            }

            TrackerType.TEXT -> emptyList()
        }
        val properties = DataSampleProperties(
            isDuration = trackerType == TrackerType.DURATION
        )
        val samplesByLabel = if (
            trackerType == TrackerType.MULTI_SELECT ||
            trackerType == TrackerType.SINGLE_SELECT
        ) {
            adaptedPoints
                .groupBy(AdaptedPoint::label)
                .toSortedMap()
                .mapValues { (_, labelPoints) ->
                    labelPoints.toDataSample(properties)
                }
        } else {
            emptyMap()
        }

        return AdaptedTrackingDataSamples(
            sample = adaptedPoints.toDataSample(properties),
            samplesByLabel = samplesByLabel
        )
    }

    private fun adaptSelection(point: TrackingRecordedPoint): AdaptedPoint? {
        val value = point.value?.takeIf(Double::isFinite) ?: return null
        val label = point.label?.takeIf(String::isNotBlank) ?: return null
        return point.adapted(value = value, label = label)
    }

    private fun adaptNumeric(point: TrackingRecordedPoint): AdaptedPoint? {
        val value = point.value?.takeIf(Double::isFinite) ?: return null
        return point.adapted(value = value)
    }

    private fun adaptBoolean(
        point: TrackingRecordedPoint,
        mode: BooleanSampleMode
    ): AdaptedPoint? {
        val value = point.value
        if (value != 0.0 && value != 1.0) return null
        if (mode == BooleanSampleMode.TRUE_COUNT && value == 0.0) return null
        return point.adapted(value = value)
    }
}

private data class AdaptedPoint(
    val timestamp: Instant,
    val trackerId: String,
    val value: Double,
    val label: String,
    val note: String
)

private fun TrackingRecordedPoint.adapted(
    value: Double,
    label: String = this.label.orEmpty()
) = AdaptedPoint(
    timestamp = Instant.fromEpochMilliseconds(epochMilli),
    trackerId = trackerId,
    value = value,
    label = label,
    note = note.orEmpty()
)

private fun List<AdaptedPoint>.toDataSample(
    properties: DataSampleProperties
): DataSample {
    val rawPoints = map { point ->
        RawDataPoint(
            timestamp = point.timestamp,
            trackerId = point.trackerId,
            value = point.value,
            label = point.label,
            note = point.note
        )
    }
    return DataSample.fromSequence(
        data = asSequence().map { point ->
            object : IDataPoint() {
                override val timestamp = point.timestamp
                override val value = point.value
                override val label = point.label
            }
        },
        properties = properties,
        getRawDataPoints = { rawPoints },
        onDispose = {}
    )
}
