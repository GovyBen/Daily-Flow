package com.mhss.app.tracking.presentation.history

import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingFieldValue
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackerValueValidator
import com.mhss.app.tracking.domain.validation.emptyInputValue
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToLong

data class TrackingDayRange(
    val startInclusive: Long,
    val endExclusive: Long
)

data class TrackingHistoryFieldRow(
    val name: String?,
    val value: String?,
    val isArchived: Boolean = false
)

internal fun trackingDayRange(
    epochMilli: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): TrackingDayRange {
    val date = Instant.ofEpochMilli(epochMilli).atZone(zoneId).toLocalDate()
    return TrackingDayRange(
        startInclusive = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endExclusive = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    )
}

internal fun moveTrackingDay(
    epochMilli: Long,
    days: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val date = Instant.ofEpochMilli(epochMilli).atZone(zoneId).toLocalDate().plusDays(days)
    return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

internal fun TrackingRecordHistory.toEditorState(
    template: TrackingTemplateSummary
): TrackingRecordEditorState {
    val currentTrackerIds = template.fields.mapNotNullTo(mutableSetOf()) { it.trackerId }
    return TrackingRecordEditorState(
        sessionId = id,
        occurredAtEpochMilli = occurredAtEpochMilli,
        note = note.orEmpty(),
        source = source,
        inputs = template.fields.associate { field ->
            checkNotNull(field.id) to inputFor(field)
        },
        hasArchivedFields = points.any { it.trackerId !in currentTrackerIds }
    )
}

internal fun TrackingRecordHistory.historyRows(
    template: TrackingTemplateSummary
): List<TrackingHistoryFieldRow> {
    val currentTrackerIds = template.fields.mapNotNullTo(mutableSetOf()) { it.trackerId }
    val currentRows = template.fields.map { field ->
        TrackingHistoryFieldRow(
            name = field.displayNameOverride?.takeIf(String::isNotBlank) ?: field.tracker.name,
            value = points.filter { it.trackerId == field.trackerId }.displayValue(field)
        )
    }
    val archivedRows = points
        .filterNot { it.trackerId in currentTrackerIds }
        .groupBy(TrackingRecordedPoint::trackerId)
        .map { (_, archivedPoints) ->
            TrackingHistoryFieldRow(
                name = null,
                value = archivedPoints.rawDisplayValue(),
                isArchived = true
            )
        }
    return currentRows + archivedRows
}

internal fun TrackingRecordEditorState.hasValidInputs(
    template: TrackingTemplateSummary
): Boolean = template.fields.all { field ->
    val fieldId = field.id ?: return@all false
    val input = inputs[fieldId] ?: return@all false
    TrackerValueValidator.validate(
        config = field.tracker.config,
        input = input,
        required = field.required,
        activeOptionIds = field.tracker.options
            .filter { it.isActive }
            .mapNotNullTo(mutableSetOf()) { it.id }
    ).isValid
}

internal fun TrackingRecordEditorState.toCommand(
    template: TrackingTemplateSummary,
    zoneId: ZoneId = ZoneId.systemDefault()
): RecordSessionCommand {
    val instant = Instant.ofEpochMilli(occurredAtEpochMilli)
    return RecordSessionCommand(
        id = sessionId,
        templateId = template.id,
        occurredAtEpochMilli = occurredAtEpochMilli,
        zoneId = zoneId.id,
        utcOffsetSeconds = zoneId.rules.getOffset(instant).totalSeconds,
        note = note.trim().takeIf(String::isNotEmpty),
        source = source,
        values = template.fields.map { field ->
            val fieldId = checkNotNull(field.id)
            TrackingFieldValue(
                fieldId = fieldId,
                input = checkNotNull(inputs[fieldId])
            )
        }
    )
}

private fun TrackingRecordHistory.inputFor(field: TrackingFieldDraft): TrackerInputValue {
    val matching = points.filter { it.trackerId == field.trackerId }
    val activeOptionIds = field.tracker.options
        .filter { it.isActive }
        .mapNotNullTo(mutableSetOf()) { it.id }
    return when (field.tracker.config) {
        is MultiSelectConfig -> TrackerInputValue.MultiSelect(
            matching.mapNotNullTo(mutableSetOf()) { it.optionId }
                .intersect(activeOptionIds)
        )
        SingleSelectConfig -> TrackerInputValue.SingleSelect(
            matching.firstNotNullOfOrNull { it.optionId }
                ?.takeIf { it in activeOptionIds }
        )
        is CounterConfig -> TrackerInputValue.Counter(matching.firstOrNull()?.value)
        is ScaleConfig -> TrackerInputValue.Scale(matching.firstOrNull()?.value)
        is BooleanConfig -> TrackerInputValue.BooleanValue(
            when (matching.firstOrNull()?.value) {
                1.0 -> true
                0.0 -> false
                else -> null
            }
        )
        is DurationConfig -> TrackerInputValue.Duration(
            matching.firstOrNull()?.value?.roundToLong()
        )
        is NumberConfig -> TrackerInputValue.NumberValue(matching.firstOrNull()?.value)
        is TextConfig -> TrackerInputValue.Text(matching.firstOrNull()?.note.orEmpty())
    }.takeIf { it.trackerType == field.tracker.config.trackerType }
        ?: field.tracker.config.trackerType.emptyInputValue()
}

private fun List<TrackingRecordedPoint>.displayValue(field: TrackingFieldDraft): String? {
    if (isEmpty()) return null
    return when (val config = field.tracker.config) {
        is MultiSelectConfig, SingleSelectConfig ->
            mapNotNull(TrackingRecordedPoint::label).joinToString(", ").ifBlank { null }
        is CounterConfig, is ScaleConfig, is NumberConfig ->
            firstOrNull()?.value.formatted(field.tracker.unit)
        is BooleanConfig -> when (firstOrNull()?.value) {
            1.0 -> config.trueLabel
            0.0 -> config.falseLabel
            else -> null
        }
        is DurationConfig -> firstOrNull()?.value?.roundToLong()?.formattedDuration()
        is TextConfig -> firstOrNull()?.note
    }
}

private fun List<TrackingRecordedPoint>.rawDisplayValue(): String? = mapNotNull { point ->
    point.label ?: point.note ?: point.value?.formatted(null)
}.joinToString(", ").ifBlank { null }

private fun Double?.formatted(unit: String?): String? {
    if (this == null) return null
    val valueText = if (isFinite() && this == toLong().toDouble()) {
        toLong().toString()
    } else {
        toString()
    }
    return unit?.takeIf(String::isNotBlank)?.let { "$valueText $it" } ?: valueText
}

private fun Long.formattedDuration(): String {
    val hours = this / 3_600
    val minutes = this % 3_600 / 60
    val seconds = this % 60
    return listOfNotNull(
        hours.takeIf { it > 0 }?.let { "${it}h" },
        minutes.takeIf { it > 0 }?.let { "${it}m" },
        seconds.takeIf { it > 0 || this == 0L }?.let { "${it}s" }
    ).joinToString(" ")
}
