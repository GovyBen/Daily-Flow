package com.mhss.app.daily.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.daily.domain.model.DailyItemRange
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.ui.components.common.DateDialog
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDate as KxLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyItemsScreen(
    onBack: (() -> Unit)? = null,
    onOpenItem: (String) -> Unit,
    onCreateItem: () -> Unit,
    viewModel: DailyItemsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var customStartAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var customPickerTarget by rememberSaveable { mutableStateOf<CustomRangeTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Items") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setMode(DailyItemsMode.LIST) }) {
                        Icon(Icons.Rounded.List, contentDescription = "List")
                    }
                    IconButton(onClick = { viewModel.setMode(DailyItemsMode.MONTH) }) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = "Month")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateItem) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Daily Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setQuery(it)
                },
                singleLine = true,
                placeholder = { Text("Search items") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            DailyItemRangeChips(
                selected = state.filter.range,
                onSelected = viewModel::setRange,
                onCustomSelected = {
                    customPickerTarget = CustomRangeTarget.START
                }
            )
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.mode == DailyItemsMode.MONTH) {
                DailyItemsMonthView(
                    items = state.items,
                    onOpenItem = onOpenItem
                )
            } else {
                DailyItemsList(
                    items = state.items,
                    onOpenItem = onOpenItem,
                    onToggle = viewModel::toggleCompleted
                )
            }
        }
    }

    customPickerTarget?.let { target ->
        key(target) {
            DateDialog(
                initialDate = when (target) {
                    CustomRangeTarget.START -> customStartAt ?: System.currentTimeMillis()
                    CustomRangeTarget.END -> customStartAt ?: System.currentTimeMillis()
                },
                onDismissRequest = { customPickerTarget = null },
                onDatePicked = { picked ->
                    when (target) {
                        CustomRangeTarget.START -> {
                            customStartAt = picked
                            customPickerTarget = CustomRangeTarget.END
                        }
                        CustomRangeTarget.END -> {
                            val start = customStartAt ?: picked
                            viewModel.setCustomRange(
                                startInclusive = start.toKotlinLocalDate(),
                                endInclusive = picked.toKotlinLocalDate()
                            )
                            customPickerTarget = null
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DailyItemRangeChips(
    selected: DailyItemRange,
    onSelected: (DailyItemRange) -> Unit,
    onCustomSelected: () -> Unit
) {
    val ranges = listOf(
        "Today" to DailyItemRange.Today,
        "+/- 7" to DailyItemRange.SurroundingSevenDays,
        "Next 7" to DailyItemRange.FutureSevenDays,
        "Week" to DailyItemRange.ThisWeek,
        "Month" to DailyItemRange.ThisMonth,
        "Overdue" to DailyItemRange.Overdue,
        "No date" to DailyItemRange.NoDate,
        "Done" to DailyItemRange.Completed,
        "All" to DailyItemRange.All
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ranges.forEach { (label, range) ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelected(range) },
                label = { Text(label) }
            )
        }
        FilterChip(
            selected = selected is DailyItemRange.Custom,
            onClick = onCustomSelected,
            label = { Text("Custom") }
        )
    }
}

@Composable
fun DailyItemsList(
    items: List<DailyItem>,
    onOpenItem: (String) -> Unit,
    onToggle: (DailyItem) -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp)
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Daily Items")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            DailyItemRow(
                item = item,
                onOpenItem = onOpenItem,
                onToggle = onToggle,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
fun DailyItemRow(
    item: DailyItem,
    onOpenItem: (String) -> Unit,
    onToggle: (DailyItem) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpenItem(item.id) }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = if (item.isCompleted) "Reopen" else "Complete",
                tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else item.priorityColor(),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onToggle(item) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    text = item.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = item.priorityColor().copy(alpha = 0.16f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    item.priority.name.lowercase().replaceFirstChar(Char::uppercase),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = item.priorityColor()
                )
            }
        }
    }
}

@Composable
private fun DailyItemsMonthView(
    items: List<DailyItem>,
    onOpenItem: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val month = remember { YearMonth.now() }
    val dates = remember(month) {
        (1..month.lengthOfMonth()).map { month.atDay(it) }
    }
    val grouped = remember(items) { items.groupBy { it.localDateOrNull() } }

    Column(Modifier.fillMaxSize()) {
        Text(
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(dates, key = { it.toString() }) { date ->
                val count = grouped[date].orEmpty().size
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (date == selectedDate) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable { selectedDate = date }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge)
                        if (count > 0) {
                            Text("$count", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Text(
            selectedDate.format(DateTimeFormatter.ofPattern("MMM d")),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        DailyItemsList(
            items = grouped[selectedDate].orEmpty(),
            onOpenItem = onOpenItem,
            onToggle = {},
            contentPadding = PaddingValues(bottom = 96.dp)
        )
    }
}

fun DailyItem.subtitle(): String {
    val time = primaryTimeEpochMilli?.formatShortDateTime() ?: "No date"
    val sync = if (calendarSync.enabled) " | ${calendarSync.state.name.lowercase()}" else ""
    return "${kind.name.lowercase().replaceFirstChar(Char::uppercase)} | $time$sync"
}

fun DailyItem.localDateOrNull(): LocalDate? {
    val millis = primaryTimeEpochMilli ?: return null
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Long.formatShortDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
}

private fun Long.toKotlinLocalDate(): KxLocalDate {
    val date = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return KxLocalDate(date.year, date.monthValue, date.dayOfMonth)
}

private enum class CustomRangeTarget {
    START,
    END
}

@Composable
fun DailyItem.priorityColor(): Color = when (priority) {
    DailyItemPriority.LOW -> MaterialTheme.colorScheme.primary
    DailyItemPriority.MEDIUM -> Color(0xFFE69A00)
    DailyItemPriority.HIGH -> MaterialTheme.colorScheme.error
}
