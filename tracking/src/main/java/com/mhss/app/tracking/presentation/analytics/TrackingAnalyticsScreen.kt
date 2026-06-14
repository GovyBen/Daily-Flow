package com.mhss.app.tracking.presentation.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.model.TrackingDailySummary
import com.mhss.app.tracking.analytics.model.TrackingStreak
import com.mhss.app.tracking.presentation.analytics.chart.TrackingBarChart
import com.mhss.app.tracking.presentation.analytics.chart.TrackingChartValue
import com.mhss.app.tracking.presentation.analytics.chart.TrackingLineChart
import com.mhss.app.tracking.presentation.analytics.chart.TrackingPieChart
import java.text.DecimalFormat
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

const val TRACKING_ANALYTICS_TRACKER_TAG = "tracking-analytics-tracker"
const val TRACKING_ANALYTICS_RANGE_TAG = "tracking-analytics-range"
const val TRACKING_ANALYTICS_AGGREGATION_TAG = "tracking-analytics-aggregation"
const val TRACKING_ANALYTICS_CONTENT_TAG = "tracking-analytics-content"
const val TRACKING_ANALYTICS_RETRY_TAG = "tracking-analytics-retry"

fun trackingAnalyticsTrackerOptionTag(id: String) = "tracking-analytics-tracker-$id"
fun trackingAnalyticsRangeTag(range: TrackingAnalyticsRange) =
    "$TRACKING_ANALYTICS_RANGE_TAG-${range.name.lowercase()}"
fun trackingAnalyticsAggregationTag(operation: AggregationOperation) =
    "$TRACKING_ANALYTICS_AGGREGATION_TAG-${operation.name.lowercase()}"

@Composable
fun TrackingAnalyticsScreen(
    initialTemplateId: String?,
    onBack: () -> Unit,
    viewModel: TrackingAnalyticsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialTemplateId) {
        viewModel.load(initialTemplateId)
    }

    TrackingAnalyticsContent(
        state = state,
        onBack = onBack,
        onTrackerSelected = viewModel::selectTracker,
        onRangeSelected = viewModel::selectRange,
        onAggregationSelected = viewModel::selectAggregation,
        onChartTypeSelected = viewModel::selectChartType,
        onRetry = viewModel::retry
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingAnalyticsContent(
    state: TrackingAnalyticsUiState,
    onBack: () -> Unit = {},
    onTrackerSelected: (String) -> Unit = {},
    onRangeSelected: (TrackingAnalyticsRange) -> Unit = {},
    onAggregationSelected: (AggregationOperation) -> Unit = {},
    onChartTypeSelected: (TrackingAnalyticsChartType) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_analytics_title)) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.trackerOptions.isEmpty() && !state.isLoading) {
                AnalyticsMessage(
                    title = stringResource(R.string.tracking_analytics_no_trackers),
                    message = stringResource(R.string.tracking_analytics_no_trackers_detail)
                )
            } else {
                AnalyticsList(
                    state = state,
                    onTrackerSelected = onTrackerSelected,
                    onRangeSelected = onRangeSelected,
                    onAggregationSelected = onAggregationSelected,
                    onChartTypeSelected = onChartTypeSelected,
                    onRetry = onRetry
                )
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsList(
    state: TrackingAnalyticsUiState,
    onTrackerSelected: (String) -> Unit,
    onRangeSelected: (TrackingAnalyticsRange) -> Unit,
    onAggregationSelected: (AggregationOperation) -> Unit,
    onChartTypeSelected: (TrackingAnalyticsChartType) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TRACKING_ANALYTICS_CONTENT_TAG),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TrackerSelector(
                options = state.trackerOptions,
                selected = state.selectedTracker,
                onSelected = onTrackerSelected
            )
        }
        item {
            RangeSelector(state.range, onRangeSelected)
        }
        item {
            AggregationSelector(state.aggregation, onAggregationSelected)
        }
        if (state.loadFailed) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.tracking_analytics_load_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(TRACKING_ANALYTICS_RETRY_TAG)
                        ) {
                            Text(stringResource(R.string.tracking_analytics_retry))
                        }
                    }
                }
            }
        }
        state.data?.let { data ->
            item {
                DailySummaryCard(data.dailySummary)
            }
            item {
                StreakCards(data.currentStreak, data.longestStreak)
            }
            item {
                ChartTypeSelector(state.chartType, onChartTypeSelected)
            }
            item {
                SeriesCard(state)
            }
            data.distribution?.let { distribution ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.tracking_analytics_distribution),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            TrackingPieChart(
                                values = distribution.items.map {
                                    TrackingChartValue(it.label, it.count.toDouble())
                                },
                                title = stringResource(
                                    R.string.tracking_analytics_distribution
                                ),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerSelector(
    options: List<TrackingAnalyticsTrackerOption>,
    selected: TrackingAnalyticsTrackerOption?,
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.tracking_analytics_tracker),
            style = MaterialTheme.typography.labelLarge
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRACKING_ANALYTICS_TRACKER_TAG)
            ) {
                Text(
                    selected?.displayName
                        ?: stringResource(R.string.tracking_analytics_no_trackers),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            expanded = false
                            onSelected(option.trackerId)
                        },
                        modifier = Modifier.testTag(
                            trackingAnalyticsTrackerOptionTag(option.trackerId)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selected: TrackingAnalyticsRange,
    onSelected: (TrackingAnalyticsRange) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.tracking_analytics_range),
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackingAnalyticsRange.entries.forEach { range ->
                FilterChip(
                    selected = range == selected,
                    onClick = { onSelected(range) },
                    label = { Text(range.displayName()) },
                    modifier = Modifier.testTag(trackingAnalyticsRangeTag(range))
                )
            }
        }
    }
}

