/*
 * Adapted from Track & Graph DurationInput.kt at
 * 4bb925a731e0537f6330971853770e9aafb51983.
 * Daily Flow replaces the upstream ViewModel and MiniNumericTextField with a
 * controlled Material 3 component that stores seconds.
 */
package com.mhss.app.tracking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mhss.app.tracking.R

const val DURATION_HOURS_TAG = "duration-hours"
const val DURATION_MINUTES_TAG = "duration-minutes"
const val DURATION_SECONDS_TAG = "duration-seconds"
const val DURATION_CLEAR_TAG = "duration-clear"

@Composable
fun DurationInput(
    totalSeconds: Long?,
    onTotalSecondsChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var hours by rememberSaveable { mutableStateOf(totalSeconds.durationHours()) }
    var minutes by rememberSaveable { mutableStateOf(totalSeconds.durationMinutes()) }
    var seconds by rememberSaveable { mutableStateOf(totalSeconds.durationSeconds()) }
    val minuteFocusRequester = remember { FocusRequester() }
    val secondFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(totalSeconds) {
        if (durationInSeconds(hours, minutes, seconds) != totalSeconds) {
            hours = totalSeconds.durationHours()
            minutes = totalSeconds.durationMinutes()
            seconds = totalSeconds.durationSeconds()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DurationPartField(
            value = hours,
            onValueChange = { value ->
                hours = value.digitsOnly(maxLength = 6)
                onTotalSecondsChange(durationInSeconds(hours, minutes, seconds))
            },
            label = stringResource(R.string.tracking_duration_hours),
            testTag = DURATION_HOURS_TAG,
            enabled = enabled,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(
                onNext = { minuteFocusRequester.requestFocus() }
            ),
            modifier = Modifier.weight(1f)
        )
        DurationPartField(
            value = minutes,
            onValueChange = { value ->
                value.validMinuteOrSecond()?.let {
                    minutes = it
                    onTotalSecondsChange(durationInSeconds(hours, minutes, seconds))
                }
            },
            label = stringResource(R.string.tracking_duration_minutes),
            testTag = DURATION_MINUTES_TAG,
            enabled = enabled,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(
                onNext = { secondFocusRequester.requestFocus() }
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(minuteFocusRequester)
        )
        DurationPartField(
            value = seconds,
            onValueChange = { value ->
                value.validMinuteOrSecond()?.let {
                    seconds = it
                    onTotalSecondsChange(durationInSeconds(hours, minutes, seconds))
                }
            },
            label = stringResource(R.string.tracking_duration_seconds),
            testTag = DURATION_SECONDS_TAG,
            enabled = enabled,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(secondFocusRequester)
        )
        TextButton(
            onClick = {
                hours = ""
                minutes = ""
                seconds = ""
                onTotalSecondsChange(null)
                focusManager.clearFocus()
            },
            enabled = enabled && totalSeconds != null,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .testTag(DURATION_CLEAR_TAG)
        ) {
            Text(stringResource(R.string.tracking_clear_duration))
        }
    }
}

@Composable
private fun DurationPartField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    enabled: Boolean,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        modifier = modifier.testTag(testTag)
    )
}

private fun durationInSeconds(
    hours: String,
    minutes: String,
    seconds: String
): Long? {
    if (hours.isBlank() && minutes.isBlank() && seconds.isBlank()) return null
    return hours.toLongOrNull().orZero() * 3_600 +
        minutes.toLongOrNull().orZero() * 60 +
        seconds.toLongOrNull().orZero()
}

private fun String.digitsOnly(maxLength: Int): String =
    filter(Char::isDigit).take(maxLength)

private fun String.validMinuteOrSecond(): String? {
    val filtered = digitsOnly(maxLength = 2)
    return if (filtered.isBlank() || filtered.toInt() <= 59) filtered else null
}

private fun Long?.durationHours(): String =
    this?.let { (it / 3_600).toString() }.orEmpty()

private fun Long?.durationMinutes(): String =
    this?.let { ((it % 3_600) / 60).toString() }.orEmpty()

private fun Long?.durationSeconds(): String =
    this?.let { (it % 60).toString() }.orEmpty()

private fun Long?.orZero(): Long = this ?: 0L
