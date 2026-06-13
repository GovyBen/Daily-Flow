package com.mhss.app.tracking.presentation.history

import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingHistoryModelsTest {

    @Test
    fun dayRangeUsesLocalMidnightsAcrossDaylightSavingChange() {
        val zoneId = ZoneId.of("America/New_York")
        val midday = LocalDate.of(2026, 3, 8)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val range = trackingDayRange(midday, zoneId)

        assertEquals(23 * 60 * 60 * 1_000L, range.endExclusive - range.startInclusive)
    }

    @Test
    fun mapsAllTrackerTypesIntoEditableInputs() {
        val template = template()
        val history = history()

        val editor = history.toEditorState(template)

        assertEquals(
            setOf("multi-a", "multi-b"),
            (editor.inputs["multi"] as TrackerInputValue.MultiSelect).optionIds
        )
        assertEquals(
            "single-a",
            (editor.inputs["single"] as TrackerInputValue.SingleSelect).optionId
        )
        assertEquals(3.0, (editor.inputs["counter"] as TrackerInputValue.Counter).value)
        assertEquals(7.0, (editor.inputs["scale"] as TrackerInputValue.Scale).value)
        assertEquals(true, (editor.inputs["boolean"] as TrackerInputValue.BooleanValue).value)
        assertEquals(3_661L, (editor.inputs["duration"] as TrackerInputValue.Duration).seconds)
        assertEquals(12.5, (editor.inputs["number"] as TrackerInputValue.NumberValue).value)
        assertEquals("hello", (editor.inputs["text"] as TrackerInputValue.Text).value)
        assertTrue(editor.hasArchivedFields)
    }

    @Test
    fun editorCommandPreservesIdentitySourceAndZoneOffset() {
        val template = template()
        val editor = history().toEditorState(template).copy(note = "  edited  ")

        val command = editor.toCommand(template, ZoneId.of("Asia/Shanghai"))

        assertEquals("session", command.id)
        assertEquals(RecordSource.IMPORT, command.source)
        assertEquals("edited", command.note)
        assertEquals(28_800, command.utcOffsetSeconds)
        assertTrue(editor.hasValidInputs(template))
    }

    @Test
    fun historyRowsIncludeEveryCurrentFieldAndArchivedPoints() {
        val rows = history().historyRows(template())

        assertEquals(9, rows.size)
        assertEquals("A, B", rows.first().value)
        assertEquals("1h 1m 1s", rows.first { it.name == "Duration" }.value)
        assertEquals("legacy", rows.single { it.isArchived }.value)
    }

    private fun template(): TrackingTemplateSummary {
        val multiOptions = listOf(option("multi-a", "A"), option("multi-b", "B"))
        val singleOptions = listOf(option("single-a", "Only"))
        val fields = listOf(
            field("multi", "Multi", MultiSelectConfig(), multiOptions),
            field("single", "Single", SingleSelectConfig, singleOptions),
            field("counter", "Counter", CounterConfig()),
            field("scale", "Scale", ScaleConfig()),
            field("boolean", "Boolean", BooleanConfig()),
            field("duration", "Duration", DurationConfig()),
            field("number", "Number", NumberConfig()),
            field("text", "Text", TextConfig())
        )
        return TrackingTemplateSummary(
            id = "template",
            name = "Template",
            description = "",
            icon = "",
            color = 0,
            isPinned = false,
            displayOrder = 0,
            createdAtEpochMilli = 1,
            updatedAtEpochMilli = 1,
            lastRecordedAtEpochMilli = 1_700_000_000_000,
            fields = fields
        )
    }

    private fun history() = TrackingRecordHistory(
        id = "session",
        templateId = "template",
        occurredAtEpochMilli = 1_700_000_000_000,
        zoneId = "Asia/Shanghai",
        note = "session",
        source = RecordSource.IMPORT,
        points = listOf(
            point("multi-tracker", label = "A", optionId = "multi-a"),
            point("multi-tracker", label = "B", optionId = "multi-b"),
            point("single-tracker", label = "Only", optionId = "single-a"),
            point("counter-tracker", value = 3.0),
            point("scale-tracker", value = 7.0),
            point("boolean-tracker", value = 1.0),
            point("duration-tracker", value = 3_661.0),
            point("number-tracker", value = 12.5),
            point("text-tracker", note = "hello"),
            point("archived-tracker", note = "legacy")
        )
    )

    private fun field(
        id: String,
        name: String,
        config: com.mhss.app.tracking.domain.model.TrackerConfig,
        options: List<TrackingOptionDraft> = emptyList()
    ) = TrackingFieldDraft(
        id = id,
        trackerId = "$id-tracker",
        tracker = TrackingTrackerDraft(
            id = "$id-tracker",
            name = name,
            config = config,
            options = options
        ),
        displayOrder = 0
    )

    private fun option(id: String, label: String) = TrackingOptionDraft(
        id = id,
        label = label,
        displayOrder = 0
    )

    private fun point(
        trackerId: String,
        value: Double? = null,
        label: String? = null,
        note: String? = null,
        optionId: String? = null
    ) = TrackingRecordedPoint(
        id = "$trackerId-${value ?: label ?: note}",
        trackerId = trackerId,
        epochMilli = 1_700_000_000_000,
        utcOffsetSeconds = 28_800,
        value = value,
        label = label,
        note = note,
        optionId = optionId
    )
}
