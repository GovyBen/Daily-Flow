package com.mhss.app.tracking.domain.validation

data class TrackerValueValidationResult(
    val errors: List<TrackerValueError> = emptyList()
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

enum class TrackerValueError {
    REQUIRED,
    TYPE_MISMATCH,
    INACTIVE_OPTION,
    TOO_MANY_SELECTIONS,
    NON_INTEGER,
    NON_FINITE,
    BELOW_MINIMUM,
    ABOVE_MAXIMUM,
    INVALID_STEP,
    TOO_MANY_DECIMAL_PLACES,
    TEXT_TOO_LONG,
    MULTILINE_NOT_ALLOWED,
    INVALID_CONFIGURATION
}
