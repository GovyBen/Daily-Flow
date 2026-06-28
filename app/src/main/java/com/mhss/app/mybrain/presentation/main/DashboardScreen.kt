package com.mhss.app.mybrain.presentation.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mhss.app.daily.domain.model.DailyItemRangePreset
import com.mhss.app.daily.domain.model.DailyItemsPanelConfig
import com.mhss.app.daily.domain.model.DashboardPanelType
import com.mhss.app.daily.domain.model.PendingRemindersPanelConfig
import com.mhss.app.daily.presentation.DailyItemsDashboardPanel
import com.mhss.app.presentation.CalendarDashboardWidget
import com.mhss.app.presentation.MoodCircularBar
import com.mhss.app.tracking.presentation.dashboard.TrackingDashboardSection
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.MyBrainAppBar
import com.mhss.app.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel


@Composable
fun DashboardScreen(
    navController: NavHostController,
    onCalendarClick: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val searchContentDescription = stringResource(R.string.search)
    Scaffold(
        topBar = {
            MyBrainAppBar(
                title = stringResource(R.string.dashboard),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.DashboardEditScreen) }) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.dashboard_edit)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.GlobalSearch) }) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = searchContentDescription
                        )
                    }
                }
            )
        }
    ) {paddingValues ->
        LaunchedEffect(true) { viewModel.onDashboardEvent(DashboardEvent.InitAll) }
        LazyColumn(contentPadding = paddingValues) {
            val panels = viewModel.uiState.dashboardPanels
                .filter { it.enabled }
                .sortedBy { it.displayOrder }
            if (panels.isEmpty()) {
                item {
                    TrackingDashboardSection(
                        onQuickRecord = { templateId ->
                            navController.navigate(Screen.TrackingQuickRecordScreen(templateId))
                        },
                        onOpenHistory = {
                            navController.navigate(Screen.TrackingHistoryScreen())
                        }
                    )
                }
            }
            items(panels, key = { it.id }) { panel ->
                when (panel.type) {
                    DashboardPanelType.QUICK_RECORD -> {
                        TrackingDashboardSection(
                            onQuickRecord = { templateId ->
                                navController.navigate(Screen.TrackingQuickRecordScreen(templateId))
                            },
                            onOpenHistory = {
                                navController.navigate(Screen.TrackingHistoryScreen())
                            }
                        )
                    }
                    DashboardPanelType.DAILY_ITEMS -> {
                        DailyItemsDashboardPanel(
                            title = stringResource(R.string.daily_items_title),
                            config = panel.config as? DailyItemsPanelConfig
                                ?: DailyItemsPanelConfig(),
                            onOpenItems = {
                                navController.navigate(Screen.DailyItemsScreen)
                            },
                            onOpenItem = {
                                navController.navigate(Screen.DailyItemDetailsScreen(it))
                            },
                            onAddItem = {
                                navController.navigate(Screen.DailyItemEditorScreen())
                            }
                        )
                    }
                    DashboardPanelType.OVERDUE_ITEMS -> {
                        DailyItemsDashboardPanel(
                            title = stringResource(R.string.daily_items_filter_overdue),
                            config = panel.config as? DailyItemsPanelConfig
                                ?: DailyItemsPanelConfig(
                                    range = DailyItemRangePreset.OVERDUE,
                                    maxItems = 4
                                ),
                            onOpenItems = {
                                navController.navigate(Screen.DailyItemsScreen)
                            },
                            onOpenItem = {
                                navController.navigate(Screen.DailyItemDetailsScreen(it))
                            },
                            onAddItem = {
                                navController.navigate(Screen.DailyItemEditorScreen())
                            }
                        )
                    }
                    DashboardPanelType.PENDING_REMINDERS -> {
                        val config = panel.config as? PendingRemindersPanelConfig
                            ?: PendingRemindersPanelConfig()
                        PendingRemindersCard(
                            reminders = viewModel.uiState.pendingReminders
                                .take(config.maxRows.coerceAtLeast(1)),
                            navController = navController
                        )
                    }
                    DashboardPanelType.TRACKING_SUMMARY,
                    DashboardPanelType.TRACKING_TRENDS -> {
                        Row {
                            MoodCircularBar(
                                entries = viewModel.uiState.dashBoardEntries,
                                showPercentage = false,
                                modifier = Modifier.weight(1f, fill = true),
                                onClick = {
                                    navController.navigate(Screen.DiaryChartScreen)
                                }
                            )
                            TasksSummaryCard(
                                modifier = Modifier.weight(1f, fill = true),
                                tasks = viewModel.uiState.summaryTasks
                            )
                        }
                    }
                    DashboardPanelType.POMODORO -> {
                        PomodoroPanel(
                            onClick = { navController.navigate(Screen.PomodoroScreen) }
                        )
                    }
                    DashboardPanelType.AI_ASSISTANT -> {
                        SimpleDashboardPanel(
                            title = stringResource(R.string.assistant),
                            subtitle = stringResource(R.string.dashboard_assistant_subtitle),
                            onClick = { navController.navigate(Screen.AssistantScreen) }
                        )
                    }
                    DashboardPanelType.CALENDAR_SYNC_STATUS -> {
                        SimpleDashboardPanel(
                            title = stringResource(R.string.daily_item_calendar_sync),
                            subtitle = stringResource(R.string.dashboard_calendar_sync_subtitle),
                            onClick = onCalendarClick
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(65.dp)) }
        }
    }
}

@Composable
private fun PomodoroPanel(onClick: () -> Unit) {
    SimpleDashboardPanel(
        title = stringResource(R.string.pomodoro),
        subtitle = stringResource(R.string.dashboard_pomodoro_subtitle),
        onClick = onClick
    )
}

@Composable
private fun SimpleDashboardPanel(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = onClick
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}
