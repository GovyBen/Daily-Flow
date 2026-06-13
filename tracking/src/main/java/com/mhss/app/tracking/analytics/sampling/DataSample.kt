/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: adapted package names and raw point types while
 * retaining lazy sequence and explicit resource-disposal semantics.
 */

package com.mhss.app.tracking.analytics.sampling

/**
 * A sequence of data points ordered from newest to oldest.
 *
 * Call [dispose] after iteration to release resources held by an implementation.
 */
abstract class DataSample(
    val properties: DataSampleProperties = DataSampleProperties()
) : Sequence<IDataPoint> {

    companion object {
        fun fromSequence(
            data: Sequence<IDataPoint>,
            properties: DataSampleProperties = DataSampleProperties(),
            onDispose: () -> Unit
        ): DataSample {
            return fromSequence(
                data = data,
                properties = properties,
                getRawDataPoints = { emptyList() },
                onDispose = onDispose
            )
        }

        fun fromSequence(
            data: Sequence<IDataPoint>,
            properties: DataSampleProperties = DataSampleProperties(),
            getRawDataPoints: () -> List<RawDataPoint>,
            onDispose: () -> Unit
        ): DataSample {
            return object : DataSample(properties) {
                override fun getRawDataPoints() = getRawDataPoints()
                override fun iterator(): Iterator<IDataPoint> = data.iterator()
                override fun dispose() = onDispose()
            }
        }
    }

    abstract fun dispose()

    /**
     * Returns the raw points consumed so far by this lazy sample.
     */
    abstract fun getRawDataPoints(): List<RawDataPoint>

    /**
     * Exhausts the sample before returning every consumed raw point.
     */
    open fun getAllRawDataPoints(): List<RawDataPoint> {
        iterator().let { iterator ->
            while (iterator.hasNext()) {
                iterator.next()
            }
        }
        return getRawDataPoints()
    }
}
