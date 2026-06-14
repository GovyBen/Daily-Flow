package com.mhss.app.tracking.analytics.aggregation

import com.mhss.app.tracking.analytics.sampling.DataSample
import com.mhss.app.tracking.analytics.sampling.DataSampleProperties
import com.mhss.app.tracking.analytics.sampling.IDataPoint
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class AggregatorTest {

    private val timeZone = TimeZone.UTC
    private val preferences = AggregationPreferences(timeZone = timeZone)

    @Test
    fun fixedBinsSupportEveryAggregationOperation() = runBlocking {
        val input = sample(
            point(2026, 6, 14, 12, value = 4.0, label = "same"),
            point(2026, 6, 14, 8, value = 2.0, label = "same")
        )
        val expected = mapOf(
            AggregationOperation.SUM to 6.0,
            AggregationOperation.COUNT to 2.0,
            AggregationOperation.AVERAGE to 3.0,
            AggregationOperation.MIN to 2.0,
            AggregationOperation.MAX to 4.0
        )

        expected.forEach { (operation, value) ->
            val result = AggregationFactory.fixed(
                FixedBinSize.DAY,
                operation,
                preferences
            ).mapSample(input).single()

            assertEquals(value, result.value, 0.0)
            assertEquals("same", result.label)
        }
    }

    @Test
    fun fixedBinsIncludeGapsAndExposeRegularity() = runBlocking {
        val input = sample(
            point(2026, 3, 9, 12, value = 2.0),
            point(2026, 3, 7, 12, value = 5.0)
        )

        val result = AggregationFactory.fixed(
            FixedBinSize.DAY,
            AggregationOperation.SUM,
            preferences
        ).mapSample(input)

        assertEquals(DateTimePeriod(days = 1), result.properties.regularity)
        assertEquals(listOf(2.0, 0.0, 5.0), result.map(IDataPoint::value).toList())
    }

    @Test
    fun fixedBinsUseNanForEmptyAverageAndClearMixedLabels() = runBlocking {
        val input = sample(
            point(2026, 6, 16, 12, value = 4.0, label = "first"),
            point(2026, 6, 16, 8, value = 2.0, label = "second"),
            point(2026, 6, 14, 8, value = 1.0, label = "first")
        )

        val result = AggregationFactory.fixed(
            FixedBinSize.DAY,
            AggregationOperation.AVERAGE,
            preferences
        ).mapSample(input).toList()

        assertEquals("", result.first().label)
        assertTrue(result[1].value.isNaN())
    }

    @Test
    fun fixedBinTimestampIsEndOfLocalPeriod() = runBlocking {
        val newYork = TimeZone.of("America/New_York")
        val input = sample(
            dataPoint(
                LocalDateTime(2026, 3, 8, 12, 0).toInstant(newYork),
                value = 1.0
            )
        )

        val result = AggregationFactory.fixed(
            FixedBinSize.DAY,
            AggregationOperation.SUM,
            AggregationPreferences(timeZone = newYork)
        ).mapSample(input).single()

        assertEquals(
            LocalDateTime(2026, 3, 9, 0, 0).toInstant(newYork).toEpochMilliseconds() - 1,
            result.timestamp.toEpochMilliseconds()
        )
    }

    @Test
    fun movingAverageExcludesPointsExactlyAtWindowBoundary() = runBlocking {
        val input = sample(
            point(2026, 6, 14, 10, value = 10.0),
            point(2026, 6, 14, 9, minute = 30, value = 20.0),
            point(2026, 6, 14, 9, value = 30.0)
        )

        val result = AggregationFactory.moving(
            window = 1.hours,
            operation = AggregationOperation.AVERAGE
        ).mapSample(input).toList()

        assertEquals(listOf(15.0, 25.0, 30.0), result.map(IDataPoint::value))
        assertEquals(input.map(IDataPoint::timestamp).toList(), result.map(IDataPoint::timestamp))
    }

    @Test
    fun movingAggregationConsumesOnlyTheRequiredUpstreamPoints() = runBlocking {
        var consumed = 0
        val input = DataSample.fromSequence(
            data = sequence {
                repeat(10) { index ->
                    consumed += 1
                    yield(
                        dataPoint(
                            timestamp = point(2026, 6, 14, 12).timestamp - index.hours,
                            value = index.toDouble()
                        )
                    )
                }
            },
            onDispose = {}
        )

        AggregationFactory.moving(
            window = 2.hours,
            operation = AggregationOperation.AVERAGE
        ).mapSample(input).take(3).toList()

        assertEquals(5, consumed)
    }

    @Test
    fun movingAverageMatchesUpstreamReferenceSeries() = runBlocking {
        val start = point(2026, 6, 14, 12).timestamp
        val input = listOf(
            3.0 to 10,
            7.0 to 20,
            8.0 to 30,
            4.0 to 41,
            0.0 to 43,
            2.0 to 48,
            4.0 to 49,
            0.0 to 50,
            5.0 to 70,
            3.0 to 75
        ).map { (value, hoursBefore) ->
            dataPoint(start - hoursBefore.hours, value)
        }

        val result = AggregationFactory.moving(
            window = 10.hours,
            operation = AggregationOperation.AVERAGE
        ).mapSample(sample(*input.toTypedArray()))

        assertEquals(
            listOf(3.0, 7.0, 8.0, 2.0, 1.5, 2.0, 2.0, 0.0, 4.0, 3.0),
            result.map(IDataPoint::value).toList()
        )
    }

    @Test
    fun valueAndLabelFiltersCanBeComposed() = runBlocking {
        val input = sample(
            point(2026, 6, 14, 12, value = 1.0, label = "A"),
            point(2026, 6, 14, 11, value = 2.0, label = "B"),
            point(2026, 6, 14, 10, value = 100.0, label = "A"),
            point(2026, 6, 14, 9, value = 3.0, label = "A")
        )

        val result = AggregationFactory.composite(
            AggregationFactory.filterValue(2.0, 100.0),
            AggregationFactory.filterLabels(setOf("A"))
        ).mapSample(input).toList()

        assertEquals(listOf(100.0, 3.0), result.map(IDataPoint::value))
    }

    @Test
    fun filterPreservesPropertiesAndDisposeCallback() = runBlocking {
        var disposed = false
        val input = DataSample.fromSequence(
            data = sequenceOf(point(2026, 6, 14, 12)),
            properties = DataSampleProperties(isDuration = true),
            onDispose = { disposed = true }
        )

        val result = AggregationFactory.filterLabels(setOf("missing")).mapSample(input)
        result.dispose()

        assertTrue(result.properties.isDuration)
        assertTrue(disposed)
    }

    @Test
    fun valueFilterRejectsReversedBounds() {
        assertThrows(IllegalArgumentException::class.java) {
            AggregationFactory.filterValue(2.0, 1.0)
        }
    }

    @Test
    fun aggregatorsPreserveDisposeCallback() = runBlocking {
        var disposed = false
        val input = DataSample.fromSequence(
            data = sequenceOf(point(2026, 6, 14, 12)),
            onDispose = { disposed = true }
        )

        val result = AggregationFactory.fixed(
            FixedBinSize.MONTH,
            AggregationOperation.COUNT,
            preferences
        ).mapSample(input)
        result.dispose()

        assertTrue(disposed)
    }

    @Test
    fun aggregatorsRejectAscendingInput() = runBlocking {
        val ascending = sample(
            point(2026, 6, 14, 8),
            point(2026, 6, 14, 9)
        )
        val result = AggregationFactory.fixed(
            FixedBinSize.DAY,
            AggregationOperation.SUM,
            preferences
        ).mapSample(ascending)

        assertThrows(IllegalArgumentException::class.java) {
            result.toList()
        }
        Unit
    }

    @Test
    fun emptySampleProducesNoBins() = runBlocking {
        val result = AggregationFactory.fixed(
            FixedBinSize.WEEK,
            AggregationOperation.COUNT,
            preferences
        ).mapSample(sample())

        assertTrue(result.none())
    }

    private fun sample(vararg points: IDataPoint): DataSample = DataSample.fromSequence(
        data = points.asSequence(),
        onDispose = {}
    )

    private fun point(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
        value: Double = 1.0,
        label: String = ""
    ) = dataPoint(
        timestamp = LocalDateTime(year, month, day, hour, minute).toInstant(timeZone),
        value = value,
        label = label
    )
}

private fun dataPoint(
    timestamp: Instant,
    value: Double,
    label: String = ""
) = object : IDataPoint() {
    override val timestamp = timestamp
    override val value = value
    override val label = label
}
