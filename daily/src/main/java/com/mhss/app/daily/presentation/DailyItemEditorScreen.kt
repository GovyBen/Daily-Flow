package com.mhss.app.daily.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.mhss.app.daily.domain.model.DailyItemKind
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.ui.components.common.DateTimeDialog
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyItemEditorScreen(
    itemId: String?,
    onBack: () -> Unit,
    viewModel: DailyItemEditorViewModel = koinViewModel(
        parameters = { parametersOf(itemId.orEmpty()) }
    )
) {
    val state by viewModel.uiState.collectAsState()
    val item = state.item
    var initialized by rememberSaveable(item?.id) { mutableStateOf(false) }
    var title by rememberSaveable(item?.id) { mutableStateOf("") }
    var description by rememberSaveable(item?.id) { mutableStateOf("") }
    var kind by rememberSaveable(item?.id) { mutableStateOf(DailyItemKind.TASK) }
    var priority by rememberSaveable(item?.id) { mutableStateOf(DailyItemPriority.LOW) }
    var hasTimeBlock by rememberSaveable(item?.id) { mutableStateOf(false) }
    var startAt by rememberSaveable(item?.id) { mutableStateOf<Long?>(null) }
    var endAt by rememberSaveable(item?.id) { mutableStateOf<Long?>(null) }
    var hasDueDate by rememberSaveable(item?.id) { mutableStateOf(false) }
    var dueAt by rememberSaveable(item?.id) { mutableStateOf<Long?>(null) }
    var allDay by rememberSaveable(item?.id) { mutableStateOf(false) }
    var completable by rememberSaveable(item?.id) { mutableStateOf(true) }
    var syncToCalendar by rememberSaveable(item?.id) { mutableStateOf(false) }
    var pickerTarget by rememberSaveable { mutableStateOf<DateTarget?>(null) }

    LaunchedEffect(item, state.isLoading) {
        if (!initialized && !state.isLoading) {
            title = item?.title.orEmpty()
            description = item?.description.orEmpty()
            kind = item?.kind ?: DailyItemKind.TASK
            priority = item?.priority ?: DailyItemPriority.LOW
            startAt = item?.schedule?.startAtEpochMilli
            endAt = item?.schedule?.endAtEpochMilli
            hasTimeBlock = startAt != null
            dueAt = item?.schedule?.dueAtEpochMilli
            hasDueDate = dueAt != null
            allDay = item?.schedule?.allDay ?: false
            completable = item?.isCompletable ?: true
            syncToCalendar = item?.calendarSync?.enabled ?: false
            initialized = true
        }
    }
    LaunchedEffect(hasTimeBlock, hasDueDate) {
        if (!hasTimeBlock && !hasDueDate) syncToCalendar = false
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    val now = System.currentTimeMillis()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "Create Daily Item" else "Edit Daily Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.save(
                        DailyItemEditorDraft(
                            title = title,
                            description = description,
                            kind = kind,
                            priority = priority,
                            startAtEpochMilli = startAt.takeIf { hasTimeBlock },
                            endAtEpochMilli = endAt.takeIf { hasTimeBlock },
                            dueAtEpochMilli = dueAt.takeIf { hasDueDate },
                            allDay = allDay,
                            isCompletable = completable,
                            syncToCalendar = syncToCalendar
                        )
                    )
                }
            ) {
                Icon(Icons.Rounded.Save, contentDescription = "Save")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                isError = state.titleError,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            EnumDropdown("Kind", kind, DailyItemKind.entries) { kind = it }
            PriorityChips(priority = priority, onSelected = { priority = it })
            ToggleRow("Has time block", hasTimeBlock) {
                hasTimeBlock = it
                if (it && startAt == null) {
                    startAt = now
                    endAt = now + 60 * 60 * 1000L
                }
            }
            if (hasTimeBlock) {
                DateRow("Start", startAt ?: now) { pickerTarget = DateTarget.START }
                DateRow("End", endAt ?: ((startAt ?: now) + 60 * 60 * 1000L)) {
                    pickerTarget = DateTarget.END
                }
                ToggleRow("All day", allDay) { allDay = it }
            }
            ToggleRow("Has due date", hasDueDate) {
                hasDueDate = it
                if (it && dueAt == null) dueAt = now
            }
            if (hasDueDate) {
                DateRow("Due", dueAt ?: now) { pickerTarget = DateTarget.DUE }
            }
            ToggleRow("Completable", completable) { completable = it }
            ToggleRow(
                label = "Sync to system calendar",
                checked = syncToCalendar,
                enabled = hasTimeBlock || hasDueDate
            ) { syncToCalendar = it }
            Spacer(Modifier.height(72.dp))
        }
    }

    pickerTarget?.let { target ->
        DateTimeDialog(
            initialDate = when (target) {
                DateTarget.START -> startAt ?: now
                DateTarget.END -> endAt ?: ((startAt ?: now) + 60 * 60 * 1000L)
                DateTarget.DUE -> dueAt ?: now
            },
            onDismissRequest = { pickerTarget = null },
            onDatePicked = { picked ->
                when (target) {
                    DateTarget.START -> {
                        startAt = picked
                        if ((endAt ?: 0L) < picked) endAt = picked + 60 * 60 * 1000L
                    }
                    DateTarget.END -> endAt = maxOf(picked, startAt ?: picked)
                    DateTarget.DUE -> dueAt = picked
                }
                pickerTarget = null
            }
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    value: T,
    entries: List<T>,
    onSelected: (T) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.name)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.name) },
                        onClick = {
                            expanded = false
                            onSelected(entry)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityChips(
    priority: DailyItemPriority,
    onSelected: (DailyItemPriority) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DailyItemPriority.entries.forEach { value ->
            FilterChip(
                selected = priority == value,
                onClick = { onSelected(value) },
                label = { Text(value.name.lowercase().replaceFirstChar(Char::uppercase)) }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(
            checked = checked && enabled,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DateRow(label: String, value: Long, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.formatShortDateTime())
        }
    }
}

private enum class DateTarget {
    START,
    END,
    DUE
}
