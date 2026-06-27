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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhss.app.daily.domain.model.DailyItem
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
                title = { Text("Daily Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.item?.let { item ->
                        IconButton(onClick = { onEdit(item.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = viewModel::archive) {
                            Icon(Icons.Rounded.Archive, contentDescription = "Archive")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
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
                    Text("Daily Item not found")
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
            title = { Text("Delete Daily Item?") },
            text = { Text("This removes the Daily Flow item. Existing provider events are left untouched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
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
            item.description.ifBlank { "No description" },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onToggleComplete, modifier = Modifier.fillMaxWidth()) {
            Text(if (item.isCompleted) "Reopen item" else "Complete item")
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine("Kind", item.kind.name)
                DetailLine("Status", item.status.name)
                DetailLine("Priority", item.priority.name)
                DetailLine("Start", item.schedule.startAtEpochMilli?.formatShortDateTime() ?: "-")
                DetailLine("End", item.schedule.endAtEpochMilli?.formatShortDateTime() ?: "-")
                DetailLine("Due", item.schedule.dueAtEpochMilli?.formatShortDateTime() ?: "-")
                DetailLine("All day", item.schedule.allDay.toString())
                DetailLine("Timezone", item.schedule.timeZoneId)
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine("Calendar sync", if (item.calendarSync.enabled) "Enabled" else "Disabled")
                DetailLine("Sync state", item.calendarSync.state.name)
                DetailLine("System event", item.calendarSync.systemEventId?.toString() ?: "-")
                item.calendarSync.lastError?.let { DetailLine("Last error", it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = item.calendarSync.enabled
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text("Sync")
                    }
                    OutlinedButton(
                        onClick = onDisableSync,
                        enabled = item.calendarSync.enabled
                    ) {
                        Text("Disable")
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
