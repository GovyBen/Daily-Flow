/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: consolidated the upstream value, label and
 * composite sample functions in the tracking analytics package.
 */

package com.mhss.app.tracking.analytics.aggregation

import com.mhss.app.tracking.analytics.sampling.DataSample
import com.mhss.app.tracking.analytics.sampling.IDataPoint

class FilterValueFunction(
    private val fromValue: Double,
    private val toValue: Double
) : DataSampleFunction {

    init {
        require(fromValue <= toValue) {
            "Value filter lower bound must not exceed upper bound"
        }
    }

    override suspend fun mapSample(dataSample: DataSample): DataSample =
        dataSample.filtered { it.value in fromValue..toValue }
}

class FilterLabelFunction(
    private val labels: Set<String>
) : DataSampleFunction {
    override suspend fun mapSample(dataSample: DataSample): DataSample =
        dataSample.filtered { it.label in labels }
}

class CompositeDataSampleFunction(
    private vararg val functions: DataSampleFunction
) : DataSampleFunction {
    override suspend fun mapSample(dataSample: DataSample): DataSample =
        functions.fold(dataSample) { sample, function ->
            function.mapSample(sample)
        }
}

private fun DataSample.filtered(
    predicate: (IDataPoint) -> Boolean
): DataSample = DataSample.fromSequence(
    data = filter(predicate),
    properties = properties,
    getRawDataPoints = ::getRawDataPoints,
    onDispose = ::dispose
)
