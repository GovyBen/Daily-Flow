/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: supports explicit sum/count/average/min/max
 * operations and Kotlin datetime day, week and month bins.
 */

package com.mhss.app.tracking.analytics.aggregation

import com.mhss.app.tracking.analytics.sampling.DataSample
import com.mhss.app.tracking.analytics.sampling.IDataPoint
import kotlin.time.Duration.Companion.milliseconds

class FixedBinAggregator(
    private val timeBinHelper: TimeBinHelper,
    private val binSize: FixedBinSize,
    private val operation: AggregationOperation
) : DataSampleFunction {

    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return DataSample.fromSequence(
            data = aggregate(dataSample),
            properties = dataSample.properties.copy(regularity = binSize.regularity),
            getRawDataPoints = dataSample::getRawDataPoints,
            onDispose = dataSample::dispose
        )
    }

    private fun aggregate(dataSample: Sequence<IDataPoint>) = sequence {
        val iterator = dataSample.iterator()
        if (!iterator.hasNext()) return@sequence

        val first = iterator.next()
        var previousTimestamp = first.timestamp
        var currentBin = timeBinHelper.binContaining(first.timestamp, binSize)
        var currentPoints = mutableListOf(first)

        while (iterator.hasNext()) {
            val next = iterator.next()
            require(next.timestamp <= previousTimestamp) {
                "Fixed-bin aggregation requires points ordered newest to oldest"
            }
            previousTimestamp = next.timestamp
            while (next.timestamp < currentBin.startInclusive) {
                yield(currentBin.toDataPoint(currentPoints))
                currentBin = timeBinHelper.previous(currentBin, binSize)
                currentPoints = mutableListOf()
            }
            currentPoints += next
        }
        yield(currentBin.toDataPoint(currentPoints))
    }

    private fun TimeBin.toDataPoint(points: List<IDataPoint>) =
        AggregatedDataPoint(
            timestamp = endExclusive - 1.milliseconds,
            value = aggregateValue(points, operation),
            label = aggregateLabel(points)
        )
}
