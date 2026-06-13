package com.mhss.app.tracking.domain.validation

import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackerConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

object TrackerValueValidator {

    const val MAX_REASONABLE_DURATION_SECONDS: Long = 366L * 24 * 60 * 60

    fun validate(
        config: TrackerConfig,
        input: TrackerInputValue,
        required: Boolean,
        activeOptionIds: Set<String> = emptySet()
    ): TrackerValueValidationResult {
        if (config.trackerType != input.trackerType) {
            return invalid(TrackerValueError.TYPE_MISMATCH)
        }
        if (input.isEmpty()) {
            return if (required) invalid(TrackerValueError.REQUIRED) else valid()
        }

        return when {
            config is MultiSelectConfig && input is TrackerInputValue.MultiSelect ->
                validateMultiSelect(config, input, activeOptionIds)

            config is SingleSelectConfig && input is TrackerInputValue.SingleSelect ->
                validateSingleSelect(input, activeOptionIds)

            config is CounterConfig && input is TrackerInputValue.Counter ->
                validateCounter(config, input)

            config is ScaleConfig && input is TrackerInputValue.Scale ->
                validateScale(config, input)

            config is BooleanConfig && input is TrackerInputValue.BooleanValue -> valid()

            config is DurationConfig && input is TrackerInputValue.Duration ->
                validateDuration(config, input)

            config is NumberConfig && input is TrackerInputValue.NumberValue ->
                validateNumber(config, input)

            config is TextConfig && input is TrackerInputValue.Text ->
                validateText(config, input)

            else -> invalid(TrackerValueError.TYPE_MISMATCH)
        }
    }

    private fun validateMultiSelect(
        config: MultiSelectConfig,
        input: TrackerInputValue.MultiSelect,
        activeOptionIds: Set<String>
    ): TrackerValueValidationResult {
        val errors = buildList {
            val maximum = config.maxSelections
            if (maximum != null && maximum < 1) {
                add(TrackerValueError.INVALID_CONFIGURATION)
            } else if (maximum != null && input.optionIds.size > maximum) {
                add(TrackerValueError.TOO_MANY_SELECTIONS)
            }
            if (!activeOptionIds.containsAll(input.optionIds)) {
                add(TrackerValueError.INACTIVE_OPTION)
            }
        }
        return result(errors)
    }

    private fun validateSingleSelect(
        input: TrackerInputValue.SingleSelect,
        activeOptionIds: Set<String>
    ): TrackerValueValidationResult {
        return if (input.optionId in activeOptionIds) {
            valid()
        } else {
            invalid(TrackerValueError.INACTIVE_OPTION)
        }
    }

    private fun validateCounter(
        config: CounterConfig,
        input: TrackerInputValue.Counter
    ): TrackerValueValidationResult {
        val value = checkNotNull(input.value)
        if (!value.isFinite()) {
            return invalid(TrackerValueError.NON_FINITE)
        }

        val errors = buildList {
            if (
                config.minimum < 0 ||
                config.step <= 0 ||
                config.maximum?.let { it < config.minimum } == true
            ) {
                add(TrackerValueError.INVALID_CONFIGURATION)
            }
            if (!isWholeNumber(value)) {
                add(TrackerValueError.NON_INTEGER)
            }
            val minimum = max(0L, config.minimum).toDouble()
            if (value < minimum) {
                add(TrackerValueError.BELOW_MINIMUM)
            }
            if (config.maximum != null && value > config.maximum.toDouble()) {
                add(TrackerValueError.ABOVE_MAXIMUM)
            } else if (value > Long.MAX_VALUE.toDouble()) {
                add(TrackerValueError.ABOVE_MAXIMUM)
            }
            if (
                config.step > 0 &&
                isWholeNumber(value) &&
                !isOnStep(value, config.minimum.toDouble(), config.step.toDouble())
            ) {
                add(TrackerValueError.INVALID_STEP)
            }
        }
        return result(errors)
    }

    private fun validateScale(
        config: ScaleConfig,
        input: TrackerInputValue.Scale
    ): TrackerValueValidationResult {
        val value = checkNotNull(input.value)
        return validateNumericRange(
            value = value,
            minimum = config.minimum,
            maximum = config.maximum,
            step = config.step,
            stepBase = config.minimum
        )
    }

