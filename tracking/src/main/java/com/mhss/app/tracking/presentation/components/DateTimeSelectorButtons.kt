/*
 * Adapted from Track & Graph DateTimeSelectorButtons.kt at
 * 4bb925a731e0537f6330971853770e9aafb51983.
 * Daily Flow reuses the existing core UI dialogs and stores epoch milliseconds.
 */
package com.mhss.app.tracking.presentation.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mhss.app.tracking.R
import com.mhss.app.ui.components.common.DateDialog
import com.mhss.app.ui.components.common.TimeDialog
import java.util.Date

const val TRACKING_DATE_BUTTON_TAG = "tracking-date-button"
const val TRACKING_TIME_BUTTON_TAG = "tracking-time-button"

@Composable
fun DateTimeSelectorButtons(
    selectedEpochMilli: Long,
    onDateTimeSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var showDateDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SelectorButton(
            text = DateFormat.getMediumDateFormat(context).format(Date(selectedEpochMilli)),
            contentDescription = stringResource(R.string.tracking_select_date),
            testTag = TRACKING_DATE_BUTTON_TAG,
            enabled = enabled,
            onClick = { showDateDialog = true },
            modifier = Modifier.weight(1f)
        )
        SelectorButton(
            text = DateFormat.getTimeFormat(context).format(Date(selectedEpochMilli)),
            contentDescription = stringResource(R.string.tracking_select_time),
            testTag = TRACKING_TIME_BUTTON_TAG,
            enabled = enabled,
            onClick = { showTimeDialog = true },
            modifier = Modifier.weight(1f)
        )
    }

    if (showDateDialog) {
        DateDialog(
            initialDate = selectedEpochMilli,
            onDismissRequest = { showDateDialog = false },
            onDatePicked = {
                onDateTimeSelected(it)
                showDateDialog = false
            }
        )
    }
    if (showTimeDialog) {
        TimeDialog(
            initialDate = selectedEpochMilli,
            onDismissRequest = { showTimeDialog = false },
            onTimePicked = {
                onDateTimeSelected(it)
                showTimeDialog = false
            }
        )
    }
}

@Composable
private fun SelectorButton(
    text: String,
    contentDescription: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .widthIn(min = 104.dp)
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag)
    ) {
        Text(text)
    }
}
