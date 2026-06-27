package com.mhss.app.daily.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhss.app.daily.domain.model.DailyItemRangePreset
import com.mhss.app.daily.domain.model.DailyItemsPanelConfig
import com.mhss.app.daily.domain.model.DashboardPanel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardEditScreen(
    onBack: () -> Unit,
    viewModel: DashboardEditViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::reset) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.panels, key = { _, panel -> panel.id }) { index, panel ->
                DashboardPanelEditRow(
                    panel = panel,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.panels.lastIndex,
                    onEnabled = { viewModel.setEnabled(panel, it) },
                    onMove = { viewModel.move(panel, it) },
                    onUpdateDailyItemsConfig = {
                        viewModel.updateDailyItemsConfig(panel, it)
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun DashboardPanelEditRow(
    panel: DashboardPanel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEnabled: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onUpdateDailyItemsConfig: (DailyItemsPanelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        panel.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Size: ${panel.size.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down")
                }
                Switch(checked = panel.enabled, onCheckedChange = onEnabled)
            }
            val config = panel.config
            if (config is DailyItemsPanelConfig) {
                DailyItemsPanelConfigEditor(
                    config = config,
                    onConfig = onUpdateDailyItemsConfig
                )
            }
        }
    }
}

@Composable
private fun DailyItemsPanelConfigEditor(
    config: DailyItemsPanelConfig,
    onConfig: (DailyItemsPanelConfig) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Range: ${config.range.name.replace('_', ' ')}",
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DailyItemRangePreset.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.name.replace('_', ' ')) },
                    onClick = {
                        expanded = false
                        onConfig(config.copy(range = range))
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onConfig(config.copy(maxItems = (config.maxItems - 1).coerceAtLeast(1))) }) {
                Text("-")
            }
            Text(config.maxItems.toString())
            TextButton(onClick = { onConfig(config.copy(maxItems = (config.maxItems + 1).coerceAtMost(20))) }) {
                Text("+")
            }
        }
    }
}
