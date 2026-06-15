package com.mhss.app.tracking.presentation.csv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.data.csv.TrackingCsvCounts
import com.mhss.app.tracking.data.csv.TrackingCsvErrorReason
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import org.koin.androidx.compose.koinViewModel

const val TRACKING_CSV_EXPORT_TAG = "tracking-csv-export"
const val TRACKING_CSV_IMPORT_TAG = "tracking-csv-import"
const val TRACKING_CSV_PREVIEW_TAG = "tracking-csv-preview"
const val TRACKING_CSV_CONFIRM_TAG = "tracking-csv-confirm"

@Composable
fun TrackingCsvScreen(
    onBack: () -> Unit,
    viewModel: TrackingCsvViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Custom(
            listOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv"
            )
        ),
        selectionMode = FilePickerSelectionMode.Single
    ) { files ->
        files.firstOrNull()?.uri?.toString()?.let(viewModel::previewImport)
    }
    val exportLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Folder,
        selectionMode = FilePickerSelectionMode.Single
    ) { files ->
        files.firstOrNull()?.uri?.toString()?.let(viewModel::export)
    }

    TrackingCsvContent(
        state = state,
        onBack = onBack,
        onExport = exportLauncher::launch,
        onChooseImport = importLauncher::launch,
        onConfirmImport = viewModel::confirmImport,
        onCancelPreview = viewModel::dismissPreview,
        onDismissMessage = viewModel::dismissMessage
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingCsvContent(
    state: TrackingCsvUiState,
    onBack: () -> Unit = {},
    onExport: () -> Unit = {},
    onChooseImport: () -> Unit = {},
    onConfirmImport: () -> Unit = {},
    onCancelPreview: () -> Unit = {},
    onDismissMessage: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_csv_title)) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isWorking) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.status?.let { status ->
                MessageCard(
                    message = when (status) {
                        is TrackingCsvStatus.Exported -> pluralStringResource(
                            R.plurals.tracking_csv_exported,
                            status.counts.totalRows,
                            status.fileName,
                            status.counts.totalRows
                        )
                        is TrackingCsvStatus.Imported -> pluralStringResource(
                            R.plurals.tracking_csv_imported,
                            status.counts.totalRows,
                            status.counts.totalRows
                        )
                    },
                    onDismiss = onDismissMessage
                )
            }
            state.error?.let { error ->
                MessageCard(
                    message = error.localizedMessage(),
                    isError = true,
                    onDismiss = onDismissMessage
                )
            }
            CsvActionCard(
                title = stringResource(R.string.tracking_csv_export_title),
                description = stringResource(R.string.tracking_csv_export_description),
                buttonText = stringResource(R.string.tracking_csv_export_action),
                icon = {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                },
                enabled = !state.isWorking,
                tag = TRACKING_CSV_EXPORT_TAG,
                onClick = onExport
            )
            CsvActionCard(
                title = stringResource(R.string.tracking_csv_import_title),
                description = stringResource(R.string.tracking_csv_import_description),
                buttonText = stringResource(R.string.tracking_csv_choose_file),
                icon = {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                },
                enabled = !state.isWorking,
                tag = TRACKING_CSV_IMPORT_TAG,
                onClick = onChooseImport
            )
            state.preview?.let { preview ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TRACKING_CSV_PREVIEW_TAG)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.tracking_csv_preview_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            preview.sourceName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CsvCounts(preview.counts)
                        Text(
                            stringResource(R.string.tracking_csv_import_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onCancelPreview,
                                enabled = !state.isWorking
                            ) {
                                Text(stringResource(R.string.tracking_csv_cancel))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onConfirmImport,
                                enabled = !state.isWorking,
                                modifier = Modifier.testTag(TRACKING_CSV_CONFIRM_TAG)
                            ) {
                                if (state.isWorking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.tracking_csv_confirm_import))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CsvActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                icon()
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun CsvCounts(counts: TrackingCsvCounts) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.tracking_csv_count_templates, counts.templates))
        Text(stringResource(R.string.tracking_csv_count_trackers, counts.trackers))
        Text(stringResource(R.string.tracking_csv_count_options, counts.options))
        Text(stringResource(R.string.tracking_csv_count_fields, counts.fields))
        Text(stringResource(R.string.tracking_csv_count_sessions, counts.sessions))
        Text(stringResource(R.string.tracking_csv_count_points, counts.dataPoints))
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean = false,
    onDismiss: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tracking_csv_dismiss))
            }
        }
    }
}

@Composable
private fun TrackingCsvUiError.localizedMessage(): String {
    val base = stringResource(
        when (reason) {
            TrackingCsvErrorReason.INVALID_HEADERS -> R.string.tracking_csv_error_headers
            TrackingCsvErrorReason.UNSUPPORTED_VERSION ->
                R.string.tracking_csv_error_version
            TrackingCsvErrorReason.UNKNOWN_RECORD_TYPE ->
                R.string.tracking_csv_error_record_type
            TrackingCsvErrorReason.MISSING_VALUE ->
                R.string.tracking_csv_error_missing_value
            TrackingCsvErrorReason.INVALID_VALUE -> R.string.tracking_csv_error_invalid_value
            TrackingCsvErrorReason.DUPLICATE_ID -> R.string.tracking_csv_error_duplicate
            TrackingCsvErrorReason.BROKEN_REFERENCE ->
                R.string.tracking_csv_error_reference
            TrackingCsvErrorReason.READ_FAILED -> R.string.tracking_csv_error_read
            TrackingCsvErrorReason.WRITE_FAILED -> R.string.tracking_csv_error_write
            TrackingCsvErrorReason.IMPORT_FAILED -> R.string.tracking_csv_error_import
        }
    )
    return when {
        lineNumber != null && detail != null -> stringResource(
            R.string.tracking_csv_error_line_detail,
            lineNumber,
            base,
            detail
        )
        lineNumber != null -> stringResource(
            R.string.tracking_csv_error_line,
            lineNumber,
            base
        )
        detail != null -> "$base ($detail)"
        else -> base
    }
}
