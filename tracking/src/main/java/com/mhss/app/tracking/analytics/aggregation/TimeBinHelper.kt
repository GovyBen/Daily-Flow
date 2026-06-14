/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: replaced ThreeTen temporal arithmetic with
 * Kotlin datetime calendar arithmetic so day, week and month bins remain
 * correct across device time zones and daylight-saving changes.
 */

package com.mhss.app.tracking.analytics.aggregation

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class TimeBin(
    val startInclusive: Instant,
    val endExclusive: Instant
)

class TimeBinHelper(
    private val preferences: AggregationPreferences
) {
    fun binContaining(
        instant: Instant,
        binSize: FixedBinSize
    ): TimeBin {
        val localDateTime = instant.toLocalDateTime(preferences.timeZone)
        val effectiveDate = if (localDateTime.time < preferences.startTimeOfDay) {
            localDateTime.date.minus(1, DateTimeUnit.DAY)
        } else {
            localDateTime.date
        }
        val startDate = when (binSize) {
            FixedBinSize.DAY -> effectiveDate
            FixedBinSize.WEEK -> effectiveDate.minus(
                effectiveDate.dayOfWeek.dayNumberFrom(preferences.firstDayOfWeek),
                DateTimeUnit.DAY
            )
            FixedBinSize.MONTH -> LocalDate(
                effectiveDate.year,
                effectiveDate.month,
                1
            )
        }
        return binStarting(startDate, binSize)
    }

    fun previous(bin: TimeBin, binSize: FixedBinSize): TimeBin {
        val currentStartDate = bin.startInclusive
            .toLocalDateTime(preferences.timeZone)
            .date
        val previousStartDate = when (binSize) {
            FixedBinSize.DAY -> currentStartDate.minus(1, DateTimeUnit.DAY)
            FixedBinSize.WEEK -> currentStartDate.minus(7, DateTimeUnit.DAY)
            FixedBinSize.MONTH -> currentStartDate.minus(1, DateTimeUnit.MONTH)
        }
        return binStarting(previousStartDate, binSize)
    }

    private fun binStarting(
        startDate: LocalDate,
        binSize: FixedBinSize
    ): TimeBin {
        val endDate = when (binSize) {
            FixedBinSize.DAY -> startDate.plus(1, DateTimeUnit.DAY)
            FixedBinSize.WEEK -> startDate.plus(7, DateTimeUnit.DAY)
            FixedBinSize.MONTH -> startDate.plus(1, DateTimeUnit.MONTH)
        }
        return TimeBin(
            startInclusive = startDate
                .atTime(preferences.startTimeOfDay)
                .toInstant(preferences.timeZone),
            endExclusive = endDate
                .atTime(preferences.startTimeOfDay)
                .toInstant(preferences.timeZone)
        )
    }
}

private fun kotlinx.datetime.DayOfWeek.dayNumberFrom(
    firstDayOfWeek: kotlinx.datetime.DayOfWeek
): Int = (ordinal - firstDayOfWeek.ordinal + 7) % 7
