package com.mhss.app.tracking.presentation.record

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.suggestion.TrackingValueSuggestions
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.presentation.components.DateTimeSelectorButtons
import com.mhss.app.tracking.presentation.components.TrackingFieldInput
import java.util.Date
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

const val TRACKING_QUICK_RECORD_LIST_TAG = "tracking-quick-record-list"
const val TRACKING_QUICK_RECORD_NOTE_TAG = "tracking-quick-record-note"
const val TRACKING_QUICK_RECORD_SAVE_TAG = "tracking-quick-record-save"
const val TRACKING_QUICK_RECORD_RESULT_TAG = "tracking-quick-record-result"

fun trackingQuickRecordSuggestionTag(fieldId: String, index: Int) =
    "tracking-quick-record-suggestion-$fieldId-$index"

@Composable
fun TrackingQuickRecordScreen(
    templateId: String,
    onBack: () -> Unit,
    viewModel: TrackingQuickRecordViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(templateId) {
        viewModel.load(templateId)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event == TrackingQuickRecordEvent.SaveFailed) {
                snackbarHostState.showSnackbar(
                    resources.getString(R.string.tracking_record_save_failed)
                )
            }
        }
    }

    TrackingQuickRecordContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOccurredAtChange = viewModel::updateOccurredAt,
        onNoteChange = viewModel::updateNote,
        onInputChange = viewModel::updateInput,
        onSave = viewModel::save,
        onRecordAnother = viewModel::recordAnother,
        onDone = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingQuickRecordContent(
    state: TrackingQuickRecordUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onOccurredAtChange: (Long) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onInputChange: (String, TrackerInputValue) -> Unit = { _, _ -> },
    onSave: () -> Unit = {},
    onRecordAnother: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val template = state.template
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        template?.name ?: stringResource(R.string.tracking_quick_record_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.tracking_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.tracking_loading))
            }

            state.templateMissing || template == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.tracking_template_not_found))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .testTag(TRACKING_QUICK_RECORD_LIST_TAG),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.tracking_record_time),
                                style = MaterialTheme.typography.titleSmall
                            )
                            DateTimeSelectorButtons(
                                selectedEpochMilli = state.occurredAtEpochMilli,
                                onDateTimeSelected = onOccurredAtChange,
                                enabled = !state.isSaving
                            )
                        }
                    }
                    template.fields.forEach { field ->
                        val fieldId = checkNotNull(field.id)
                        val fieldState = checkNotNull(state.fields[fieldId])
                        item(key = fieldId) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrackingFieldInput(
                                    field = field,
                                    input = fieldState.input,
                                    onInputChange = { onInputChange(fieldId, it) },
                                    enabled = !state.isSaving,
                                    showValidationErrors = state.showValidationErrors
                                )
                                SuggestionChips(
                                    field = field,
                                    currentInput = fieldState.input,
                                    suggestions = fieldState.suggestions,
                                    enabled = !state.isSaving,
                                    onSelect = { onInputChange(fieldId, it) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = state.note,
                            onValueChange = onNoteChange,
                            enabled = !state.isSaving,
                            label = { Text(stringResource(R.string.tracking_session_note)) },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TRACKING_QUICK_RECORD_NOTE_TAG)
                        )
                    }
                    item {
                        Button(
                            onClick = onSave,
                            enabled = !state.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TRACKING_QUICK_RECORD_SAVE_TAG)
                        ) {
                            Text(stringResource(R.string.tracking_save_record))
                        }
                        if (state.isSaving) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    state.savedResult?.let { result ->
        SavedRecordDialog(
            result = result,
            onRecordAnother = onRecordAnother,
            onDone = onDone
        )
    }
}

@Composable
private fun SuggestionChips(
    field: TrackingFieldDraft,
    currentInput: TrackerInputValue,
    suggestions: TrackingValueSuggestions,
    enabled: Boolean,
    onSelect: (TrackerInputValue) -> Unit
) {
    val inputs = buildList {
        suggestions.defaultValue?.let(::add)
        suggestions.counterIncrement?.let(::add)
        addAll(suggestions.recent.map { it.input })
        addAll(suggestions.frequent.map { it.input })
    }.distinct().filterNot { it == currentInput }.take(5)
    if (inputs.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        inputs.forEachIndexed { index, input ->
            AssistChip(
                onClick = { onSelect(input) },
                label = { Text(input.displayLabel(field)) },
                enabled = enabled,
                modifier = Modifier.testTag(
                    trackingQuickRecordSuggestionTag(checkNotNull(field.id), index)
                )
            )
        }
    }
}

@Composable
private fun SavedRecordDialog(
    result: TrackingQuickRecordResult,
    onRecordAnother: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val dateTime = buildString {
        append(DateFormat.getMediumDateFormat(context).format(Date(result.occurredAtEpochMilli)))
        append(' ')
        append(DateFormat.getTimeFormat(context).format(Date(result.occurredAtEpochMilli)))
    }
    AlertDialog(
        onDismissRequest = onDone,
        title = {
            Text(
                stringResource(R.string.tracking_record_saved_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.testTag(TRACKING_QUICK_RECORD_RESULT_TAG),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(result.templateName)
                Text(dateTime)
                Text(
                    stringResource(R.string.tracking_record_session_id, result.sessionId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRecordAnother) {
                Text(stringResource(R.string.tracking_record_another))
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.tracking_done))
            }
        }
    )
}

private fun TrackerInputValue.displayLabel(field: TrackingFieldDraft): String = when (this) {
    is TrackerInputValue.MultiSelect -> optionIds.mapNotNull { optionId ->
        field.tracker.options.firstOrNull { it.id == optionId }?.label
    }.joinToString(", ")
    is TrackerInputValue.SingleSelect -> field.tracker.options
        .firstOrNull { it.id == optionId }
        ?.label
        .orEmpty()
    is TrackerInputValue.Counter -> value.formatted(field.tracker.unit)
    is TrackerInputValue.Scale -> value.formatted(field.tracker.unit)
    is TrackerInputValue.BooleanValue -> {
        val config = field.tracker.config as BooleanConfig
        when (value) {
            true -> config.trueLabel
            false -> config.falseLabel
            null -> ""
        }
    }
    is TrackerInputValue.Duration -> seconds?.let { total ->
        val hours = total / 3_600
        val minutes = total % 3_600 / 60
        val remainingSeconds = total % 60
        listOfNotNull(
            hours.takeIf { it > 0 }?.let { "${it}h" },
            minutes.takeIf { it > 0 }?.let { "${it}m" },
            remainingSeconds.takeIf { it > 0 || total == 0L }?.let { "${it}s" }
        ).joinToString(" ")
    }.orEmpty()
    is TrackerInputValue.NumberValue -> value.formatted(field.tracker.unit)
    is TrackerInputValue.Text -> value
}

private fun Double?.formatted(unit: String?): String {
    val valueText = when {
        this == null -> ""
        isFinite() && this == toLong().toDouble() -> toLong().toString()
        else -> toString()
    }
    return unit?.takeIf(String::isNotBlank)?.let { "$valueText $it" } ?: valueText
}
