package com.mhss.app.presentation.model

import com.mhss.app.domain.model.CalendarDay
import kotlinx.datetime.YearMonth

data class CalendarMonth(
    val month: YearMonth,
    val days: List<CalendarDay>
)

