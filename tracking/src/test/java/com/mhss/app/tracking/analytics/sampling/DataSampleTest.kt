package com.mhss.app.tracking.analytics.sampling

import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSampleTest {

    @Test
    fun dataPointsUseValueEquality() {
        val first = samplePoint(value = 2.5, label = "Good")
        val equal = samplePoint(value = 2.5, label = "Good")
        val different = samplePoint(value = 3.0, label = "Good")

        assertEquals(first, equal)
        assertEquals(first.hashCode(), equal.hashCode())
        assertNotEquals(first, different)
    }

    @Test
    fun dataSampleRetainsPropertiesAndDisposesResources() {
        var disposed = false
        val properties = DataSampleProperties(
            regularity = DateTimePeriod(days = 1),
            isDuration = true
        )
        val sample = DataSample.fromSequence(
            data = sequenceOf(samplePoint()),
            properties = properties,
            onDispose = { disposed = true }
        )

        assertEquals(properties, sample.properties)
        assertEquals(1, sample.count())
        assertFalse(disposed)

        sample.dispose()

        assertTrue(disposed)
    }

    @Test
    fun getAllRawDataPointsExhaustsLazySequence() {
        val consumed = mutableListOf<RawDataPoint>()
        val points = listOf(
            rawPoint("first", 2L),
            rawPoint("second", 1L)
        )
        val sample = DataSample.fromSequence(
            data = points.asSequence().map { point ->
                consumed += point
                samplePoint(
                    timestamp = point.timestamp,
                    value = point.value,
                    label = point.label
                )
            },
            getRawDataPoints = { consumed.toList() },
            onDispose = {}
        )

        assertTrue(sample.getRawDataPoints().isEmpty())
        assertEquals(points, sample.getAllRawDataPoints())
    }

    @Test
    fun rawSampleConvertsWithoutLosingRawPointsOrDisposeCallback() {
        var disposed = false
        val rawPoints = listOf(rawPoint("entry", 1L))
        val rawSample = RawDataSample.fromSequence(
            data = rawPoints.asSequence(),
            getRawDataPoints = { rawPoints },
            onDispose = { disposed = true }
        )

        val sample = rawSample.asDataSample()

        assertEquals("entry", sample.single().label)
        assertEquals(rawPoints, sample.getRawDataPoints())
        sample.dispose()
        assertTrue(disposed)
    }

    @Test
    fun emptyRawSampleHasNoValues() {
        val sample = RawDataSample.empty()

        assertTrue(sample.none())
        assertTrue(sample.getRawDataPoints().isEmpty())
    }
}

private fun samplePoint(
    timestamp: Instant = Instant.parse("2026-01-15T08:00:00Z"),
    value: Double = 1.0,
    label: String = "Value"
) = object : IDataPoint() {
    override val timestamp = timestamp
    override val value = value
    override val label = label
}

private fun rawPoint(label: String, epochSeconds: Long) = RawDataPoint(
    timestamp = Instant.fromEpochSeconds(epochSeconds),
    trackerId = "tracker-1",
    value = epochSeconds.toDouble(),
    label = label
)
