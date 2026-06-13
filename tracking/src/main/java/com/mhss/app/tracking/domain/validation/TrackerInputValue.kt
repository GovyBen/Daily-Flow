package com.mhss.app.tracking.domain.validation

import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TrackerInputValue {
    val trackerType: TrackerType

    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        val optionIds: Set<String>
    ) : TrackerInputValue {
        override val trackerType = TrackerType.MULTI_SELECT
    }

    @Serializable
    @SerialName("single_select")
    data class SingleSelect(
        val optionId: String?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.SINGLE_SELECT
    }

    @Serializable
    @SerialName("counter")
    data class Counter(
        val value: Double?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.COUNTER
    }

    @Serializable
    @SerialName("scale")
    data class Scale(
        val value: Double?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.SCALE
    }

    @Serializable
    @SerialName("boolean")
    data class BooleanValue(
        val value: Boolean?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.BOOLEAN
    }

    @Serializable
    @SerialName("duration")
    data class Duration(
        val seconds: Long?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.DURATION
    }

    @Serializable
    @SerialName("number")
    data class NumberValue(
        val value: Double?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.NUMBER
    }

    @Serializable
    @SerialName("text")
    data class Text(
        val value: String
    ) : TrackerInputValue {
        override val trackerType = TrackerType.TEXT
    }
}

fun TrackerType.emptyInputValue(): TrackerInputValue = when (this) {
    TrackerType.MULTI_SELECT -> TrackerInputValue.MultiSelect(emptySet())
    TrackerType.SINGLE_SELECT -> TrackerInputValue.SingleSelect(null)
    TrackerType.COUNTER -> TrackerInputValue.Counter(null)
    TrackerType.SCALE -> TrackerInputValue.Scale(null)
    TrackerType.BOOLEAN -> TrackerInputValue.BooleanValue(null)
    TrackerType.DURATION -> TrackerInputValue.Duration(null)
    TrackerType.NUMBER -> TrackerInputValue.NumberValue(null)
    TrackerType.TEXT -> TrackerInputValue.Text("")
}
