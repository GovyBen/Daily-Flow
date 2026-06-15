/*
 * This file is part of Daily Flow.
 *
 * Daily Flow is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * CSV parsing and row-level validation were adapted from Track & Graph's
 * CSVReadWriterImpl.kt. Daily Flow uses a versioned multi-record snapshot.
 */

package com.mhss.app.tracking.data.csv

import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.data.serialization.TrackerConfigJson
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackerType
import java.io.BufferedReader
import java.io.Reader
import java.io.Writer
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.koin.core.annotation.Factory

enum class TrackingCsvErrorReason {
    INVALID_HEADERS,
    UNSUPPORTED_VERSION,
    UNKNOWN_RECORD_TYPE,
    MISSING_VALUE,
    INVALID_VALUE,
    DUPLICATE_ID,
    BROKEN_REFERENCE,
    READ_FAILED,
    WRITE_FAILED,
    IMPORT_FAILED
}

class TrackingCsvException(
    val reason: TrackingCsvErrorReason,
    val lineNumber: Int? = null,
    val detail: String? = null,
    cause: Throwable? = null
) : IllegalArgumentException(
    buildString {
        append(reason.name)
        lineNumber?.let { append(" at line ").append(it) }
        detail?.let { append(": ").append(it) }
    },
    cause
)

@Factory
class TrackingCsvCodec {

    fun write(snapshot: TrackingCsvSnapshot, writer: Writer) {
        try {
            CSVFormat.DEFAULT.builder()
                .setHeader(*HEADERS.toTypedArray())
                .get()
                .print(writer)
                .use { printer ->
                    snapshot.templates.forEach { printer.printRecord(templateRow(it)) }
                    snapshot.trackers.forEach { printer.printRecord(trackerRow(it)) }
                    snapshot.options.forEach { printer.printRecord(optionRow(it)) }
                    snapshot.fields.forEach { printer.printRecord(fieldRow(it)) }
                    snapshot.sessions.forEach { printer.printRecord(sessionRow(it)) }
                    snapshot.dataPoints.forEach { printer.printRecord(dataPointRow(it)) }
                }
        } catch (exception: TrackingCsvException) {
            throw exception
        } catch (exception: Exception) {
            throw TrackingCsvException(
                reason = TrackingCsvErrorReason.WRITE_FAILED,
                detail = exception.message,
                cause = exception
            )
        }
    }

