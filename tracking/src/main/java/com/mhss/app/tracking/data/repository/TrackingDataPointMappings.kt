package com.mhss.app.tracking.data.repository

import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint

internal fun DataPointEntity.toRecordedPoint() = TrackingRecordedPoint(
    id = id,
    trackerId = trackerId,
    epochMilli = epochMilli,
    utcOffsetSeconds = utcOffsetSeconds,
    value = value,
    label = label,
    note = note,
    optionId = optionId
)
