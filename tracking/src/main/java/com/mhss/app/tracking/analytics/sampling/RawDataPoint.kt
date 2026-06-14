/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: replaced the numeric feature ID and ThreeTen
 * timestamp with Daily Flow tracker IDs and kotlin.time.Instant.
 */

package com.mhss.app.tracking.analytics.sampling

import kotlin.time.Instant

data class RawDataPoint(
    val timestamp: Instant,
    val trackerId: String,
    val value: Double,
    val label: String = "",
    val note: String = ""
)
