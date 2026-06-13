package com.mhss.app.tracking.presentation.record

import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingQuickRecordStateTest {

    @Test
    fun buildsCommandWithTrimmedNoteAndOccurrenceOffset() {
        val state = state(TrackerInputValue.NumberValue(12.5)).copy(note = "  session note  ")

        val command = state.toRecordSessionCommand(ZoneId.of("Asia/Shanghai"))

        assertEquals("template", command.templateId)
        assertEquals("Asia/Shanghai", command.zoneId)
        assertEquals(8 * 60 * 60, command.utcOffsetSeconds)
        assertEquals("session note", command.note)
        assertEquals(12.5, (command.values.single().input as TrackerInputValue.NumberValue).value)
    }

    @Test
    fun validatesRequiredInputsBeforeSave() {
        assertFalse(state(TrackerInputValue.NumberValue(null)).hasValidInputs())
        assertTrue(state(TrackerInputValue.NumberValue(12.5)).hasValidInputs())
    }

    private fun state(input: TrackerInputValue): TrackingQuickRecordUiState {
        val field = TrackingFieldDraft(
            id = "field",
            trackerId = "tracker",
            tracker = TrackingTrackerDraft(
                name = "Value",
                config = NumberConfig()
            ),
            displayOrder = 0,
            required = true
        )
        return TrackingQuickRecordUiState(
            isLoading = false,
            occurredAtEpochMilli = 1_700_000_000_000,
            template = TrackingTemplateSummary(
                id = "template",
                name = "Template",
                description = "",
                icon = "",
                color = 0xFF000000,
                isPinned = false,
                displayOrder = 0,
                createdAtEpochMilli = 1,
                updatedAtEpochMilli = 1,
                lastRecordedAtEpochMilli = null,
                fields = listOf(field)
            ),
            fields = mapOf("field" to TrackingQuickRecordFieldState(input))
        )
    }
}
