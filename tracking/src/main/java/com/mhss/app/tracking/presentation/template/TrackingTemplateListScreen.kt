package com.mhss.app.tracking.presentation.template

import android.text.format.DateFormat
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import com.mhss.app.ui.R as UiR

const val TRACKING_TEMPLATE_LIST_TAG = "tracking-template-list"
const val TRACKING_TEMPLATE_CREATE_TAG = "tracking-template-create"
const val TRACKING_TEMPLATE_NAME_TAG = "tracking-template-name"
const val TRACKING_TEMPLATE_CREATE_CONFIRM_TAG = "tracking-template-create-confirm"

fun trackingTemplateCardTag(id: String) = "tracking-template-card-$id"
fun trackingTemplateMenuTag(id: String) = "tracking-template-menu-$id"
fun trackingTemplatePinTag(id: String) = "tracking-template-pin-$id"
fun trackingTemplateDuplicateTag(id: String) = "tracking-template-duplicate-$id"
fun trackingTemplateEditTag(id: String) = "tracking-template-edit-$id"
fun trackingTemplateDeactivateTag(id: String) = "tracking-template-deactivate-$id"

@Composable
fun TrackingTemplateListScreen(
    onBack: () -> Unit,
    onTemplateClick: (String) -> Unit = {},
    onEditTemplate: (String) -> Unit = {},
    onCreateTemplate: (() -> Unit)? = null,
    viewModel: TrackingTemplateListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            val message = when (event) {
                TrackingTemplateListEvent.Created -> R.string.tracking_template_created
                TrackingTemplateListEvent.Duplicated -> R.string.tracking_template_duplicated
                TrackingTemplateListEvent.Deactivated -> R.string.tracking_template_deactivated
                TrackingTemplateListEvent.Failed -> R.string.tracking_template_operation_failed
            }
            snackbarHostState.showSnackbar(resources.getString(message))
        }
    }

    TrackingTemplateListContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTemplateClick = onTemplateClick,
        onEditTemplate = onEditTemplate,
        onCreateTemplate = onCreateTemplate,
        onCreate = viewModel::create,
        onPin = viewModel::togglePinned,
        onDuplicate = viewModel::duplicate,
        onMoveUp = { viewModel.move(it, -1) },
        onMoveDown = { viewModel.move(it, 1) },
        onDeactivate = viewModel::deactivate
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingTemplateListContent(
    state: TrackingTemplateListUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onTemplateClick: (String) -> Unit = {},
    onEditTemplate: (String) -> Unit = {},
    onCreateTemplate: (() -> Unit)? = null,
    onCreate: (String) -> Unit = {},
    onPin: (TrackingTemplateSummary) -> Unit = {},
    onDuplicate: (String) -> Unit = {},
    onMoveUp: (String) -> Unit = {},
    onMoveDown: (String) -> Unit = {},
    onDeactivate: (String) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var deactivateTarget by remember { mutableStateOf<TrackingTemplateSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_templates_title)) },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (onCreateTemplate != null) {
                        onCreateTemplate()
                    } else {
                        showCreateDialog = true
                    }
                },
                modifier = Modifier.testTag(TRACKING_TEMPLATE_CREATE_TAG)
            ) {
                Icon(
                    painterResource(UiR.drawable.ic_add),
                    contentDescription = stringResource(R.string.tracking_create_template)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.templates.isEmpty()) {
                EmptyTemplateList()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TRACKING_TEMPLATE_LIST_TAG),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = state.templates,
                        key = { _, template -> template.id }
                    ) { index, template ->
                        val previous = state.templates.getOrNull(index - 1)
                        val next = state.templates.getOrNull(index + 1)
                        TrackingTemplateCard(
                            template = template,
                            canMoveUp = previous?.isPinned == template.isPinned,
                            canMoveDown = next?.isPinned == template.isPinned,
                            onClick = { onTemplateClick(template.id) },
                            onEdit = { onEditTemplate(template.id) },
                            onPin = { onPin(template) },
                            onDuplicate = { onDuplicate(template.id) },
                            onMoveUp = { onMoveUp(template.id) },
                            onMoveDown = { onMoveDown(template.id) },
                            onDeactivate = { deactivateTarget = template }
                        )
                    }
                }
            }

            if (state.isWorking) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                onCreate(name)
            }
        )
    }

    deactivateTarget?.let { template ->
        AlertDialog(
            onDismissRequest = { deactivateTarget = null },
            title = { Text(stringResource(R.string.tracking_deactivate_template_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.tracking_deactivate_template_message,
                        template.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deactivateTarget = null
                        onDeactivate(template.id)
                    }
                ) {
                    Text(stringResource(R.string.tracking_deactivate))
                }
            },
            dismissButton = {
                TextButton(onClick = { deactivateTarget = null }) {
                    Text(stringResource(R.string.tracking_cancel))
                }
            }
        )
    }
}

@Composable
private fun TrackingTemplateCard(
    template: TrackingTemplateSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDeactivate: () -> Unit
) {
    var menuExpanded by rememberSaveable(template.id) { mutableStateOf(false) }
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(trackingTemplateCardTag(template.id))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color(template.color).copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = template.name.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (template.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.tracking_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(16.dp)
                        )
                    }
                }
                if (template.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.tracking_field_count,
                        template.fields.size,
                        template.fields.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = template.lastRecordedAtEpochMilli?.let { timestamp ->
                        stringResource(
                            R.string.tracking_last_recorded,
                            DateFormat.getMediumDateFormat(context)
                                .format(java.util.Date(timestamp))
                        )
                    } ?: stringResource(R.string.tracking_never_recorded),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag(trackingTemplateMenuTag(template.id))
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.tracking_template_actions)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tracking_edit_template)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        modifier = Modifier.testTag(trackingTemplateEditTag(template.id))
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (template.isPinned) {
                                        R.string.tracking_unpin
                                    } else {
                                        R.string.tracking_pin
                                    }
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onPin()
                        },
                        modifier = Modifier.testTag(trackingTemplatePinTag(template.id))
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tracking_duplicate)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        },
                        modifier = Modifier.testTag(trackingTemplateDuplicateTag(template.id))
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tracking_move_up)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                        },
                        enabled = canMoveUp,
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tracking_move_down)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        },
                        enabled = canMoveDown,
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tracking_deactivate)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDeactivate()
                        },
                        modifier = Modifier.testTag(trackingTemplateDeactivateTag(template.id))
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTemplateList() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.tracking_no_templates),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracking_create_template)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.tracking_template_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRACKING_TEMPLATE_NAME_TAG)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(TRACKING_TEMPLATE_CREATE_CONFIRM_TAG)
            ) {
                Text(stringResource(R.string.tracking_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tracking_cancel))
            }
        }
    )
}
