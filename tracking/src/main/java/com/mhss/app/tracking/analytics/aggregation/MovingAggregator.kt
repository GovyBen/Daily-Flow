/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: generalized the moving average implementation
 * to all supported aggregation operations and kotlin.time.Duration.
 */

package com.mhss.app.tracking.analytics.aggregation

import com.mhss.app.tracking.analytics.sampling.DataSample
import com.mhss.app.tracking.analytics.sampling.IDataPoint
import kotlin.time.Duration
import kotlin.time.Instant

class MovingAggregator(
    private val window: Duration,
    private val operation: AggregationOperation
) : DataSampleFunction {

    init {
        require(window > Duration.ZERO) { "Moving aggregation window must be positive" }
    }

    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return DataSample.fromSequence(
            data = aggregate(dataSample),
            properties = dataSample.properties,
            getRawDataPoints = dataSample::getRawDataPoints,
            onDispose = dataSample::dispose
        )
    }

    private fun aggregate(dataSample: Sequence<IDataPoint>) = sequence {
        val currentWindow = mutableListOf<IDataPoint>()
        var previousTimestamp: Instant? = null

        dataSample.forEach { next ->
            previousTimestamp?.let { previous ->
                require(next.timestamp <= previous) {
                    "Moving aggregation requires points ordered newest to oldest"
                }
            }
            previousTimestamp = next.timestamp

            while (
                currentWindow.isNotEmpty() &&
                currentWindow.first().timestamp - next.timestamp >= window
            ) {
                yieldCurrent(currentWindow)
                currentWindow.removeAt(0)
            }
            currentWindow += next
        }

        while (currentWindow.isNotEmpty()) {
            yieldCurrent(currentWindow)
            currentWindow.removeAt(0)
        }
    }

    private suspend fun SequenceScope<IDataPoint>.yieldCurrent(
        points: List<IDataPoint>
    ) {
        yield(
            AggregatedDataPoint(
                timestamp = points.first().timestamp,
                value = aggregateValue(points, operation),
                label = aggregateLabel(points)
            )
        )
    }
}
