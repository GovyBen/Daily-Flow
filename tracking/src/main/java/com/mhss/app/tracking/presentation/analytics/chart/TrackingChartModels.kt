package com.mhss.app.tracking.presentation.analytics.chart

import androidx.compose.ui.graphics.Color

data class TrackingChartValue(
    val label: String,
    val value: Double,
    val color: Color? = null
)
