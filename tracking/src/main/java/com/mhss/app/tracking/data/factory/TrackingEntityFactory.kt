package com.mhss.app.tracking.data.factory

import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.data.serialization.TrackerConfigJson
import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackerConfig
import org.koin.core.annotation.Factory

@Factory
class TrackingEntityFactory(
    private val idGenerator: TrackingIdGenerator
) {

    fun createTemplate(
        name: String,
        color: Long,
        nowEpochMilli: Long,
        description: String = "",
        icon: String = "",
        displayOrder: Int = 0
    ) = RecordTemplateEntity(
        id = idGenerator.newId(),
        name = name,
        description = description,
        icon = icon,
        color = color,
        displayOrder = displayOrder,
        createdAtEpochMilli = nowEpochMilli,
        updatedAtEpochMilli = nowEpochMilli
    )

    fun createTracker(
        name: String,
        config: TrackerConfig,
        nowEpochMilli: Long,
        unit: String? = null
    ) = TrackerEntity(
        id = idGenerator.newId(),
        name = name,
        type = config.trackerType,
        unit = unit,
        configJson = TrackerConfigJson.encode(config),
        createdAtEpochMilli = nowEpochMilli,
        updatedAtEpochMilli = nowEpochMilli
    )

    fun createTemplateField(
        templateId: String,
        trackerId: String,
        displayOrder: Int,
        required: Boolean = false,
        displayNameOverride: String? = null,
        defaultValueJson: String? = null
    ) = TemplateFieldEntity(
        id = idGenerator.newId(),
        templateId = templateId,
        trackerId = trackerId,
        displayOrder = displayOrder,
        required = required,
        displayNameOverride = displayNameOverride,
        defaultValueJson = defaultValueJson
    )

    fun createTrackerOption(
        trackerId: String,
        label: String,
        displayOrder: Int,
        numericValue: Double? = null,
        color: Long? = null
    ) = TrackerOptionEntity(
        id = idGenerator.newId(),
        trackerId = trackerId,
        label = label,
        numericValue = numericValue,
        color = color,
        displayOrder = displayOrder
    )

    fun createRecordSession(
        templateId: String,
        occurredAtEpochMilli: Long,
        zoneId: String,
        nowEpochMilli: Long,
        note: String? = null,
        source: RecordSource = RecordSource.MANUAL
    ) = RecordSessionEntity(
        id = idGenerator.newId(),
        templateId = templateId,
        occurredAtEpochMilli = occurredAtEpochMilli,
        zoneId = zoneId,
        note = note,
        source = source,
        createdAtEpochMilli = nowEpochMilli,
        updatedAtEpochMilli = nowEpochMilli
    )

    fun createDataPoint(
        trackerId: String,
        epochMilli: Long,
        utcOffsetSeconds: Int,
        nowEpochMilli: Long,
        sessionId: String? = null,
        value: Double? = null,
        label: String? = null,
        note: String? = null,
        optionId: String? = null
    ) = DataPointEntity(
        id = idGenerator.newId(),
        sessionId = sessionId,
        trackerId = trackerId,
        epochMilli = epochMilli,
        utcOffsetSeconds = utcOffsetSeconds,
        value = value,
        label = label,
        note = note,
        optionId = optionId,
        createdAtEpochMilli = nowEpochMilli,
        updatedAtEpochMilli = nowEpochMilli
    )
}