@Composable
private fun AggregationSelector(
    selected: AggregationOperation,
    onSelected: (AggregationOperation) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.tracking_analytics_aggregation),
            style = MaterialTheme.typography.labelLarge
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRACKING_ANALYTICS_AGGREGATION_TAG)
            ) {
                Text(
                    selected.displayName(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AggregationOperation.entries.forEach { operation ->
                    DropdownMenuItem(
                        text = { Text(operation.displayName()) },
                        onClick = {
                            expanded = false
                            onSelected(operation)
                        },
                        modifier = Modifier.testTag(
                            trackingAnalyticsAggregationTag(operation)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(summary: TrackingDailySummary?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.tracking_analytics_today_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (summary == null || summary.sampleCount == 0) {
                Text(
                    stringResource(R.string.tracking_analytics_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MetricRow(
                    stringResource(R.string.tracking_analytics_samples),
                    summary.sampleCount.toString()
                )
                MetricRow(
                    stringResource(R.string.tracking_analytics_sum),
                    formatMetric(summary.sum, summary.unit)
                )
                MetricRow(
                    stringResource(R.string.tracking_analytics_average),
                    formatMetric(summary.average, summary.unit)
                )
                MetricRow(
                    stringResource(R.string.tracking_analytics_minimum),
                    formatMetric(summary.minimum, summary.unit)
                )
                MetricRow(
                    stringResource(R.string.tracking_analytics_maximum),
                    formatMetric(summary.maximum, summary.unit)
                )
            }
        }
    }
}

@Composable
private fun StreakCards(
    current: TrackingStreak?,
    longest: TrackingStreak?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StreakCard(
            title = stringResource(R.string.tracking_analytics_current_streak),
            streak = current,
            modifier = Modifier.weight(1f)
        )
        StreakCard(
            title = stringResource(R.string.tracking_analytics_longest_streak),
            streak = longest,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StreakCard(
    title: String,
    streak: TrackingStreak?,
    modifier: Modifier
) {
    ElevatedCard(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(
                    R.string.tracking_analytics_streak_days,
                    streak?.length ?: 0
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChartTypeSelector(
    selected: TrackingAnalyticsChartType,
    onSelected: (TrackingAnalyticsChartType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrackingAnalyticsChartType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(type.displayName()) }
            )
        }
    }
}

@Composable
private fun SeriesCard(state: TrackingAnalyticsUiState) {
    val series = state.data?.series
    val values = series?.points
        ?.filter { it.sampleCount > 0 && it.value != null }
        ?.map { point ->
            TrackingChartValue(
                label = point.startDate.shortLabel(state.range),
                value = requireNotNull(point.value)
            )
        }
        .orEmpty()
    val title = stringResource(
        R.string.tracking_analytics_series_title,
        state.range.displayName(),
        state.aggregation.displayName()
    )

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            when (state.chartType) {
                TrackingAnalyticsChartType.LINE -> TrackingLineChart(
                    values = values,
                    title = title,
                    modifier = Modifier.padding(top = 12.dp)
                )

                TrackingAnalyticsChartType.BAR -> TrackingBarChart(
                    values = values,
                    title = title,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AnalyticsMessage(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val metricFormat = DecimalFormat("0.##")

private fun formatMetric(value: Double?, unit: String?): String {
    if (value == null) return "-"
    return buildString {
        append(metricFormat.format(value))
        if (!unit.isNullOrBlank()) {
            append(' ')
            append(unit)
        }
    }
}

private fun LocalDate.shortLabel(range: TrackingAnalyticsRange): String = when (range) {
    TrackingAnalyticsRange.DAY,
    TrackingAnalyticsRange.WEEK -> "${month.ordinal + 1}/$day"

    TrackingAnalyticsRange.MONTH ->
        "$year-${(month.ordinal + 1).toString().padStart(2, '0')}"
}

@Composable
private fun TrackingAnalyticsRange.displayName(): String = stringResource(
    when (this) {
        TrackingAnalyticsRange.DAY -> R.string.tracking_analytics_range_day
        TrackingAnalyticsRange.WEEK -> R.string.tracking_analytics_range_week
        TrackingAnalyticsRange.MONTH -> R.string.tracking_analytics_range_month
    }
)

@Composable
private fun TrackingAnalyticsChartType.displayName(): String = stringResource(
    when (this) {
        TrackingAnalyticsChartType.LINE -> R.string.tracking_analytics_line_chart
        TrackingAnalyticsChartType.BAR -> R.string.tracking_analytics_bar_chart
    }
)

@Composable
private fun AggregationOperation.displayName(): String = stringResource(
    when (this) {
        AggregationOperation.SUM -> R.string.tracking_analytics_sum
        AggregationOperation.COUNT -> R.string.tracking_analytics_count
        AggregationOperation.AVERAGE -> R.string.tracking_analytics_average
        AggregationOperation.MIN -> R.string.tracking_analytics_minimum
        AggregationOperation.MAX -> R.string.tracking_analytics_maximum
    }
)
