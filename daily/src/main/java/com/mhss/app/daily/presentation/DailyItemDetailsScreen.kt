package com.mhss.app.daily.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.ui.R
import com.mhss.app.util.permissions.Permission
import com.mhss.app.util.permissions.rememberPermissionState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyItemDetailsScreen(
    itemId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: DailyItemDetailsViewModel = koinViewModel(
        parameters = { parametersOf(itemId) }
    )
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.navigateUp) {
        if (state.navigateUp) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_item_title)) },
                navigationIcon = {
                    val backContentDescription = stringResource(R.string.back)
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backContentDescription)
                    }
                },
                actions = {
                    state.item?.let { item ->
                        val editContentDescription = stringResource(R.string.daily_item_edit_action)
                        val archiveContentDescription = stringResource(R.string.daily_item_archive)
                        val deleteContentDescription = stringResource(R.string.daily_item_delete_action)
                        IconButton(onClick = { onEdit(item.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = editContentDescription)
                        }
                        IconButton(onClick = viewModel::archive) {
                            Icon(Icons.Rounded.Archive, contentDescription = archiveContentDescription)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = deleteContentDescription)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val item = state.item
            if (item == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.daily_item_not_found))
                }
            } else {
                DailyItemDetailsContent(
                    item = item,
                    onToggleComplete = viewModel::toggleComplete,
                    onSyncNow = viewModel::syncNow,
                    onDisableSync = viewModel::disableSync,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.daily_item_delete_title)) },
            text = { Text(stringResource(R.string.daily_item_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    }
                ) {
                    Text(stringResource(R.string.daily_item_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DailyItemDetailsContent(
    item: DailyItem,
    onToggleComplete: () -> Unit,
    onSyncNow: () -> Unit,
    onDisableSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val writeCalendarPermissionState = rememberPermissionState(Permission.WRITE_CALENDAR)
    val noDescriptionLabel = stringResource(R.string.daily_item_no_description)
    val completeLabel = stringResource(R.string.daily_item_complete)
    val reopenLabel = stringResource(R.string.daily_item_reopen)
    val enabledLabel = stringResource(R.string.daily_item_enabled)
    val disabledLabel = stringResource(R.string.daily_item_disabled)
    val yesLabel = stringResource(R.string.daily_item_yes)
    val noLabel = stringResource(R.string.daily_item_no)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            item.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            item.description.ifBlank { noDescriptionLabel },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onToggleComplete, modifier = Modifier.fillMaxWidth()) {
            Text(if (item.isCompleted) reopenLabel else completeLabel)
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine(stringResource(R.string.daily_item_kind), stringResource(item.kind.labelRes()))
                DetailLine(stringResource(R.string.daily_item_status), stringResource(item.status.labelRes()))
                DetailLine(stringResource(R.string.priority), stringResource(item.priority.labelRes()))
                DetailLine(stringResource(R.string.daily_item_start), item.schedule.startAtEpochMilli?.formatShortDateTime() ?: "-")
                DetailLine(stringResource(R.string.daily_item_end), item.schedule.endAtEpochMilli?.formatShortDateTime() ?: "-")
                DetailLine(stringResource(R.string.daily_item_due), item.schedule.dueAtEpochMilli?.formatShortDateTime() ?: "-")
                DetailLine(stringResource(R.string.all_day), if (item.schedule.allDay) yesLabel else noLabel)
                DetailLine(stringResource(R.string.daily_item_timezone), item.schedule.timeZoneId)
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine(
                    stringResource(R.string.daily_item_calendar_sync),
                    if (item.calendarSync.enabled) enabledLabel else disabledLabel
                )
                DetailLine(
                    stringResource(R.string.daily_item_sync_state),
                    stringResource(item.calendarSync.state.labelRes())
                )
                DetailLine(
                    stringResource(R.string.daily_item_system_event),
                    item.calendarSync.systemEventId?.toString() ?: "-"
                )
                item.calendarSync.lastError?.let {
                    DetailLine(stringResource(R.string.daily_item_last_error), it)
                }
                if (item.calendarSync.enabled && !writeCalendarPermissionState.isGranted) {
                    Text(
                        text = stringResource(R.string.daily_item_sync_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (writeCalendarPermissionState.isGranted) {
                                onSyncNow()
                            } else {
                                writeCalendarPermissionState.launchRequest()
                            }
                        },
                        enabled = item.calendarSync.enabled
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text(
                            if (writeCalendarPermissionState.isGranted) {
                                stringResource(R.string.daily_item_sync)
                            } else {
                                stringResource(R.string.grant_permission)
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = onDisableSync,
                        enabled = item.calendarSync.enabled
                    ) {
                        Text(stringResource(R.string.daily_item_disable_sync))
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
