package com.mhss.app.tracking.data.serialization

import com.mhss.app.tracking.domain.model.TrackerConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TrackerConfigJson {
    private val json = Json {
        classDiscriminator = "configType"
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(config: TrackerConfig): String = json.encodeToString(config)

    fun decode(value: String): TrackerConfig = json.decodeFromString(value)
}
