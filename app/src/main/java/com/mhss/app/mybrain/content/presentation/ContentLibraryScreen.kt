package com.mhss.app.mybrain.content.presentation

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mhss.app.mybrain.content.domain.ContentItem
import com.mhss.app.mybrain.content.domain.ContentType
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.LiquidFloatingActionButton
import com.mhss.app.ui.components.common.MyBrainAppBar
import com.mhss.app.ui.navigation.Screen
import com.mhss.app.util.date.formatDateDependingOnDay
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContentLibraryScreen(
    navController: NavHostController,
    initialType: ContentType? = null,
    viewModel: ContentLibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val liquidState = rememberLiquidState()
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialType) {
        viewModel.selectType(initialType)
    }

    Scaffold(
        topBar = {
            MyBrainAppBar(stringResource(R.string.content_library))
        },
        floatingActionButton = {
            LiquidFloatingActionButton(
                onClick = { showCreateSheet = true },
                iconPainter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.create_content),
                liquidState = liquidState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .liquefiable(liquidState)
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_content_library)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    }
                },
                singleLine = true
            )
            ContentFilters(
                selectedType = uiState.selectedType,
                onSelected = viewModel::selectType
            )
            if (uiState.items.isEmpty()) {
                EmptyContentLibrary(
                    hasActiveSearch = uiState.query.isNotBlank() ||
                        uiState.selectedType != null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items, key = ContentItem::stableId) { item ->
                        ContentLibraryItem(
                            item = item,
                            onClick = { navController.openContent(item) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateContentSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { type ->
                showCreateSheet = false
                navController.createContent(type)
            }
        )
    }
}

@Composable
private fun ContentFilters(
    selectedType: ContentType?,
    onSelected: (ContentType?) -> Unit
) {
    val filters = remember {
        listOf(null, ContentType.NOTE, ContentType.DIARY, ContentType.LINK)
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onSelected(type) },
                label = {
                    Text(
                        if (type == null) {
                            stringResource(R.string.content_filter_all)
                        } else {
                            stringResource(type.filterLabel())
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ContentLibraryItem(
    item: ContentItem,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.type.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(item.type.typeLabel()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.sortDate > 0L) {
                        Text(
                            text = item.sortDate.formatDateDependingOnDay(context),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.title.ifBlank {
                        stringResource(R.string.untitled_content)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.preview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!item.url.isNullOrBlank() && item.preview != item.url) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContentLibrary(hasActiveSearch: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(
                if (hasActiveSearch) {
                    R.string.no_content_library_results
                } else {
                    R.string.no_content_library_items
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CreateContentSheet(
    onDismiss: () -> Unit,
    onCreate: (ContentType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.create_content),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        ContentType.entries.forEachIndexed { index, type ->
            ListItem(
                headlineContent = {
                    Text(stringResource(type.createLabel()))
                },
                supportingContent = {
                    Text(stringResource(type.typeLabel()))
                },
                leadingContent = {
                    Icon(type.icon(), contentDescription = null)
                },
                modifier = Modifier.clickable { onCreate(type) }
            )
            if (index < ContentType.entries.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun NavHostController.openContent(item: ContentItem) {
    when (item.type) {
        ContentType.NOTE -> navigate(
            Screen.NoteDetailsScreen(
                noteId = item.sourceId,
                folderId = item.folderId
            )
        )
        ContentType.DIARY -> navigate(Screen.DiaryDetailScreen(item.sourceId))
        ContentType.LINK -> navigate(Screen.BookmarkDetailScreen(item.sourceId))
    }
}

private fun NavHostController.createContent(type: ContentType) {
    when (type) {
        ContentType.NOTE -> navigate(Screen.NoteDetailsScreen())
        ContentType.DIARY -> navigate(Screen.DiaryDetailScreen())
        ContentType.LINK -> navigate(Screen.BookmarkDetailScreen())
    }
}

private fun ContentType.filterLabel(): Int = when (this) {
    ContentType.NOTE -> R.string.content_filter_notes
    ContentType.DIARY -> R.string.content_filter_diary
    ContentType.LINK -> R.string.content_filter_links
}

private fun ContentType.typeLabel(): Int = when (this) {
    ContentType.NOTE -> R.string.content_type_note
    ContentType.DIARY -> R.string.content_type_diary
    ContentType.LINK -> R.string.content_type_link
}

private fun ContentType.createLabel(): Int = when (this) {
    ContentType.NOTE -> R.string.add_note
    ContentType.DIARY -> R.string.add_entry
    ContentType.LINK -> R.string.add_bookmark
}

private fun ContentType.icon(): ImageVector = when (this) {
    ContentType.NOTE -> Icons.Outlined.Description
    ContentType.DIARY -> Icons.AutoMirrored.Outlined.MenuBook
    ContentType.LINK -> Icons.Outlined.Link
}
