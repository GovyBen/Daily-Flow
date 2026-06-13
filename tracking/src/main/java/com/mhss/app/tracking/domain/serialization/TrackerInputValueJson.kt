package com.mhss.app.tracking.domain.serialization

import com.mhss.app.tracking.domain.validation.TrackerInputValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TrackerInputValueJson {
    private val json = Json {
        classDiscriminator = "inputType"
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(input: TrackerInputValue): String = json.encodeToString(input)

    fun decode(value: String): TrackerInputValue = json.decodeFromString(value)
}
