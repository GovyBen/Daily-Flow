package com.mhss.app.tracking.data.mapping

import com.mhss.app.tracking.domain.validation.TrackerValueError

class TrackerValueMappingException(
    val errors: List<TrackerValueError>
) : IllegalArgumentException(
    "Tracker input cannot be mapped: ${errors.joinToString()}"
)
