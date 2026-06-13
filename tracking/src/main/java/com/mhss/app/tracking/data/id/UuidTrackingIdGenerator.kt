package com.mhss.app.tracking.data.id

import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import kotlin.uuid.Uuid

class UuidTrackingIdGenerator : TrackingIdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
