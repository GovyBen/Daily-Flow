/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: adapted package names, timestamps and tracker IDs
 * while retaining the upstream lazy conversion and disposal behavior.
 */

package com.mhss.app.tracking.analytics.sampling

abstract class RawDataSample : Sequence<RawDataPoint> {

    companion object {
        fun fromSequence(
            data: Sequence<RawDataPoint>,
            getRawDataPoints: () -> List<RawDataPoint>,
            onDispose: () -> Unit
        ): RawDataSample {
            return object : RawDataSample() {
                override fun getRawDataPoints() = getRawDataPoints()
                override fun iterator(): Iterator<RawDataPoint> = data.iterator()
                override fun dispose() = onDispose()
            }
        }

        fun empty(): RawDataSample {
            return fromSequence(
                data = emptySequence(),
                getRawDataPoints = { emptyList() },
                onDispose = {}
            )
        }
    }

    abstract fun dispose()

    abstract fun getRawDataPoints(): List<RawDataPoint>

    fun asDataSample(
        properties: DataSampleProperties = DataSampleProperties()
    ): DataSample {
        return DataSample.fromSequence(
            data = map { rawPoint ->
                object : IDataPoint() {
                    override val timestamp = rawPoint.timestamp
                    override val value = rawPoint.value
                    override val label = rawPoint.label
                }
            },
            properties = properties,
            getRawDataPoints = ::getRawDataPoints,
            onDispose = ::dispose
        )
    }
}
