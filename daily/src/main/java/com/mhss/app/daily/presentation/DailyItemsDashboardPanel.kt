package com.mhss.app.daily.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mhss.app.daily.domain.model.DailyItemFilter
import com.mhss.app.daily.domain.model.DailyItemRange
import com.mhss.app.daily.domain.model.DailyItemStatusFilter
import com.mhss.app.daily.domain.model.DailyItemsPanelConfig
import com.mhss.app.daily.domain.model.toDailyItemRange
import com.mhss.app.daily.domain.usecase.ObserveDailyItemsUseCase
import com.mhss.app.ui.R
import org.koin.compose.koinInject

@Composable
fun DailyItemsDashboardPanel(
    title: String,
    config: DailyItemsPanelConfig,
    onOpenItems: () -> Unit,
    onOpenItem: (String) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    observeDailyItems: ObserveDailyItemsUseCase = koinInject()
) {
    val range = config.range.toDailyItemRange()
    val status = if (range == DailyItemRange.Completed || config.showCompleted) {
        DailyItemStatusFilter.ActiveAndCompleted
    } else {
        DailyItemStatusFilter.Active
    }
    val items by observeDailyItems(
        DailyItemFilter(
            range = range,
            status = status,
            includeCompleted = range == DailyItemRange.Completed || config.showCompleted
        )
    ).collectAsState(initial = emptyList())
    val addContentDescription = stringResource(R.string.daily_item_add)
    val noItemsLabel = stringResource(R.string.daily_items_empty_short)
    val noDateLabel = stringResource(R.string.daily_item_no_date)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpenItems)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddItem) {
                    Icon(Icons.Rounded.Add, contentDescription = addContentDescription)
                }
            }
            val visibleItems = items.take(config.maxItems.coerceIn(1, 20))
            if (visibleItems.isEmpty()) {
                Text(noItemsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visibleItems.forEach { item ->
                    val kindLabel = stringResource(item.kind.labelRes())
                    val syncStateLabel = if (item.calendarSync.enabled) {
                        stringResource(item.calendarSync.state.labelRes())
                    } else {
                        null
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenItem(item.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = item.priorityColor()
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                item.subtitle(
                                    kindLabel = kindLabel,
                                    noDateLabel = noDateLabel,
                                    syncStateLabel = syncStateLabel
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
