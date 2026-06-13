package com.mhss.app.tracking.domain.model

import com.mhss.app.tracking.domain.validation.TrackerInputValue

data class TrackingTemplateDraft(
    val id: String? = null,
    val name: String,
    val description: String = "",
    val icon: String = "",
    val color: Long,
    val displayOrder: Int = 0,
    val fields: List<TrackingFieldDraft>
)

data class TrackingFieldDraft(
    val id: String? = null,
    val trackerId: String? = null,
    val tracker: TrackingTrackerDraft,
    val displayOrder: Int,
    val required: Boolean = false,
    val displayNameOverride: String? = null,
    val defaultValueJson: String? = null
)

data class TrackingTrackerDraft(
    val id: String? = null,
    val name: String,
    val config: TrackerConfig,
    val unit: String? = null,
    val options: List<TrackingOptionDraft> = emptyList()
)

data class TrackingOptionDraft(
    val id: String? = null,
    val label: String,
    val numericValue: Double? = null,
    val color: Long? = null,
    val displayOrder: Int,
    val isActive: Boolean = true
)

data class TrackingTemplateSummary(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: Long,
    val isPinned: Boolean,
    val displayOrder: Int,
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long,
    val lastRecordedAtEpochMilli: Long?,
    val fields: List<TrackingFieldDraft>
)

data class RecordSessionCommand(
    val id: String? = null,
    val templateId: String,
    val occurredAtEpochMilli: Long,
    val zoneId: String,
    val utcOffsetSeconds: Int,
    val note: String? = null,
    val source: RecordSource = RecordSource.MANUAL,
    val values: List<TrackingFieldValue>
)

data class TrackingFieldValue(
    val fieldId: String,
    val input: TrackerInputValue
)

data class TrackingRecordHistory(
    val id: String,
    val templateId: String,
    val occurredAtEpochMilli: Long,
    val zoneId: String,
    val note: String?,
    val source: RecordSource,
    val points: List<TrackingRecordedPoint>
)

data class TrackingRecordedPoint(
    val id: String,
    val trackerId: String,
    val epochMilli: Long,
    val utcOffsetSeconds: Int,
    val value: Double?,
    val label: String?,
    val note: String?,
    val optionId: String?
)

data class TrackingSuggestedValue(
    val value: Double?,
    val label: String?,
    val note: String?,
    val optionId: String?,
    val lastUsedAtEpochMilli: Long,
    val usageCount: Int = 1
)
