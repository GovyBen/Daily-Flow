package com.mhss.app.tracking.analytics.sampling

import com.mhss.app.tracking.analytics.aggregation.AggregationFactory
import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.aggregation.AggregationPreferences
import com.mhss.app.tracking.analytics.aggregation.FixedBinSize
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingDataSampleAdapterTest {

    private val adapter = TrackingDataSampleAdapter()

    @Test
    fun numericPointsPreserveMeaningAndAreSortedNewestFirst() {
        val result = adapter.adapt(
            trackerId = "number",
            trackerType = TrackerType.NUMBER,
            points = listOf(
                point(id = "old", trackerId = "number", epochMilli = 1_000, value = 2.5),
                point(id = "new", trackerId = "number", epochMilli = 3_000, value = 7.5),
                point(id = "missing", trackerId = "number", epochMilli = 2_000, value = null)
            )
        )

        assertEquals(listOf(7.5, 2.5), result.sample.map(IDataPoint::value).toList())
        assertEquals(
            listOf(3_000L, 1_000L),
            result.sample.map { it.timestamp.toEpochMilliseconds() }.toList()
        )
        assertEquals(listOf(7.5, 2.5), result.sample.getRawDataPoints().map { it.value })
        assertTrue(result.samplesByLabel.isEmpty())
    }

    @Test
    fun multiSelectUsesStoredLabelsAndNumericSnapshotsForGroups() {
        val result = adapter.adapt(
            trackerId = "muscle",
            trackerType = TrackerType.MULTI_SELECT,
            points = listOf(
                point(
                    id = "back-old",
                    trackerId = "muscle",
                    epochMilli = 1_000,
                    value = 1.0,
                    label = "Back"
                ),
                point(
                    id = "chest",
                    trackerId = "muscle",
                    epochMilli = 2_000,
                    value = 4.0,
                    label = "Chest"
                ),
                point(
                    id = "back-new",
                    trackerId = "muscle",
                    epochMilli = 3_000,
                    value = 2.0,
                    label = "Back"
                )
            )
        )

        assertEquals(listOf("Back", "Chest"), result.samplesByLabel.keys.toList())
        assertEquals(
            listOf(2.0, 1.0),
            result.samplesByLabel.getValue("Back").map(IDataPoint::value).toList()
        )
        assertEquals(
            listOf(4.0),
            result.samplesByLabel.getValue("Chest").map(IDataPoint::value).toList()
        )
        assertEquals(
            listOf("Back", "Chest", "Back"),
            result.sample.map(IDataPoint::label).toList()
        )
    }

    @Test
    fun textPointsAreExcludedFromNumericSamples() {
        val result = adapter.adapt(
            trackerId = "notes",
            trackerType = TrackerType.TEXT,
            points = listOf(
                point(
                    id = "text",
                    trackerId = "notes",
                    epochMilli = 1_000,
                    value = null,
                    note = "private text"
                )
            )
        )

        assertTrue(result.sample.none())
        assertTrue(result.sample.getRawDataPoints().isEmpty())
        assertTrue(result.samplesByLabel.isEmpty())
    }

    @Test
    fun booleanModeSupportsTrueCountAndTrueRatio() = runBlocking {
        val points = listOf(
            point("true-1", "done", 3_000, 1.0),
            point("false", "done", 2_000, 0.0),
            point("true-2", "done", 1_000, 1.0)
        )
        val preferences = AggregationPreferences(timeZone = TimeZone.UTC)

        val count = AggregationFactory.fixed(
            binSize = FixedBinSize.DAY,
            operation = AggregationOperation.COUNT,
            preferences = preferences
        ).mapSample(
            adapter.adapt(
                trackerId = "done",
                trackerType = TrackerType.BOOLEAN,
                points = points,
                booleanMode = BooleanSampleMode.TRUE_COUNT
            ).sample
        ).single()

        val ratio = AggregationFactory.fixed(
            binSize = FixedBinSize.DAY,
            operation = AggregationOperation.AVERAGE,
            preferences = preferences
        ).mapSample(
            adapter.adapt(
                trackerId = "done",
                trackerType = TrackerType.BOOLEAN,
                points = points,
                booleanMode = BooleanSampleMode.TRUE_RATIO
            ).sample
        ).single()

        assertEquals(2.0, count.value, 0.0)
        assertEquals(2.0 / 3.0, ratio.value, 0.0)
    }

    @Test
    fun durationSamplesExposeDurationProperty() {
        val result = adapter.adapt(
            trackerId = "duration",
            trackerType = TrackerType.DURATION,
            points = listOf(point("duration", "duration", 1_000, 90.0))
        )

        assertTrue(result.sample.properties.isDuration)
    }

    @Test
    fun mixedTrackerInputIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            adapter.adapt(
                trackerId = "expected",
                trackerType = TrackerType.NUMBER,
                points = listOf(point("wrong", "other", 1_000, 1.0))
            )
        }
    }

    private fun point(
        id: String,
        trackerId: String,
        epochMilli: Long,
        value: Double?,
        label: String? = null,
        note: String? = null
    ) = TrackingRecordedPoint(
        id = id,
        trackerId = trackerId,
        epochMilli = epochMilli,
        utcOffsetSeconds = 0,
        value = value,
        label = label,
        note = note,
        optionId = null
    )
}
