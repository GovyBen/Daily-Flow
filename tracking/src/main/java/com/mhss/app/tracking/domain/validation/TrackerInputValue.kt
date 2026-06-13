package com.mhss.app.tracking.domain.validation

import com.mhss.app.tracking.domain.model.TrackerType

sealed interface TrackerInputValue {
    val trackerType: TrackerType

    data class MultiSelect(
        val optionIds: Set<String>
    ) : TrackerInputValue {
        override val trackerType = TrackerType.MULTI_SELECT
    }

    data class SingleSelect(
        val optionId: String?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.SINGLE_SELECT
    }

    data class Counter(
        val value: Double?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.COUNTER
    }

    data class Scale(
        val value: Double?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.SCALE
    }

    data class BooleanValue(
        val value: Boolean?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.BOOLEAN
    }

    data class Duration(
        val seconds: Long?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.DURATION
    }

    data class NumberValue(
        val value: Double?
    ) : TrackerInputValue {
        override val trackerType = TrackerType.NUMBER
    }

    data class Text(
        val value: String
    ) : TrackerInputValue {
        override val trackerType = TrackerType.TEXT
    }
}
