package com.mhss.app.tracking.data.csv

import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.data.serialization.TrackerConfigJson
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.TrackerType
import java.io.StringReader
import java.io.StringWriter
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingCsvCodecTest {

    private val codec = TrackingCsvCodec()

    @Test
    fun exportedSnapshotImportsEquivalentlyWithEscapedUnicodeContent() {
        val expected = completeSnapshot()
        val output = StringWriter()

        codec.write(expected, output)
        val csv = output.toString()
        val actual = codec.preview(StringReader(csv), "round-trip.csv")

        assertEquals(expected, actual.snapshot)
        assertEquals(expected.counts(), actual.counts)
        assertTrue(csv.contains("\"健康, \"\"每日\"\""))
        assertTrue(csv.contains("第一行"))
        assertTrue(csv.contains("中文"))
    }

    @Test
    fun utf8BomIsAccepted() {
        val output = StringWriter()
        codec.write(completeSnapshot(), output)

        val preview = codec.preview(
            StringReader("\uFEFF${output}"),
            "bom.csv"
        )

        assertEquals(1, preview.counts.templates)
        assertEquals(1, preview.counts.dataPoints)
    }

    @Test
    fun invalidValueReportsLogicalCsvLine() {
        val output = StringWriter()
        codec.write(completeSnapshot(), output)
        val csv = output.toString().replace(",28800,7.5,", ",invalid,7.5,")

        val error = runCatching {
            codec.preview(StringReader(csv), "invalid.csv")
        }.exceptionOrNull() as TrackingCsvException

        assertEquals(TrackingCsvErrorReason.INVALID_VALUE, error.reason)
        assertEquals(7, error.lineNumber)
        assertEquals("utc_offset_seconds", error.detail)
    }

    @Test
    fun unsupportedSchemaVersionReportsLine() {
        val output = StringWriter()
        codec.write(completeSnapshot(), output)
        val csv = output.toString().replaceFirst("1,template", "2,template")

        val error = runCatching {
            codec.preview(StringReader(csv), "future.csv")
        }.exceptionOrNull() as TrackingCsvException

        assertEquals(TrackingCsvErrorReason.UNSUPPORTED_VERSION, error.reason)
        assertEquals(2, error.lineNumber)
    }

    @Test
    fun brokenReferenceReportsOwningRecordLine() {
        val snapshot = completeSnapshot().copy(
            fields = completeSnapshot().fields.map { it.copy(trackerId = "missing") }
        )
        val output = StringWriter()
        codec.write(snapshot, output)

        val error = runCatching {
            codec.preview(StringReader(output.toString()), "broken.csv")
        }.exceptionOrNull() as TrackingCsvException

        assertEquals(TrackingCsvErrorReason.BROKEN_REFERENCE, error.reason)
        assertEquals(5, error.lineNumber)
        assertEquals("field field-1 tracker", error.detail)
    }

    private fun completeSnapshot(): TrackingCsvSnapshot {
        val epochMilli = Instant.parse("2026-06-14T08:30:00Z").toEpochMilliseconds()
        val template = RecordTemplateEntity(
            id = "template-1",
            name = "健康, \"每日\"\n模板",
            description = "第一行\n第二行，中文",
            icon = "DF",
            color = 0xFF336699,
            isPinned = true,
            displayOrder = 2,
            createdAtEpochMilli = 100,
            updatedAtEpochMilli = 200
        )
        val tracker = TrackerEntity(
            id = "tracker-1",
            name = "心情, \"评分\"",
            type = TrackerType.SCALE.name,
            unit = "分",
            configJson = TrackerConfigJson.encode(
                ScaleConfig(minimum = 0.0, maximum = 10.0, step = 0.5)
            ),
            createdAtEpochMilli = 100,
            updatedAtEpochMilli = 200
        )
        val option = TrackerOptionEntity(
            id = "option-1",
            trackerId = tracker.id,
            label = "很好, \"继续\"",
            numericValue = 7.5,
            color = 0xFF00FF00,
            displayOrder = 1
        )
        val field = TemplateFieldEntity(
            id = "field-1",
            templateId = template.id,
            trackerId = tracker.id,
            displayOrder = 0,
            required = true,
            displayNameOverride = "今天，感觉",
            defaultValueJson = "{\"value\":\"中文,\\\"引号\\\"\"}"
        )
        val session = RecordSessionEntity(
            id = "session-1",
            templateId = template.id,
            occurredAtEpochMilli = epochMilli,
            zoneId = "Asia/Shanghai",
            note = "第一行\n第二行, \"备注\"",
            source = RecordSource.MANUAL.name,
            createdAtEpochMilli = epochMilli,
            updatedAtEpochMilli = epochMilli + 1
        )
        val dataPoint = DataPointEntity(
            id = "point-1",
            sessionId = session.id,
            trackerId = tracker.id,
            epochMilli = epochMilli,
            utcOffsetSeconds = 28_800,
            value = 7.5,
            label = "中文, \"标签\"",
            note = "点备注\n下一行",
            optionId = option.id,
            createdAtEpochMilli = epochMilli,
            updatedAtEpochMilli = epochMilli + 1
        )
        return TrackingCsvSnapshot(
            templates = listOf(template),
            trackers = listOf(tracker),
            options = listOf(option),
            fields = listOf(field),
            sessions = listOf(session),
            dataPoints = listOf(dataPoint)
        )
    }
}
