/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: adapted the package and timestamp type to
 * kotlin.time.Instant.
 */

package com.mhss.app.tracking.analytics.sampling

import kotlin.time.Instant

abstract class IDataPoint {
    abstract val timestamp: Instant
    abstract val value: Double
    abstract val label: String

    override fun equals(other: Any?): Boolean {
        return other is IDataPoint &&
            timestamp == other.timestamp &&
            value == other.value &&
            label == other.label
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + label.hashCode()
        return result
    }
}
