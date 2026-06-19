package com.mhss.app.tracking.presentation.dashboard

import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingDashboardModelsTest {

    @Test
    fun quickTemplatesPreferPinnedThenMostRecentAndSummaryCountsSessions() {
        val templates = listOf(
            template("old", pinned = false, lastRecordedAt = 100),
            template("pinned", pinned = true, lastRecordedAt = 50),
            template("recent", pinned = false, lastRecordedAt = 300),
            template("middle", pinned = false, lastRecordedAt = 200),
            template("fifth", pinned = false, lastRecordedAt = null)
        )
        val history = mapOf(
            "recent" to listOf(record("recent", 300), record("recent", 250)),
            "pinned" to listOf(record("pinned", 50))
        )

        val state = buildTrackingDashboardState(templates, history, emptyMap())

        assertEquals(
            listOf("pinned", "recent", "middle", "old"),
            state.quickTemplates.map { it.id }
        )
        assertEquals(3, state.totalSessionCount)
        assertEquals(listOf("recent", "pinned"), state.todaySummaries.map { it.templateId })
    }

    private fun template(
        id: String,
        pinned: Boolean,
        lastRecordedAt: Long?
    ) = TrackingTemplateSummary(
        id = id,
        name = id,
        description = "",
        icon = "",
        color = 0,
        isPinned = pinned,
        displayOrder = 0,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1,
        lastRecordedAtEpochMilli = lastRecordedAt,
        fields = emptyList()
    )

    private fun record(templateId: String, occurredAt: Long) = TrackingRecordHistory(
        id = "$templateId-$occurredAt",
        templateId = templateId,
        occurredAtEpochMilli = occurredAt,
        zoneId = "UTC",
        note = null,
        source = RecordSource.MANUAL,
        points = emptyList()
    )
}
