package com.mhss.app.tracking.presentation.history

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.presentation.components.DateTimeSelectorButtons
import com.mhss.app.tracking.presentation.components.TrackingFieldInput
import com.mhss.app.ui.components.common.DateDialog
import java.util.Date
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

const val TRACKING_HISTORY_TEMPLATE_TAG = "tracking-history-template"
const val TRACKING_HISTORY_DATE_TAG = "tracking-history-date"
const val TRACKING_HISTORY_LIST_TAG = "tracking-history-list"
const val TRACKING_HISTORY_EDITOR_NOTE_TAG = "tracking-history-editor-note"
const val TRACKING_HISTORY_EDITOR_SAVE_TAG = "tracking-history-editor-save"
const val TRACKING_HISTORY_DELETE_CONFIRM_TAG = "tracking-history-delete-confirm"

fun trackingHistoryTemplateOptionTag(id: String) = "tracking-history-template-$id"
fun trackingHistoryCardTag(id: String) = "tracking-history-card-$id"
fun trackingHistoryEditTag(id: String) = "tracking-history-edit-$id"
fun trackingHistoryDeleteTag(id: String) = "tracking-history-delete-$id"

@Composable
fun TrackingHistoryScreen(
    initialTemplateId: String?,
    onBack: () -> Unit,
    viewModel: TrackingHistoryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(initialTemplateId) {
        viewModel.load(initialTemplateId)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            val message = when (event) {
                TrackingHistoryEvent.Updated -> R.string.tracking_history_updated
                TrackingHistoryEvent.Deleted -> R.string.tracking_history_deleted
                TrackingHistoryEvent.OperationFailed ->
                    R.string.tracking_history_operation_failed
            }
            snackbarHostState.showSnackbar(resources.getString(message))
        }
    }
    LaunchedEffect(state.pendingDeleteId) {
        val sessionId = state.pendingDeleteId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(R.string.tracking_history_delete_pending),
            actionLabel = resources.getString(R.string.tracking_undo),
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete(sessionId)
        } else {
            viewModel.commitDelete(sessionId)
        }
    }

    val editorState = state.editor
    val template = state.selectedTemplate
    if (editorState != null && template != null) {
        TrackingRecordEditorContent(
            template = template,
            state = editorState,
            snackbarHostState = snackbarHostState,
            onBack = viewModel::closeEditor,
            onOccurredAtChange = viewModel::updateEditorOccurredAt,
            onNoteChange = viewModel::updateEditorNote,
            onInputChange = viewModel::updateEditorInput,
            onSave = viewModel::saveEditor
        )
    } else {
        TrackingHistoryContent(
            state = state,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            onTemplateSelected = viewModel::selectTemplate,
            onDateSelected = viewModel::selectDay,
            onPreviousDay = { viewModel.moveDay(-1) },
            onNextDay = { viewModel.moveDay(1) },
            onEdit = viewModel::beginEdit,
            onDelete = viewModel::requestDelete
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingHistoryContent(
    state: TrackingHistoryUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onTemplateSelected: (String) -> Unit = {},
    onDateSelected: (Long) -> Unit = {},
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onDelete: (String) -> Unit = {}
) {
    var templateMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showDateDialog by rememberSaveable { mutableStateOf(false) }
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_history_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { templateMenuExpanded = true },
                        enabled = state.templates.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TRACKING_HISTORY_TEMPLATE_TAG)
                    ) {
                        Text(
                            state.selectedTemplate?.name
                                ?: stringResource(R.string.tracking_history_no_template),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = templateMenuExpanded,
                        onDismissRequest = { templateMenuExpanded = false }
                    ) {
                        state.templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    templateMenuExpanded = false
                                    onTemplateSelected(template.id)
                                },
                                modifier = Modifier.testTag(
                                    trackingHistoryTemplateOptionTag(template.id)
                                )
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onPreviousDay) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(
                                R.string.tracking_history_previous_day
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = { showDateDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TRACKING_HISTORY_DATE_TAG)
                    ) {
                        Text(
                            DateFormat.getMediumDateFormat(context)
                                .format(Date(state.selectedDayEpochMilli))
                        )
                    }
                    IconButton(onClick = onNextDay) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(
                                R.string.tracking_history_next_day
                            )
                        )
                    }
                }
            }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.tracking_loading))
                }
                state.selectedTemplate == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.tracking_history_no_template))
                }
                state.records.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.tracking_history_empty))
                }
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TRACKING_HISTORY_LIST_TAG),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 6.dp,
                        end = 16.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.records, key = TrackingRecordHistory::id) { record ->
                        TrackingHistoryCard(
                            template = state.selectedTemplate,
                            record = record,
                            onEdit = { onEdit(record.id) },
                            onDelete = { deleteTargetId = record.id }
                        )
                    }
                }
            }
        }
    }

    if (showDateDialog) {
        DateDialog(
            initialDate = state.selectedDayEpochMilli,
            onDismissRequest = { showDateDialog = false },
            onDatePicked = {
                showDateDialog = false
                onDateSelected(it)
            }
        )
    }

    deleteTargetId?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text(stringResource(R.string.tracking_history_delete_title)) },
            text = { Text(stringResource(R.string.tracking_history_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTargetId = null
                        onDelete(sessionId)
                    },
                    modifier = Modifier.testTag(TRACKING_HISTORY_DELETE_CONFIRM_TAG)
                ) {
                    Text(stringResource(R.string.tracking_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) {
                    Text(stringResource(R.string.tracking_cancel))
                }
            }
        )
    }
}

@Composable
private fun TrackingHistoryCard(
    template: TrackingTemplateSummary,
    record: TrackingRecordHistory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(trackingHistoryCardTag(record.id))
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                DateFormat.getTimeFormat(context).format(Date(record.occurredAtEpochMilli)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            record.historyRows(template).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        row.name ?: stringResource(R.string.tracking_history_archived_field),
                        modifier = Modifier.weight(0.42f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (row.isArchived) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        row.value ?: stringResource(R.string.tracking_history_not_recorded),
                        modifier = Modifier.weight(0.58f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            record.note?.takeIf(String::isNotBlank)?.let { note ->
                Text(
                    stringResource(R.string.tracking_history_session_note_value, note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag(trackingHistoryEditTag(record.id))
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Text(stringResource(R.string.tracking_edit))
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(trackingHistoryDeleteTag(record.id))
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Text(stringResource(R.string.tracking_delete))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingRecordEditorContent(
    template: TrackingTemplateSummary,
    state: TrackingRecordEditorState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onOccurredAtChange: (Long) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onInputChange: (String, TrackerInputValue) -> Unit = { _, _ -> },
    onSave: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_history_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
                item(key = fieldId) {
                    TrackingFieldInput(
                        field = field,
                        input = checkNotNull(state.inputs[fieldId]),
                        onInputChange = { onInputChange(fieldId, it) },
                        enabled = !state.isSaving,
                        showValidationErrors = state.showValidationErrors
                    )
                }
            }
            if (state.hasArchivedFields) {
                item {
                    Text(
                        stringResource(R.string.tracking_history_archived_preserved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
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
                        .testTag(TRACKING_HISTORY_EDITOR_NOTE_TAG)
                )
            }
            item {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TRACKING_HISTORY_EDITOR_SAVE_TAG)
                ) {
                    Text(stringResource(R.string.tracking_save))
                }
                if (state.isSaving) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
