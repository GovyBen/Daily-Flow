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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerValueValidatorTest {

    @Test
    fun emptyValuesRespectRequiredFlag() {
        val cases = listOf(
            case(MultiSelectConfig(), TrackerInputValue.MultiSelect(emptySet())),
            case(SingleSelectConfig, TrackerInputValue.SingleSelect(null)),
            case(CounterConfig(), TrackerInputValue.Counter(null)),
            case(ScaleConfig(), TrackerInputValue.Scale(null)),
            case(BooleanConfig(), TrackerInputValue.BooleanValue(null)),
            case(DurationConfig(), TrackerInputValue.Duration(null)),
            case(NumberConfig(), TrackerInputValue.NumberValue(null)),
            case(TextConfig(), TrackerInputValue.Text("  "))
        )

        cases.forEach { case ->
            assertTrue(validate(case, required = false).isValid)
            assertEquals(
                listOf(TrackerValueError.REQUIRED),
                validate(case, required = true).errors
            )
        }
    }

    @Test
    fun validBoundaryValuesPass() {
        val cases = listOf(
            case(
                MultiSelectConfig(maxSelections = 2),
                TrackerInputValue.MultiSelect(setOf("a", "b")),
                setOf("a", "b")
            ),
            case(
                SingleSelectConfig,
                TrackerInputValue.SingleSelect("a"),
                setOf("a")
            ),
            case(CounterConfig(maximum = 10, step = 2), TrackerInputValue.Counter(10.0)),
            case(
                ScaleConfig(minimum = 0.0, maximum = 1.0, step = 0.1),
                TrackerInputValue.Scale(0.3)
            ),
            case(BooleanConfig(), TrackerInputValue.BooleanValue(false)),
            case(
                DurationConfig(maximumSeconds = 3_600),
                TrackerInputValue.Duration(3_600)
            ),
            case(
                NumberConfig(minimum = -5.0, maximum = 5.0, step = 0.25, decimalPlaces = 2),
                TrackerInputValue.NumberValue(1.25)
            ),
            case(
                TextConfig(maximumLength = 4, multiline = false),
                TrackerInputValue.Text("text")
            )
        )

        cases.forEach { case ->
            assertTrue("$case should be valid", validate(case).isValid)
        }
    }

    @Test
    fun invalidValuesReturnStableErrors() {
        val cases = listOf(
            invalidCase(
                MultiSelectConfig(maxSelections = 1),
                TrackerInputValue.MultiSelect(setOf("a", "b")),
                setOf("a", "b"),
                TrackerValueError.TOO_MANY_SELECTIONS
            ),
            invalidCase(
                MultiSelectConfig(),
                TrackerInputValue.MultiSelect(setOf("inactive")),
                emptySet(),
                TrackerValueError.INACTIVE_OPTION
            ),
            invalidCase(
                SingleSelectConfig,
                TrackerInputValue.SingleSelect("inactive"),
                emptySet(),
                TrackerValueError.INACTIVE_OPTION
            ),
            invalidCase(
                CounterConfig(),
                TrackerInputValue.Counter(-1.0),
                error = TrackerValueError.BELOW_MINIMUM
            ),
            invalidCase(
                CounterConfig(),
                TrackerInputValue.Counter(1.5),
                error = TrackerValueError.NON_INTEGER
            ),
            invalidCase(
                CounterConfig(step = 2),
                TrackerInputValue.Counter(3.0),
                error = TrackerValueError.INVALID_STEP
            ),
            invalidCase(
                ScaleConfig(minimum = 0.0, maximum = 10.0),
                TrackerInputValue.Scale(-1.0),
                error = TrackerValueError.BELOW_MINIMUM
            ),
            invalidCase(
                ScaleConfig(minimum = 0.0, maximum = 10.0),
                TrackerInputValue.Scale(11.0),
                error = TrackerValueError.ABOVE_MAXIMUM
            ),
            invalidCase(
                ScaleConfig(minimum = 0.0, maximum = 1.0, step = 0.2),
                TrackerInputValue.Scale(0.3),
                error = TrackerValueError.INVALID_STEP
            ),
            invalidCase(
                DurationConfig(),
                TrackerInputValue.Duration(-1),
                error = TrackerValueError.BELOW_MINIMUM
            ),
            invalidCase(
                DurationConfig(maximumSeconds = 60),
                TrackerInputValue.Duration(61),
                error = TrackerValueError.ABOVE_MAXIMUM
            ),
            invalidCase(
                DurationConfig(),
                TrackerInputValue.Duration(
                    TrackerValueValidator.MAX_REASONABLE_DURATION_SECONDS + 1
                ),
                error = TrackerValueError.ABOVE_MAXIMUM
            ),
            invalidCase(
                NumberConfig(),
                TrackerInputValue.NumberValue(Double.NaN),
                error = TrackerValueError.NON_FINITE
            ),
            invalidCase(
                NumberConfig(),
                TrackerInputValue.NumberValue(Double.POSITIVE_INFINITY),
                error = TrackerValueError.NON_FINITE
            ),
            invalidCase(
                NumberConfig(minimum = 0.0),
                TrackerInputValue.NumberValue(-1.0),
                error = TrackerValueError.BELOW_MINIMUM
            ),
            invalidCase(
                NumberConfig(maximum = 10.0),
                TrackerInputValue.NumberValue(11.0),
                error = TrackerValueError.ABOVE_MAXIMUM
            ),
            invalidCase(
                NumberConfig(step = 0.5),
                TrackerInputValue.NumberValue(1.25),
                error = TrackerValueError.INVALID_STEP
            ),
            invalidCase(
                NumberConfig(decimalPlaces = 2),
                TrackerInputValue.NumberValue(1.234),
                error = TrackerValueError.TOO_MANY_DECIMAL_PLACES
            ),
            invalidCase(
                TextConfig(maximumLength = 3),
                TrackerInputValue.Text("four"),
                error = TrackerValueError.TEXT_TOO_LONG
            ),
            invalidCase(
                TextConfig(multiline = false),
                TrackerInputValue.Text("two\nlines"),
                error = TrackerValueError.MULTILINE_NOT_ALLOWED
            )
        )

        cases.forEach { case ->
            val result = validate(case.validationCase)
            assertFalse("$case should be invalid", result.isValid)
            assertTrue("$case should contain ${case.error}", case.error in result.errors)
        }
    }

    @Test
    fun mismatchedInputTypeIsRejected() {
        val result = TrackerValueValidator.validate(
            config = ScaleConfig(),
            input = TrackerInputValue.Text("5"),
            required = true
        )

        assertEquals(listOf(TrackerValueError.TYPE_MISMATCH), result.errors)
    }

    private fun validate(
        case: ValidationCase,
        required: Boolean = true
    ): TrackerValueValidationResult {
        return TrackerValueValidator.validate(
            config = case.config,
            input = case.input,
            required = required,
            activeOptionIds = case.activeOptionIds
        )
    }

    private fun case(
        config: TrackerConfig,
        input: TrackerInputValue,
        activeOptionIds: Set<String> = emptySet()
    ) = ValidationCase(config, input, activeOptionIds)

    private fun invalidCase(
        config: TrackerConfig,
        input: TrackerInputValue,
        activeOptionIds: Set<String> = emptySet(),
        error: TrackerValueError
    ) = InvalidValidationCase(case(config, input, activeOptionIds), error)
}

private data class ValidationCase(
    val config: TrackerConfig,
    val input: TrackerInputValue,
    val activeOptionIds: Set<String>
)

private data class InvalidValidationCase(
    val validationCase: ValidationCase,
    val error: TrackerValueError
)
