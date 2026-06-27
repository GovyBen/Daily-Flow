package com.mhss.app.mybrain.presentation.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Note
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mhss.app.ui.R
import com.mhss.app.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavHostController,
    viewModel: GlobalSearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val searchHint = stringResource(R.string.search)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(query) { viewModel.onQueryChange(query) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(searchHint) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
            )

            AnimatedVisibility(visible = state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            if (!state.isLoading && query.isNotBlank() && state.query.isNotBlank()) {
                if (state.results.isEmpty()) {
                    EmptySearchState(
                        modifier = Modifier.fillMaxSize(),
                        query = state.query
                    )
                } else {
                    SearchResultsList(
                        results = state.results,
                        navController = navController
                    )
                }
            } else if (!state.isLoading && query.isBlank()) {
                SearchSuggestions(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestions(modifier: Modifier = Modifier) {
    val suggestionsTitle = stringResource(R.string.search_suggestions_title)
    val suggestions = listOf(
        stringResource(R.string.search_suggestion_1),
        stringResource(R.string.search_suggestion_2),
        stringResource(R.string.search_suggestion_3)
    )
    Column(modifier = modifier) {
        Text(
            suggestionsTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        suggestions.forEach { suggestion ->
            Text(
                suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier, query: String) {
    val noResultsText = stringResource(R.string.no_search_results, query)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            noResultsText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    navController: NavHostController
) {
    val grouped = remember(results) { results.groupBy { it.type } }
    val typeOrder = listOf(
        SearchResultType.DAILY_ITEMS,
        SearchResultType.TASKS,
        SearchResultType.EVENTS,
        SearchResultType.RECORDS,
        SearchResultType.DIARY,
        SearchResultType.NOTES
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        typeOrder.forEach { type ->
            val typeResults = grouped[type] ?: return@forEach
            item(key = "header_${type.name}") {
                SearchResultTypeHeader(type = type)
            }
            items(typeResults, key = { it.id }) { result ->
                SearchResultItem(
                    result = result,
                    onClick = {
                        when (result) {
                            is SearchResult.DailyItemResult ->
                                navController.navigate(Screen.DailyItemDetailsScreen(result.itemId))
                            is SearchResult.TaskResult ->
                                navController.navigate(Screen.TaskDetailScreen(result.taskId))
                            is SearchResult.EventResult ->
                                navController.navigate(
                                    Screen.CalendarEventDetailsScreen(
                                        eventId = result.eventId
                                    )
                                )
                            is SearchResult.DiaryResult ->
                                navController.navigate(
                                    Screen.DiaryDetailScreen(entryId = result.entryId)
                                )
                            is SearchResult.NoteResult ->
                                navController.navigate(
                                    Screen.NoteDetailsScreen(
                                        noteId = result.noteId,
                                        folderId = result.folderId
                                    )
                                )
                            is SearchResult.TrackingResult ->
                                navController.navigate(
                                    Screen.TrackingHistoryScreen(
                                        templateId = result.templateId
                                    )
                                )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchResultTypeHeader(type: SearchResultType) {
    val label = when (type) {
        SearchResultType.DAILY_ITEMS -> stringResource(R.string.daily_items)
        SearchResultType.TASKS -> stringResource(R.string.tasks)
        SearchResultType.EVENTS -> stringResource(R.string.events)
        SearchResultType.DIARY -> stringResource(R.string.diary)
        SearchResultType.NOTES -> stringResource(R.string.notes)
        SearchResultType.RECORDS -> stringResource(R.string.records)
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    val icon = result.type.toIcon()
    val iconDescription = result.type.toIconDescription()
    val typeLabel = when (result.type) {
        SearchResultType.DAILY_ITEMS -> stringResource(R.string.daily_items)
        SearchResultType.TASKS -> stringResource(R.string.task)
        SearchResultType.EVENTS -> stringResource(R.string.event)
        SearchResultType.DIARY -> stringResource(R.string.diary)
        SearchResultType.NOTES -> stringResource(R.string.note)
        SearchResultType.RECORDS -> stringResource(R.string.record)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = iconDescription,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.title.ifBlank { typeLabel },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                result.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun SearchResultType.toIcon(): ImageVector = when (this) {
    SearchResultType.DAILY_ITEMS -> Icons.Rounded.CheckCircle
    SearchResultType.TASKS -> Icons.Rounded.CheckCircle
    SearchResultType.EVENTS -> Icons.Rounded.CalendarMonth
    SearchResultType.DIARY -> Icons.Rounded.MenuBook
    SearchResultType.NOTES -> Icons.Rounded.Note
    SearchResultType.RECORDS -> Icons.Rounded.MonitorHeart
}

private fun SearchResultType.toIconDescription(): String = when (this) {
    SearchResultType.DAILY_ITEMS -> "Daily Item"
    SearchResultType.TASKS -> "Task"
    SearchResultType.EVENTS -> "Event"
    SearchResultType.DIARY -> "Diary"
    SearchResultType.NOTES -> "Note"
    SearchResultType.RECORDS -> "Record"
}
