package com.mhss.app.tracking.presentation.analytics

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingAnalyticsModelsTest {

    @Test
    fun dayRangeContainsExactly365CalendarDays() {
        val range = TrackingAnalyticsRange.DAY.dateRange(LocalDate(2026, 6, 14))

        assertEquals(LocalDate(2025, 6, 15), range.startDate)
        assertEquals(LocalDate(2026, 6, 15), range.endDateExclusive)
    }

    @Test
    fun monthRangeContainsCurrentAndPreviousElevenMonths() {
        val range = TrackingAnalyticsRange.MONTH.dateRange(LocalDate(2026, 6, 14))

        assertEquals(LocalDate(2025, 7, 1), range.startDate)
        assertEquals(LocalDate(2026, 7, 1), range.endDateExclusive)
    }
}
