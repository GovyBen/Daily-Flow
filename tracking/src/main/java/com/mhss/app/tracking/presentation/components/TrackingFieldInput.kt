package com.mhss.app.tracking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackerValueError
import com.mhss.app.tracking.domain.validation.TrackerValueValidator
import kotlin.math.roundToInt
import kotlin.math.roundToLong

const val TRACKING_COUNTER_DECREMENT_TAG = "tracking-counter-decrement"
const val TRACKING_COUNTER_INCREMENT_TAG = "tracking-counter-increment"
const val TRACKING_SCALE_TAG = "tracking-scale"
const val TRACKING_SCALE_CLEAR_TAG = "tracking-scale-clear"
const val TRACKING_BOOLEAN_TRUE_TAG = "tracking-boolean-true"
const val TRACKING_BOOLEAN_FALSE_TAG = "tracking-boolean-false"
const val TRACKING_BOOLEAN_CLEAR_TAG = "tracking-boolean-clear"
const val TRACKING_NUMBER_TAG = "tracking-number"
const val TRACKING_TEXT_TAG = "tracking-text"

fun trackingOptionTag(optionId: String): String = "tracking-option-$optionId"

@Composable
fun TrackingFieldInput(
    field: TrackingFieldDraft,
    input: TrackerInputValue,
    onInputChange: (TrackerInputValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showValidationErrors: Boolean = true
) {
    val config = field.tracker.config
    val activeOptions = field.tracker.options.filter { it.isActive && it.id != null }
    val validation = TrackerValueValidator.validate(
        config = config,
        input = input,
        required = field.required,
        activeOptionIds = activeOptions.mapNotNull { it.id }.toSet()
    )
    val label = field.displayNameOverride
        ?.takeIf(String::isNotBlank)
        ?: field.tracker.name

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (field.required) {
                stringResource(R.string.tracking_required_field_label, label)
            } else {
                label
            },
            style = MaterialTheme.typography.titleSmall
        )

        when (config) {
            is MultiSelectConfig -> OptionChips(
                options = activeOptions.map { checkNotNull(it.id) to it.label },
                selectedIds = (input as? TrackerInputValue.MultiSelect)?.optionIds.orEmpty(),
                multiple = true,
                onSelectionChange = {
                    onInputChange(TrackerInputValue.MultiSelect(it))
                },
                enabled = enabled
            )

            SingleSelectConfig -> OptionChips(
                options = activeOptions.map { checkNotNull(it.id) to it.label },
                selectedIds = setOfNotNull(
                    (input as? TrackerInputValue.SingleSelect)?.optionId
                ),
                multiple = false,
                onSelectionChange = {
                    onInputChange(TrackerInputValue.SingleSelect(it.singleOrNull()))
                },
                enabled = enabled
            )

            is CounterConfig -> CounterInput(
                config = config,
                value = (input as? TrackerInputValue.Counter)?.value,
                unit = field.tracker.unit,
                onValueChange = {
                    onInputChange(TrackerInputValue.Counter(it))
                },
                enabled = enabled
            )

            is ScaleConfig -> ScaleInput(
                config = config,
                value = (input as? TrackerInputValue.Scale)?.value,
                unit = field.tracker.unit,
                onValueChange = {
                    onInputChange(TrackerInputValue.Scale(it))
                },
                enabled = enabled
            )

            is BooleanConfig -> BooleanInput(
                config = config,
                value = (input as? TrackerInputValue.BooleanValue)?.value,
                onValueChange = {
                    onInputChange(TrackerInputValue.BooleanValue(it))
                },
                enabled = enabled
            )

            is DurationConfig -> DurationInput(
                totalSeconds = (input as? TrackerInputValue.Duration)?.seconds,
                onTotalSecondsChange = {
                    onInputChange(TrackerInputValue.Duration(it))
                },
                enabled = enabled
            )

            is NumberConfig -> NumberInput(
                value = (input as? TrackerInputValue.NumberValue)?.value,
                unit = field.tracker.unit,
                onValueChange = {
                    onInputChange(TrackerInputValue.NumberValue(it))
                },
                enabled = enabled
            )

            is TextConfig -> OutlinedTextField(
                value = (input as? TrackerInputValue.Text)?.value.orEmpty(),
                onValueChange = {
                    onInputChange(TrackerInputValue.Text(it))
                },
                enabled = enabled,
                singleLine = !config.multiline,
                minLines = if (config.multiline) 3 else 1,
                label = { Text(label) },
                supportingText = config.maximumLength?.let { maximum ->
                    {
                        Text(
                            stringResource(
                                R.string.tracking_character_count,
                                (input as? TrackerInputValue.Text)?.value.orEmpty().length,
                                maximum
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRACKING_TEXT_TAG)
            )
        }

        if (showValidationErrors) {
            validation.errors.forEach { error ->
                Text(
                    text = stringResource(error.messageResource()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionChips(
    options: List<Pair<String, String>>,
    selectedIds: Set<String>,
    multiple: Boolean,
    onSelectionChange: (Set<String>) -> Unit,
    enabled: Boolean
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = id in selectedIds,
                onClick = {
                    val next = when {
                        id in selectedIds -> selectedIds - id
                        multiple -> selectedIds + id
                        else -> setOf(id)
                    }
                    onSelectionChange(next)
                },
                label = { Text(label) },
                enabled = enabled,
                modifier = Modifier.testTag(trackingOptionTag(id))
            )
        }
    }
}

@Composable
private fun CounterInput(
    config: CounterConfig,
    value: Double?,
    unit: String?,
    onValueChange: (Double?) -> Unit,
    enabled: Boolean
) {
    val current = value?.takeIf(Double::isFinite)?.roundToLong()
    val validConfig = config.minimum >= 0 &&
        config.step > 0 &&
        config.maximum?.let { it >= config.minimum } != false
    val decremented = current?.minus(config.step)
    val incremented = current?.plus(config.step)
    val canDecrement = decremented != null && decremented >= config.minimum
    val canIncrement = current == null ||
        config.maximum?.let { maximum ->
            incremented != null && incremented <= maximum
        } != false

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { onValueChange(checkNotNull(decremented).toDouble()) },
            enabled = enabled && validConfig && canDecrement,
            modifier = Modifier.testTag(TRACKING_COUNTER_DECREMENT_TAG)
        ) {
            Text("-")
        }
        Text(
            text = value.formattedWithUnit(unit),
            modifier = Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedButton(
            onClick = {
                onValueChange(
                    if (current == null) {
                        config.minimum.toDouble()
                    } else {
                        checkNotNull(incremented).toDouble()
                    }
                )
            },
            enabled = enabled && validConfig && canIncrement,
            modifier = Modifier.testTag(TRACKING_COUNTER_INCREMENT_TAG)
        ) {
            Text("+")
        }
    }
}

@Composable
private fun ScaleInput(
    config: ScaleConfig,
    value: Double?,
    unit: String?,
    onValueChange: (Double?) -> Unit,
    enabled: Boolean
) {
    val validConfig = config.minimum.isFinite() &&
        config.maximum.isFinite() &&
        config.step.isFinite() &&
        config.maximum > config.minimum &&
        config.step > 0
    val sliderValue = value
        ?.takeIf(Double::isFinite)
        ?.coerceIn(config.minimum, config.maximum)
        ?: config.minimum
    val valueRange = if (validConfig) {
        config.minimum.toFloat()..config.maximum.toFloat()
    } else {
        0f..1f
    }
    val steps = if (validConfig) {
        (((config.maximum - config.minimum) / config.step).roundToInt() - 1)
            .coerceIn(0, 10_000)
    } else {
        0
    }

    Text(
        text = value.formattedWithUnit(unit),
        style = MaterialTheme.typography.titleMedium
    )
    Slider(
        value = if (validConfig) sliderValue.toFloat() else 0f,
        onValueChange = { onValueChange(it.toDouble()) },
        valueRange = valueRange,
        steps = steps,
        enabled = enabled && validConfig,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TRACKING_SCALE_TAG)
    )
    TextButton(
        onClick = { onValueChange(null) },
        enabled = enabled && value != null,
        modifier = Modifier.testTag(TRACKING_SCALE_CLEAR_TAG)
    ) {
        Text(stringResource(R.string.tracking_clear_value))
    }
}

@Composable
private fun BooleanInput(
    config: BooleanConfig,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit,
    enabled: Boolean
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = value == true,
            onClick = { onValueChange(true) },
            label = { Text(config.trueLabel) },
            enabled = enabled,
            modifier = Modifier.testTag(TRACKING_BOOLEAN_TRUE_TAG)
        )
        FilterChip(
            selected = value == false,
            onClick = { onValueChange(false) },
            label = { Text(config.falseLabel) },
            enabled = enabled,
            modifier = Modifier.testTag(TRACKING_BOOLEAN_FALSE_TAG)
        )
        TextButton(
            onClick = { onValueChange(null) },
            enabled = enabled && value != null,
            modifier = Modifier.testTag(TRACKING_BOOLEAN_CLEAR_TAG)
        ) {
            Text(stringResource(R.string.tracking_clear_value))
        }
    }
}

@Composable
private fun NumberInput(
    value: Double?,
    unit: String?,
    onValueChange: (Double?) -> Unit,
    enabled: Boolean
) {
    var text by rememberSaveable { mutableStateOf(value.formatted()) }

    LaunchedEffect(value) {
        if (value != null && text.toDoubleOrNull() != value) {
            text = value.formatted()
        } else if (value == null && text.toDoubleOrNull() != null) {
            text = ""
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { candidate ->
            if (candidate.isValidNumericDraft()) {
                text = candidate
                onValueChange(candidate.toDoubleOrNull())
            }
        },
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(R.string.tracking_number_value)) },
        suffix = unit?.takeIf(String::isNotBlank)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TRACKING_NUMBER_TAG)
    )
}

