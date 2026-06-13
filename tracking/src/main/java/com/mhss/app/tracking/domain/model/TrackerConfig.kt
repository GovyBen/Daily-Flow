package com.mhss.app.tracking.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TrackerConfig {
    val trackerType: TrackerType
}

@Serializable
@SerialName("multi_select")
data class MultiSelectConfig(
    val maxSelections: Int? = null
) : TrackerConfig {
    override val trackerType = TrackerType.MULTI_SELECT
}

@Serializable
@SerialName("single_select")
data object SingleSelectConfig : TrackerConfig {
    override val trackerType = TrackerType.SINGLE_SELECT
}

@Serializable
@SerialName("counter")
data class CounterConfig(
    val minimum: Long = 0,
    val maximum: Long? = null,
    val step: Long = 1
) : TrackerConfig {
    override val trackerType = TrackerType.COUNTER
}

@Serializable
@SerialName("scale")
data class ScaleConfig(
    val minimum: Double = 0.0,
    val maximum: Double = 10.0,
    val step: Double = 1.0
) : TrackerConfig {
    override val trackerType = TrackerType.SCALE
}

@Serializable
@SerialName("boolean")
data class BooleanConfig(
    val trueLabel: String = "Yes",
    val falseLabel: String = "No"
) : TrackerConfig {
    override val trackerType = TrackerType.BOOLEAN
}

@Serializable
@SerialName("duration")
data class DurationConfig(
    val maximumSeconds: Long? = null
) : TrackerConfig {
    override val trackerType = TrackerType.DURATION
}

@Serializable
@SerialName("number")
data class NumberConfig(
    val minimum: Double? = null,
    val maximum: Double? = null,
    val step: Double? = null,
    val decimalPlaces: Int? = null
) : TrackerConfig {
    override val trackerType = TrackerType.NUMBER
}

@Serializable
@SerialName("text")
data class TextConfig(
    val maximumLength: Int? = null,
    val multiline: Boolean = true
) : TrackerConfig {
    override val trackerType = TrackerType.TEXT
}
