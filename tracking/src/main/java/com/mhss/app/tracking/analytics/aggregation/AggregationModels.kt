/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: replaced UI preferences and ThreeTen types
 * with explicit Kotlin datetime aggregation settings.
 */

package com.mhss.app.tracking.analytics.aggregation

import com.mhss.app.tracking.analytics.sampling.IDataPoint
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Instant

enum class AggregationOperation {
    SUM,
    COUNT,
    AVERAGE,
    MIN,
    MAX
}

enum class FixedBinSize(
    val regularity: DateTimePeriod
) {
    DAY(DateTimePeriod(days = 1)),
    WEEK(DateTimePeriod(days = 7)),
    MONTH(DateTimePeriod(months = 1))
}

data class AggregationPreferences(
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTimeOfDay: LocalTime = LocalTime(0, 0),
    val timeZone: TimeZone = TimeZone.currentSystemDefault()
)

object AggregationFactory {
    fun fixed(
        binSize: FixedBinSize,
        operation: AggregationOperation,
        preferences: AggregationPreferences = AggregationPreferences()
    ): DataSampleFunction = FixedBinAggregator(
        timeBinHelper = TimeBinHelper(preferences),
        binSize = binSize,
        operation = operation
    )

    fun moving(
        window: Duration,
        operation: AggregationOperation
    ): DataSampleFunction = MovingAggregator(window, operation)

    fun filterValue(
        fromValue: Double,
        toValue: Double
    ): DataSampleFunction = FilterValueFunction(fromValue, toValue)

    fun filterLabels(labels: Set<String>): DataSampleFunction =
        FilterLabelFunction(labels)

    fun composite(
        vararg functions: DataSampleFunction
    ): DataSampleFunction = CompositeDataSampleFunction(*functions)
}

internal fun aggregateValue(
    points: List<IDataPoint>,
    operation: AggregationOperation
): Double = when (operation) {
    AggregationOperation.SUM -> points.sumOf(IDataPoint::value)
    AggregationOperation.COUNT -> points.size.toDouble()
    AggregationOperation.AVERAGE -> points.map(IDataPoint::value).average()
    AggregationOperation.MIN -> points.minOfOrNull(IDataPoint::value) ?: Double.NaN
    AggregationOperation.MAX -> points.maxOfOrNull(IDataPoint::value) ?: Double.NaN
}

internal fun aggregateLabel(points: List<IDataPoint>): String = when {
    points.isEmpty() -> ""
    points.all { it.label == points.first().label } -> points.first().label
    else -> ""
}

internal data class AggregatedDataPoint(
    override val timestamp: Instant,
    override val value: Double,
    override val label: String
) : IDataPoint()
