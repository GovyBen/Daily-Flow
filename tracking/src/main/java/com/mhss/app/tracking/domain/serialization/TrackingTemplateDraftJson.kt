package com.mhss.app.tracking.domain.serialization

import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TrackingTemplateDraftJson {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(draft: TrackingTemplateDraft): String = json.encodeToString(draft)

    fun decode(value: String): TrackingTemplateDraft = json.decodeFromString(value)
}
