package com.mhss.app.presentation

import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackingCalendarRecord
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarTrackingRecordsTest {

    @Test
    fun recordsAreGroupedUsingTheCurrentDeviceTimeZone() {
        val utc = TimeZone.UTC
        val record = TrackingCalendarRecord(
            id = "record",
            templateId = "template",
            templateName = "Health",
            templateColor = 1,
            occurredAtEpochMilli = LocalDateTime(2026, 6, 15, 1, 0)
                .toInstant(utc)
                .toEpochMilliseconds(),
            zoneId = "Asia/Shanghai",
            note = null,
            source = RecordSource.MANUAL
        )

        val grouped = listOf(record).groupByDeviceDate(
            TimeZone.of("America/Los_Angeles")
        )

        assertEquals(listOf(record), grouped[LocalDate(2026, 6, 14)])
    }
}
