package com.mhss.app.tracking.data.mapping

import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.domain.model.TrackerConfig
import com.mhss.app.tracking.domain.validation.TrackerInputValue

data class TrackerValueMappingRequest(
    val sessionId: String,
    val trackerId: String,
    val config: TrackerConfig,
    val input: TrackerInputValue,
    val required: Boolean,
    val options: List<TrackerOptionEntity>,
    val epochMilli: Long,
    val utcOffsetSeconds: Int,
    val nowEpochMilli: Long
)
