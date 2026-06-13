package com.mhss.app.tracking.data.id

import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class UuidTrackingIdGenerator : TrackingIdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
