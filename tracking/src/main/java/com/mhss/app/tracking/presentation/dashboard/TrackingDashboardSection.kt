package com.mhss.app.tracking.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.presentation.analytics.chart.TrackingSparkline
import org.koin.androidx.compose.koinViewModel

const val TRACKING_DASHBOARD_SECTION_TAG = "tracking-dashboard-section"
const val TRACKING_DASHBOARD_HISTORY_TAG = "tracking-dashboard-history"

fun trackingDashboardQuickTemplateTag(id: String) = "tracking-dashboard-quick-$id"

@Composable
fun TrackingDashboardSection(
    onQuickRecord: (String) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: TrackingDashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.loadToday()
    }
    TrackingDashboardContent(
        state = state,
        onQuickRecord = onQuickRecord,
        onOpenHistory = onOpenHistory
    )
}

@Composable
fun TrackingDashboardContent(
    state: TrackingDashboardUiState,
    onQuickRecord: (String) -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(TRACKING_DASHBOARD_SECTION_TAG)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.tracking_dashboard_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.testTag(TRACKING_DASHBOARD_HISTORY_TAG)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null)
                    Text(stringResource(R.string.tracking_history))
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (state.quickTemplates.isEmpty()) {
                Text(
                    stringResource(R.string.tracking_dashboard_no_templates),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.tracking_dashboard_quick_title),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.quickTemplates.forEach { template ->
                        AssistChip(
                            onClick = { onQuickRecord(template.id) },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(18.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = Color(template.color)
                                ) {}
                            },
                            label = { Text(template.name) },
                            modifier = Modifier.testTag(
                                trackingDashboardQuickTemplateTag(template.id)
                            )
                        )
                    }
                }
            }

            if (!state.isLoading) {
                Text(
                    pluralStringResource(
                        R.plurals.tracking_dashboard_today_count,
                        state.totalSessionCount,
                        state.totalSessionCount
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                if (state.todaySummaries.isEmpty()) {
                    Text(
                        stringResource(R.string.tracking_dashboard_no_records),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    state.todaySummaries.forEach { summary ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = Color(summary.color)
                            ) {}
                            Text(summary.name, modifier = Modifier.weight(1f))
                            if (summary.sparklineValues.isNotEmpty()) {
                                TrackingSparkline(
                                    values = summary.sparklineValues,
                                    color = Color(summary.color),
                                    modifier = Modifier.width(48.dp)
                                )
                            }
                            Text(
                                summary.sessionCount.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