    private fun validateDuration(
        config: DurationConfig,
        input: TrackerInputValue.Duration
    ): TrackerValueValidationResult {
        val seconds = checkNotNull(input.seconds)
        val configuredMaximum = config.maximumSeconds
        val errors = buildList {
            if (configuredMaximum != null && configuredMaximum < 0) {
                add(TrackerValueError.INVALID_CONFIGURATION)
            }
            if (seconds < 0) {
                add(TrackerValueError.BELOW_MINIMUM)
            }
            val maximum = minOf(
                configuredMaximum ?: MAX_REASONABLE_DURATION_SECONDS,
                MAX_REASONABLE_DURATION_SECONDS
            )
            if (seconds > maximum) {
                add(TrackerValueError.ABOVE_MAXIMUM)
            }
        }
        return result(errors)
    }

    private fun validateNumber(
        config: NumberConfig,
        input: TrackerInputValue.NumberValue
    ): TrackerValueValidationResult {
        val value = checkNotNull(input.value)
        val errors = validateNumericRange(
            value = value,
            minimum = config.minimum,
            maximum = config.maximum,
            step = config.step,
            stepBase = config.minimum ?: 0.0
        ).errors.toMutableList()

        val decimalPlaces = config.decimalPlaces
        if (decimalPlaces != null) {
            if (decimalPlaces !in 0..15) {
                errors += TrackerValueError.INVALID_CONFIGURATION
            } else if (value.isFinite() && !hasAtMostDecimalPlaces(value, decimalPlaces)) {
                errors += TrackerValueError.TOO_MANY_DECIMAL_PLACES
            }
        }
        return result(errors)
    }

    private fun validateText(
        config: TextConfig,
        input: TrackerInputValue.Text
    ): TrackerValueValidationResult {
        val errors = buildList {
            val maximumLength = config.maximumLength
            if (maximumLength != null && maximumLength < 1) {
                add(TrackerValueError.INVALID_CONFIGURATION)
            } else if (maximumLength != null && input.value.length > maximumLength) {
                add(TrackerValueError.TEXT_TOO_LONG)
            }
            if (!config.multiline && input.value.containsLineBreak()) {
                add(TrackerValueError.MULTILINE_NOT_ALLOWED)
            }
        }
        return result(errors)
    }

    private fun validateNumericRange(
        value: Double,
        minimum: Double?,
        maximum: Double?,
        step: Double?,
        stepBase: Double
    ): TrackerValueValidationResult {
        if (!value.isFinite()) {
            return invalid(TrackerValueError.NON_FINITE)
        }

        val errors = buildList {
            if (
                minimum?.isFinite() == false ||
                maximum?.isFinite() == false ||
                step?.isFinite() == false ||
                (minimum != null && maximum != null && maximum < minimum) ||
                (step != null && step <= 0)
            ) {
                add(TrackerValueError.INVALID_CONFIGURATION)
            }
            if (minimum != null && value < minimum && !nearlyEqual(value, minimum)) {
                add(TrackerValueError.BELOW_MINIMUM)
            }
            if (maximum != null && value > maximum && !nearlyEqual(value, maximum)) {
                add(TrackerValueError.ABOVE_MAXIMUM)
            }
            if (step != null && step > 0 && step.isFinite() && !isOnStep(value, stepBase, step)) {
                add(TrackerValueError.INVALID_STEP)
            }
        }
        return result(errors)
    }

    private fun TrackerInputValue.isEmpty(): Boolean = when (this) {
        is TrackerInputValue.MultiSelect -> optionIds.isEmpty()
        is TrackerInputValue.SingleSelect -> optionId.isNullOrBlank()
        is TrackerInputValue.Counter -> value == null
        is TrackerInputValue.Scale -> value == null
        is TrackerInputValue.BooleanValue -> value == null
        is TrackerInputValue.Duration -> seconds == null
        is TrackerInputValue.NumberValue -> value == null
        is TrackerInputValue.Text -> value.isBlank()
    }

    private fun isWholeNumber(value: Double): Boolean = nearlyEqual(value, round(value))

    private fun isOnStep(value: Double, base: Double, step: Double): Boolean {
        val steps = (value - base) / step
        return nearlyEqual(steps, round(steps))
    }

    private fun hasAtMostDecimalPlaces(value: Double, decimalPlaces: Int): Boolean {
        val scale = 10.0.pow(decimalPlaces)
        return nearlyEqual(value * scale, round(value * scale))
    }

    private fun nearlyEqual(first: Double, second: Double): Boolean {
        val tolerance = 1e-9 * max(1.0, max(abs(first), abs(second)))
        return abs(first - second) <= tolerance
    }

    private fun String.containsLineBreak(): Boolean = any { it == '\n' || it == '\r' }

    private fun valid() = TrackerValueValidationResult()

    private fun invalid(vararg errors: TrackerValueError) =
        TrackerValueValidationResult(errors.toList())

    private fun result(errors: List<TrackerValueError>) =
        TrackerValueValidationResult(errors.distinct())
}