private fun TrackerValueError.messageResource(): Int = when (this) {
    TrackerValueError.REQUIRED -> R.string.tracking_error_required
    TrackerValueError.TYPE_MISMATCH -> R.string.tracking_error_type_mismatch
    TrackerValueError.INACTIVE_OPTION -> R.string.tracking_error_inactive_option
    TrackerValueError.TOO_MANY_SELECTIONS -> R.string.tracking_error_too_many_selections
    TrackerValueError.NON_INTEGER -> R.string.tracking_error_non_integer
    TrackerValueError.NON_FINITE -> R.string.tracking_error_non_finite
    TrackerValueError.BELOW_MINIMUM -> R.string.tracking_error_below_minimum
    TrackerValueError.ABOVE_MAXIMUM -> R.string.tracking_error_above_maximum
    TrackerValueError.INVALID_STEP -> R.string.tracking_error_invalid_step
    TrackerValueError.TOO_MANY_DECIMAL_PLACES ->
        R.string.tracking_error_too_many_decimal_places
    TrackerValueError.TEXT_TOO_LONG -> R.string.tracking_error_text_too_long
    TrackerValueError.MULTILINE_NOT_ALLOWED -> R.string.tracking_error_multiline_not_allowed
    TrackerValueError.INVALID_CONFIGURATION -> R.string.tracking_error_invalid_configuration
}

private fun Double?.formattedWithUnit(unit: String?): String {
    val valueText = this?.formatted() ?: "-"
    return unit?.takeIf(String::isNotBlank)?.let { "$valueText $it" } ?: valueText
}

private fun Double?.formatted(): String = when {
    this == null -> ""
    isFinite() && this == toLong().toDouble() -> toLong().toString()
    else -> toString()
}

private fun String.isValidNumericDraft(): Boolean =
    isEmpty() || matches(Regex("[+-]?(\\d+(\\.\\d*)?|\\.\\d*)"))
