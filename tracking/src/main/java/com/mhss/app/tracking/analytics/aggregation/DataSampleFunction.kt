/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: adapted the upstream data sample function
 * contract to the tracking module.
 */

package com.mhss.app.tracking.analytics.aggregation

import com.mhss.app.tracking.analytics.sampling.DataSample

fun interface DataSampleFunction {
    suspend fun mapSample(dataSample: DataSample): DataSample
}