    fun preview(reader: Reader, sourceName: String): TrackingCsvImportPreview {
        try {
            val parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader.withoutUtf8Bom())
            parser.use {
                validateHeaders(parser.headerNames)
                val templates = linkedMapOf<String, RecordTemplateEntity>()
                val trackers = linkedMapOf<String, TrackerEntity>()
                val options = linkedMapOf<String, TrackerOptionEntity>()
                val fields = linkedMapOf<String, TemplateFieldEntity>()
                val sessions = linkedMapOf<String, RecordSessionEntity>()
                val dataPoints = linkedMapOf<String, DataPointEntity>()
                val optionLines = mutableMapOf<String, Int>()
                val fieldLines = mutableMapOf<String, Int>()
                val sessionLines = mutableMapOf<String, Int>()
                val dataPointLines = mutableMapOf<String, Int>()

                parser.forEach { record ->
                    val line = record.lineNumber
                    val version = record.required(SCHEMA_VERSION, line)
                    if (version != CURRENT_SCHEMA_VERSION) {
                        throw TrackingCsvException(
                            reason = TrackingCsvErrorReason.UNSUPPORTED_VERSION,
                            lineNumber = line,
                            detail = version
                        )
                    }
                    when (record.required(RECORD_TYPE, line)) {
                        RecordType.TEMPLATE.csvValue ->
                            templates.putUnique(record.parseTemplate(line), line)
                        RecordType.TRACKER.csvValue ->
                            trackers.putUnique(record.parseTracker(line), line)
                        RecordType.OPTION.csvValue -> record.parseOption(line).also {
                            options.putUnique(it, line)
                            optionLines[it.id] = line
                        }
                        RecordType.FIELD.csvValue -> record.parseField(line).also {
                            fields.putUnique(it, line)
                            fieldLines[it.id] = line
                        }
                        RecordType.SESSION.csvValue -> record.parseSession(line).also {
                            sessions.putUnique(it, line)
                            sessionLines[it.id] = line
                        }
                        RecordType.DATA_POINT.csvValue -> record.parseDataPoint(line).also {
                            dataPoints.putUnique(it, line)
                            dataPointLines[it.id] = line
                        }
                        else -> throw TrackingCsvException(
                            reason = TrackingCsvErrorReason.UNKNOWN_RECORD_TYPE,
                            lineNumber = line,
                            detail = record.get(RECORD_TYPE)
                        )
                    }
                }

                validateReferences(
                    templates = templates,
                    trackers = trackers,
                    options = options,
                    fields = fields,
                    sessions = sessions,
                    dataPoints = dataPoints,
                    optionLines = optionLines,
                    fieldLines = fieldLines,
                    sessionLines = sessionLines,
                    dataPointLines = dataPointLines
                )

                return TrackingCsvImportPreview(
                    sourceName = sourceName,
                    snapshot = TrackingCsvSnapshot(
                        templates = templates.values.toList(),
                        trackers = trackers.values.toList(),
                        options = options.values.toList(),
                        fields = fields.values.toList(),
                        sessions = sessions.values.toList(),
                        dataPoints = dataPoints.values.toList()
                    )
                )
            }
        } catch (exception: TrackingCsvException) {
            throw exception
        } catch (exception: Exception) {
            throw TrackingCsvException(
                reason = TrackingCsvErrorReason.READ_FAILED,
                detail = exception.message,
                cause = exception
            )
        }
    }

    private fun validateHeaders(headerNames: List<String>) {
        val missing = HEADERS.filterNot(headerNames::contains)
        if (missing.isNotEmpty()) {
            throw TrackingCsvException(
                reason = TrackingCsvErrorReason.INVALID_HEADERS,
                lineNumber = 1,
                detail = missing.joinToString()
            )
        }
    }

    private fun CSVRecord.parseTemplate(line: Int) = RecordTemplateEntity(
        id = required(TEMPLATE_ID, line),
        name = required(TEMPLATE_NAME, line),
        description = optional(TEMPLATE_DESCRIPTION).orEmpty(),
        icon = optional(TEMPLATE_ICON).orEmpty(),
        color = requiredLong(TEMPLATE_COLOR, line),
        isActive = requiredBoolean(TEMPLATE_IS_ACTIVE, line),
        isPinned = requiredBoolean(TEMPLATE_IS_PINNED, line),
        displayOrder = requiredInt(TEMPLATE_DISPLAY_ORDER, line),
        createdAtEpochMilli = requiredLong(CREATED_AT, line),
        updatedAtEpochMilli = requiredLong(UPDATED_AT, line)
    )

    private fun CSVRecord.parseTracker(line: Int): TrackerEntity {
        val type = enumValue<TrackerType>(TRACKER_TYPE, line)
        val configJson = required(TRACKER_CONFIG_JSON, line)
        val config = try {
            TrackerConfigJson.decode(configJson)
        } catch (exception: Exception) {
            throw invalid(TRACKER_CONFIG_JSON, line, exception)
        }
        if (config.trackerType != type) {
            throw invalid(TRACKER_CONFIG_JSON, line)
        }
        return TrackerEntity(
            id = required(TRACKER_ID, line),
            name = required(TRACKER_NAME, line),
            type = type.name,
            unit = optional(TRACKER_UNIT),
            configJson = configJson,
            isActive = requiredBoolean(TRACKER_IS_ACTIVE, line),
            createdAtEpochMilli = requiredLong(CREATED_AT, line),
            updatedAtEpochMilli = requiredLong(UPDATED_AT, line)
        )
    }

    private fun CSVRecord.parseOption(line: Int) = TrackerOptionEntity(
        id = required(OPTION_ID, line),
        trackerId = required(TRACKER_ID, line),
        label = required(OPTION_LABEL, line),
        numericValue = optionalDouble(OPTION_NUMERIC_VALUE, line),
        color = optionalLong(OPTION_COLOR, line),
        displayOrder = requiredInt(OPTION_DISPLAY_ORDER, line),
        isActive = requiredBoolean(OPTION_IS_ACTIVE, line)
    )

    private fun CSVRecord.parseField(line: Int) = TemplateFieldEntity(
        id = required(FIELD_ID, line),
        templateId = required(TEMPLATE_ID, line),
        trackerId = required(TRACKER_ID, line),
        displayOrder = requiredInt(FIELD_DISPLAY_ORDER, line),
        required = requiredBoolean(FIELD_REQUIRED, line),
        displayNameOverride = optional(FIELD_DISPLAY_NAME_OVERRIDE),
        defaultValueJson = optional(FIELD_DEFAULT_VALUE_JSON)
    )

    private fun CSVRecord.parseSession(line: Int): RecordSessionEntity {
        val occurredAt = required(OCCURRED_AT, line)
        val epochMilli = try {
            Instant.parse(occurredAt).toEpochMilliseconds()
        } catch (exception: Exception) {
            throw invalid(OCCURRED_AT, line, exception)
        }
        val zoneId = required(ZONE_ID, line)
        try {
            TimeZone.of(zoneId)
        } catch (exception: Exception) {
            throw invalid(ZONE_ID, line, exception)
        }
        return RecordSessionEntity(
            id = required(SESSION_ID, line),
            templateId = required(TEMPLATE_ID, line),
            occurredAtEpochMilli = epochMilli,
            zoneId = zoneId,
            note = optional(SESSION_NOTE),
            source = enumValue<RecordSource>(RECORD_SOURCE, line).name,
            createdAtEpochMilli = requiredLong(CREATED_AT, line),
            updatedAtEpochMilli = requiredLong(UPDATED_AT, line)
        )
    }

    private fun CSVRecord.parseDataPoint(line: Int): DataPointEntity {
        val value = optionalDouble(VALUE, line)
        val utcOffset = requiredInt(UTC_OFFSET_SECONDS, line)
        if (utcOffset !in MIN_UTC_OFFSET_SECONDS..MAX_UTC_OFFSET_SECONDS) {
            throw invalid(UTC_OFFSET_SECONDS, line)
        }
        return DataPointEntity(
            id = required(POINT_ID, line),
            sessionId = optional(SESSION_ID),
            trackerId = required(TRACKER_ID, line),
            epochMilli = requiredLong(POINT_EPOCH_MILLI, line),
            utcOffsetSeconds = utcOffset,
            value = value,
            label = optional(VALUE_LABEL),
            note = optional(POINT_NOTE),
            optionId = optional(OPTION_ID),
            createdAtEpochMilli = requiredLong(CREATED_AT, line),
            updatedAtEpochMilli = requiredLong(UPDATED_AT, line)
        )
    }

    private fun validateReferences(
        templates: Map<String, RecordTemplateEntity>,
        trackers: Map<String, TrackerEntity>,
        options: Map<String, TrackerOptionEntity>,
        fields: Map<String, TemplateFieldEntity>,
        sessions: Map<String, RecordSessionEntity>,
        dataPoints: Map<String, DataPointEntity>,
        optionLines: Map<String, Int>,
        fieldLines: Map<String, Int>,
        sessionLines: Map<String, Int>,
        dataPointLines: Map<String, Int>
    ) {
        options.values.forEach { option ->
            requireReference(
                option.trackerId in trackers,
                optionLines[option.id],
                "option ${option.id} tracker"
            )
        }
        fields.values.forEach { field ->
            requireReference(
                field.templateId in templates,
                fieldLines[field.id],
                "field ${field.id} template"
            )
            requireReference(
                field.trackerId in trackers,
                fieldLines[field.id],
                "field ${field.id} tracker"
            )
        }
        sessions.values.forEach { session ->
            requireReference(
                session.templateId in templates,
                sessionLines[session.id],
                "session ${session.id} template"
            )
        }
        dataPoints.values.forEach { point ->
            requireReference(
                point.sessionId == null || point.sessionId in sessions,
                dataPointLines[point.id],
                "data point ${point.id} session"
            )
            requireReference(
                point.trackerId in trackers,
                dataPointLines[point.id],
                "data point ${point.id} tracker"
            )
            point.optionId?.let { optionId ->
                val option = options[optionId]
                requireReference(
                    option != null,
                    dataPointLines[point.id],
                    "data point ${point.id} option"
                )
                requireReference(
                    option?.trackerId == point.trackerId,
                    dataPointLines[point.id],
                    "data point ${point.id} option tracker"
                )
            }
        }
    }

    private fun requireReference(valid: Boolean, line: Int?, detail: String) {
        if (!valid) {
            throw TrackingCsvException(
                reason = TrackingCsvErrorReason.BROKEN_REFERENCE,
                lineNumber = line,
                detail = detail
            )
        }
    }

    private fun templateRow(value: RecordTemplateEntity) = row(
        RecordType.TEMPLATE,
        TEMPLATE_ID to value.id,
        TEMPLATE_NAME to value.name,
        TEMPLATE_DESCRIPTION to value.description,
        TEMPLATE_ICON to value.icon,
        TEMPLATE_COLOR to value.color,
        TEMPLATE_IS_ACTIVE to value.isActive,
        TEMPLATE_IS_PINNED to value.isPinned,
        TEMPLATE_DISPLAY_ORDER to value.displayOrder,
        CREATED_AT to value.createdAtEpochMilli,
        UPDATED_AT to value.updatedAtEpochMilli
    )

    private fun trackerRow(value: TrackerEntity) = row(
        RecordType.TRACKER,
        TRACKER_ID to value.id,
        TRACKER_NAME to value.name,
        TRACKER_TYPE to value.type,
        TRACKER_UNIT to value.unit,
        TRACKER_CONFIG_JSON to value.configJson,
        TRACKER_IS_ACTIVE to value.isActive,
        CREATED_AT to value.createdAtEpochMilli,
        UPDATED_AT to value.updatedAtEpochMilli
    )

    private fun optionRow(value: TrackerOptionEntity) = row(
        RecordType.OPTION,
        TRACKER_ID to value.trackerId,
        OPTION_ID to value.id,
        OPTION_LABEL to value.label,
        OPTION_NUMERIC_VALUE to value.numericValue,
        OPTION_COLOR to value.color,
        OPTION_DISPLAY_ORDER to value.displayOrder,
        OPTION_IS_ACTIVE to value.isActive
    )

    private fun fieldRow(value: TemplateFieldEntity) = row(
        RecordType.FIELD,
        TEMPLATE_ID to value.templateId,
        TRACKER_ID to value.trackerId,
        FIELD_ID to value.id,
        FIELD_DISPLAY_ORDER to value.displayOrder,
        FIELD_REQUIRED to value.required,
        FIELD_DISPLAY_NAME_OVERRIDE to value.displayNameOverride,
        FIELD_DEFAULT_VALUE_JSON to value.defaultValueJson
    )

    private fun sessionRow(value: RecordSessionEntity) = row(
        RecordType.SESSION,
        TEMPLATE_ID to value.templateId,
        SESSION_ID to value.id,
        OCCURRED_AT to Instant.fromEpochMilliseconds(value.occurredAtEpochMilli).toString(),
        ZONE_ID to value.zoneId,
        SESSION_NOTE to value.note,
        RECORD_SOURCE to value.source,
        CREATED_AT to value.createdAtEpochMilli,
        UPDATED_AT to value.updatedAtEpochMilli
    )

    private fun dataPointRow(value: DataPointEntity) = row(
        RecordType.DATA_POINT,
        TRACKER_ID to value.trackerId,
        OPTION_ID to value.optionId,
        SESSION_ID to value.sessionId,
        POINT_ID to value.id,
        POINT_EPOCH_MILLI to value.epochMilli,
        UTC_OFFSET_SECONDS to value.utcOffsetSeconds,
        VALUE to value.value,
        VALUE_LABEL to value.label,
        POINT_NOTE to value.note,
        CREATED_AT to value.createdAtEpochMilli,
        UPDATED_AT to value.updatedAtEpochMilli
    )

    private fun row(
        type: RecordType,
        vararg values: Pair<String, Any?>
    ): List<String> {
        val byHeader = values.toMap()
        return HEADERS.map { header ->
            when (header) {
                SCHEMA_VERSION -> CURRENT_SCHEMA_VERSION
                RECORD_TYPE -> type.csvValue
                else -> byHeader[header]?.toString().orEmpty()
            }
        }
    }

    private fun CSVRecord.required(header: String, line: Int): String =
        optional(header) ?: throw TrackingCsvException(
            reason = TrackingCsvErrorReason.MISSING_VALUE,
            lineNumber = line,
            detail = header
        )

    private fun CSVRecord.optional(header: String): String? =
        get(header).takeUnless(String::isBlank)

    private fun CSVRecord.requiredBoolean(header: String, line: Int): Boolean =
        when (val value = required(header, line)) {
            "true" -> true
            "false" -> false
            else -> throw invalid(header, line, detail = value)
        }

    private fun CSVRecord.requiredInt(header: String, line: Int): Int =
        required(header, line).toIntOrNull() ?: throw invalid(header, line)

    private fun CSVRecord.requiredLong(header: String, line: Int): Long =
        required(header, line).toLongOrNull() ?: throw invalid(header, line)

    private fun CSVRecord.optionalLong(header: String, line: Int): Long? =
        optional(header)?.toLongOrNull() ?: optional(header)?.let {
            throw invalid(header, line)
        }

    private fun CSVRecord.optionalDouble(header: String, line: Int): Double? {
        val parsed = optional(header)?.toDoubleOrNull() ?: optional(header)?.let {
            throw invalid(header, line)
        }
        if (parsed != null && !parsed.isFinite()) {
            throw invalid(header, line)
        }
        return parsed
    }

    private inline fun <reified T : Enum<T>> CSVRecord.enumValue(
        header: String,
        line: Int
    ): T = enumValues<T>().firstOrNull { it.name == required(header, line) }
        ?: throw invalid(header, line)

    private fun invalid(
        header: String,
        line: Int,
        cause: Throwable? = null,
        detail: String = header
    ) = TrackingCsvException(
        reason = TrackingCsvErrorReason.INVALID_VALUE,
        lineNumber = line,
        detail = detail,
        cause = cause
    )

    private fun Reader.withoutUtf8Bom(): Reader {
        val buffered = if (this is BufferedReader) this else buffered()
        buffered.mark(1)
        if (buffered.read() != UTF8_BOM_CODE_POINT) {
            buffered.reset()
        }
        return buffered
    }

    private fun <T> MutableMap<String, T>.putUnique(value: T, line: Int) {
        val id = when (value) {
            is RecordTemplateEntity -> value.id
            is TrackerEntity -> value.id
            is TrackerOptionEntity -> value.id
            is TemplateFieldEntity -> value.id
            is RecordSessionEntity -> value.id
            is DataPointEntity -> value.id
            else -> error("Unsupported CSV entity")
        }
        if (putIfAbsent(id, value) != null) {
            throw TrackingCsvException(
                reason = TrackingCsvErrorReason.DUPLICATE_ID,
                lineNumber = line,
                detail = id
            )
        }
    }

    private val CSVRecord.lineNumber: Int
        get() = recordNumber.toInt() + 1

    private enum class RecordType(val csvValue: String) {
        TEMPLATE("template"),
        TRACKER("tracker"),
        OPTION("option"),
        FIELD("field"),
        SESSION("session"),
        DATA_POINT("data_point")
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = "1"

        private const val SCHEMA_VERSION = "schema_version"
        private const val RECORD_TYPE = "record_type"
        private const val TEMPLATE_ID = "template_id"
        private const val TEMPLATE_NAME = "template_name"
        private const val TEMPLATE_DESCRIPTION = "template_description"
        private const val TEMPLATE_ICON = "template_icon"
        private const val TEMPLATE_COLOR = "template_color"
        private const val TEMPLATE_IS_ACTIVE = "template_is_active"
        private const val TEMPLATE_IS_PINNED = "template_is_pinned"
        private const val TEMPLATE_DISPLAY_ORDER = "template_display_order"
        private const val TRACKER_ID = "tracker_id"
        private const val TRACKER_NAME = "tracker_name"
        private const val TRACKER_TYPE = "tracker_type"
        private const val TRACKER_UNIT = "tracker_unit"
        private const val TRACKER_CONFIG_JSON = "tracker_config_json"
        private const val TRACKER_IS_ACTIVE = "tracker_is_active"
        private const val OPTION_ID = "option_id"
        private const val OPTION_LABEL = "option_label"
        private const val OPTION_NUMERIC_VALUE = "option_numeric_value"
        private const val OPTION_COLOR = "option_color"
        private const val OPTION_DISPLAY_ORDER = "option_display_order"
        private const val OPTION_IS_ACTIVE = "option_is_active"
        private const val FIELD_ID = "field_id"
        private const val FIELD_DISPLAY_ORDER = "field_display_order"
        private const val FIELD_REQUIRED = "field_required"
        private const val FIELD_DISPLAY_NAME_OVERRIDE = "field_display_name_override"
        private const val FIELD_DEFAULT_VALUE_JSON = "field_default_value_json"
        private const val SESSION_ID = "session_id"
        private const val OCCURRED_AT = "occurred_at"
        private const val ZONE_ID = "zone_id"
        private const val SESSION_NOTE = "session_note"
        private const val RECORD_SOURCE = "record_source"
        private const val POINT_ID = "point_id"
        private const val POINT_EPOCH_MILLI = "point_epoch_milli"
        private const val UTC_OFFSET_SECONDS = "utc_offset_seconds"
        private const val VALUE = "value"
        private const val VALUE_LABEL = "label"
        private const val POINT_NOTE = "point_note"
        private const val CREATED_AT = "created_at_epoch_milli"
        private const val UPDATED_AT = "updated_at_epoch_milli"
        private const val UTF8_BOM_CODE_POINT = 0xFEFF
        private const val MIN_UTC_OFFSET_SECONDS = -18 * 60 * 60
        private const val MAX_UTC_OFFSET_SECONDS = 18 * 60 * 60

        val HEADERS: List<String> = listOf(
            SCHEMA_VERSION,
            RECORD_TYPE,
            TEMPLATE_ID,
            TEMPLATE_NAME,
            TEMPLATE_DESCRIPTION,
            TEMPLATE_ICON,
            TEMPLATE_COLOR,
            TEMPLATE_IS_ACTIVE,
            TEMPLATE_IS_PINNED,
            TEMPLATE_DISPLAY_ORDER,
            TRACKER_ID,
            TRACKER_NAME,
            TRACKER_TYPE,
            TRACKER_UNIT,
            TRACKER_CONFIG_JSON,
            TRACKER_IS_ACTIVE,
            OPTION_ID,
            OPTION_LABEL,
            OPTION_NUMERIC_VALUE,
            OPTION_COLOR,
            OPTION_DISPLAY_ORDER,
            OPTION_IS_ACTIVE,
            FIELD_ID,
            FIELD_DISPLAY_ORDER,
            FIELD_REQUIRED,
            FIELD_DISPLAY_NAME_OVERRIDE,
            FIELD_DEFAULT_VALUE_JSON,
            SESSION_ID,
            OCCURRED_AT,
            ZONE_ID,
            SESSION_NOTE,
            RECORD_SOURCE,
            POINT_ID,
            POINT_EPOCH_MILLI,
            UTC_OFFSET_SECONDS,
            VALUE,
            VALUE_LABEL,
            POINT_NOTE,
            CREATED_AT,
            UPDATED_AT
        )
    }
}
