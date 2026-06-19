package com.mhss.app.presentation

data class CalendarTrackingValue(
    val templateId: String,
    val templateName: String,
    val templateColor: Long,
    val sessionCount: Int,
    val totalValue: Double? = null
)
