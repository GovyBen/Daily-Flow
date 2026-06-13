package com.mhss.app.tracking.presentation.template

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackerConfig
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.serialization.TrackingTemplateDraftJson
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackingTemplateDraftError
import com.mhss.app.tracking.domain.validation.TrackingTemplateDraftErrorCode
import com.mhss.app.tracking.presentation.components.TrackingFieldInput
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

const val TRACKING_TEMPLATE_EDITOR_TAG = "tracking-template-editor"
const val TRACKING_TEMPLATE_EDITOR_NAME_TAG = "tracking-template-editor-name"
const val TRACKING_TEMPLATE_EDITOR_SAVE_TAG = "tracking-template-editor-save"
const val TRACKING_TEMPLATE_EDITOR_ADD_FIELD_TAG = "tracking-template-editor-add-field"

fun trackingTemplateEditorFieldNameTag(index: Int) = "tracking-template-editor-field-$index-name"
fun trackingTemplateEditorFieldTypeTag(index: Int) = "tracking-template-editor-field-$index-type"
fun trackingTemplateEditorDeleteFieldTag(index: Int) = "tracking-template-editor-field-$index-delete"
fun trackingTemplateEditorAddOptionTag(index: Int) = "tracking-template-editor-field-$index-add-option"

@Composable
fun TrackingTemplateEditorScreen(
    templateId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: TrackingTemplateEditorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var validationErrors by remember { mutableStateOf(emptyList<TrackingTemplateDraftError>()) }

    LaunchedEffect(templateId) {
        viewModel.load(templateId)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TrackingTemplateEditorEvent.Saved -> onSaved(event.templateId)
                is TrackingTemplateEditorEvent.ValidationFailed -> {
                    validationErrors = event.errors
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.tracking_template_fix_errors)
                    )
                }
                TrackingTemplateEditorEvent.IncompatibleType -> {
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.tracking_template_type_history_error)
                    )
                }
                TrackingTemplateEditorEvent.Failed -> {
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.tracking_template_save_failed)
                    )
                }
            }
        }
    }

    TrackingTemplateEditorContent(
        sourceDraft = uiState.sourceDraft,
        isNew = uiState.isNew,
        isLoading = uiState.isLoading,
        notFound = uiState.notFound,
        isSaving = uiState.isSaving,
        validationErrors = validationErrors,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = {
            validationErrors = emptyList()
            viewModel.save(it)
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingTemplateEditorContent(
    sourceDraft: TrackingTemplateDraft?,
    isNew: Boolean = false,
    isLoading: Boolean = false,
    notFound: Boolean = false,
    isSaving: Boolean = false,
    validationErrors: List<TrackingTemplateDraftError> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onSave: (TrackingTemplateDraft) -> Unit = {}
) {
    var draftJson by rememberSaveable(sourceDraft?.id) {
        mutableStateOf(sourceDraft?.let(TrackingTemplateDraftJson::encode).orEmpty())
    }
    var deleteFieldIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(sourceDraft) {
        if (draftJson.isBlank() && sourceDraft != null) {
            draftJson = TrackingTemplateDraftJson.encode(sourceDraft)
        }
    }

    val draft = remember(draftJson) {
        draftJson.takeIf(String::isNotBlank)?.let {
            runCatching { TrackingTemplateDraftJson.decode(it) }.getOrNull()
        }
    }
    fun updateDraft(updated: TrackingTemplateDraft) {
        draftJson = TrackingTemplateDraftJson.encode(updated.normalizeOrder())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isNew) {
                                R.string.tracking_create_template_title
                            } else {
                                R.string.tracking_edit_template_title
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.tracking_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { draft?.let(onSave) },
                        enabled = draft != null && !isSaving,
                        modifier = Modifier.testTag(TRACKING_TEMPLATE_EDITOR_SAVE_TAG)
                    ) {
                        Text(stringResource(R.string.tracking_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                notFound -> EditorMessage(stringResource(R.string.tracking_template_not_found))
                draft == null || isLoading -> EditorMessage(
                    stringResource(R.string.tracking_loading)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(TRACKING_TEMPLATE_EDITOR_TAG),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            TemplateDetailsCard(
                                draft = draft,
                                nameHasError = validationErrors.any {
                                    it.code == TrackingTemplateDraftErrorCode.TEMPLATE_NAME_REQUIRED
                                },
                                onChange = ::updateDraft
                            )
                        }

                        itemsIndexed(
                            items = draft.fields,
                            key = { index, field ->
                                field.id ?: field.trackerId ?: "new-field-$index"
                            }
                        ) { index, field ->
                            FieldEditorCard(
                                index = index,
                                field = field,
                                errors = validationErrors.filter { it.fieldIndex == index },
                                canMoveUp = index > 0,
                                canMoveDown = index < draft.fields.lastIndex,
                                onChange = { updated ->
                                    updateDraft(
                                        draft.copy(
                                            fields = draft.fields.toMutableList().apply {
                                                this[index] = updated
                                            }
                                        )
                                    )
                                },
                                onMove = { direction ->
                                    updateDraft(draft.copy(fields = draft.fields.moved(index, direction)))
                                },
                                onDelete = { deleteFieldIndex = index }
                            )
                        }

                        item {
                            OutlinedButton(
                                onClick = {
                                    updateDraft(
                                        draft.copy(
                                            fields = draft.fields + TrackingFieldDraft(
                                                tracker = TrackingTrackerDraft(
                                                    name = "",
                                                    config = TextConfig()
                                                ),
                                                displayOrder = draft.fields.size
                                            )
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TRACKING_TEMPLATE_EDITOR_ADD_FIELD_TAG)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Text(
                                    stringResource(R.string.tracking_add_field),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        if (draft.fields.isNotEmpty()) {
                            item {
                                PreviewCard(draft.fields)
                            }
                        }
                    }
                }
            }

            if (isSaving) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    deleteFieldIndex?.let { index ->
        val field = draft?.fields?.getOrNull(index)
        if (field != null) {
            val fieldName = field.tracker.name.ifBlank {
                stringResource(R.string.tracking_unnamed_field)
            }
            AlertDialog(
                onDismissRequest = { deleteFieldIndex = null },
                title = { Text(stringResource(R.string.tracking_remove_field_title)) },
                text = {
                    Text(
                        stringResource(
                            if (field.hasRecordedData) {
                                R.string.tracking_remove_field_history_message
                            } else {
                                R.string.tracking_remove_field_message
                            },
                            fieldName
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteFieldIndex = null
                            updateDraft(draft.copy(fields = draft.fields.filterIndexed { i, _ ->
                                i != index
                            }))
                        }
                    ) {
                        Text(stringResource(R.string.tracking_remove))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteFieldIndex = null }) {
                        Text(stringResource(R.string.tracking_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun TemplateDetailsCard(
    draft: TrackingTemplateDraft,
    nameHasError: Boolean,
    onChange: (TrackingTemplateDraft) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.tracking_template_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = { Text(stringResource(R.string.tracking_template_name)) },
                isError = nameHasError,
                supportingText = if (nameHasError) {
                    { Text(stringResource(R.string.tracking_template_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRACKING_TEMPLATE_EDITOR_NAME_TAG)
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onChange(draft.copy(description = it)) },
                label = { Text(stringResource(R.string.tracking_template_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.icon,
                onValueChange = { onChange(draft.copy(icon = it.take(24))) },
                label = { Text(stringResource(R.string.tracking_template_icon)) },
                supportingText = {
                    Text(stringResource(R.string.tracking_template_icon_hint))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.tracking_template_color),
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TEMPLATE_COLORS.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(color), CircleShape)
                            .clickable { onChange(draft.copy(color = color)) }
                            .then(
                                if (draft.color == color) {
                                    Modifier.padding(5.dp)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (draft.color == color) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldEditorCard(
    index: Int,
    field: TrackingFieldDraft,
    errors: List<TrackingTemplateDraftError>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (TrackingFieldDraft) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.tracking_field_number, index + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.tracking_move_up)
                    )
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.tracking_move_down)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(trackingTemplateEditorDeleteFieldTag(index))
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.tracking_remove_field)
                    )
                }
            }

            val nameError = errors.any {
                it.code == TrackingTemplateDraftErrorCode.FIELD_NAME_REQUIRED
            }
            OutlinedTextField(
                value = field.tracker.name,
                onValueChange = {
                    onChange(field.copy(tracker = field.tracker.copy(name = it)))
                },
                label = { Text(stringResource(R.string.tracking_field_name)) },
                isError = nameError,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(trackingTemplateEditorFieldNameTag(index))
            )

            TrackerTypeSelector(
                selected = field.tracker.config.trackerType,
                enabled = !field.hasRecordedData,
                onSelected = { type ->
                    onChange(
                        field.copy(
                            tracker = field.tracker.copy(
                                config = type.defaultConfig(),
                                options = field.tracker.options.ensureOptionsFor(type)
                            )
                        )
                    )
                },
                modifier = Modifier.testTag(trackingTemplateEditorFieldTypeTag(index))
            )
            if (field.hasRecordedData) {
                Text(
                    stringResource(R.string.tracking_field_type_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.tracking_required),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = field.required,
                    onCheckedChange = { onChange(field.copy(required = it)) }
                )
            }

            if (field.tracker.config.trackerType.supportsUnit()) {
                OutlinedTextField(
                    value = field.tracker.unit.orEmpty(),
                    onValueChange = {
                        onChange(
                            field.copy(
                                tracker = field.tracker.copy(unit = it.ifBlank { null })
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.tracking_unit)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TrackerConfigEditor(
                config = field.tracker.config,
                onChange = {
                    onChange(field.copy(tracker = field.tracker.copy(config = it)))
                }
            )

            if (
                field.tracker.config is MultiSelectConfig ||
                field.tracker.config is SingleSelectConfig
            ) {
                OptionsEditor(
                    fieldIndex = index,
                    field = field,
                    onChange = onChange
                )
            }

            errors.distinctBy { it.code }.forEach { error ->
                Text(
                    stringResource(error.code.messageResource()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TrackerTypeSelector(
    selected: TrackerType,
    enabled: Boolean,
    onSelected: (TrackerType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.tracking_field_type_value, selected.displayName())
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TrackerType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName()) },
                    onClick = {
                        expanded = false
                        onSelected(type)
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackerConfigEditor(
    config: TrackerConfig,
    onChange: (TrackerConfig) -> Unit
) {
    when (config) {
        is MultiSelectConfig -> OptionalLongField(
            label = stringResource(R.string.tracking_max_selections),
            value = config.maxSelections?.toLong(),
            onValueChange = { onChange(config.copy(maxSelections = it?.toInt())) }
        )
        SingleSelectConfig -> Unit
        is CounterConfig -> {
            LongField(
                label = stringResource(R.string.tracking_minimum),
                value = config.minimum,
                onValueChange = { onChange(config.copy(minimum = it)) }
            )
            OptionalLongField(
                label = stringResource(R.string.tracking_maximum_optional),
                value = config.maximum,
                onValueChange = { onChange(config.copy(maximum = it)) }
            )
            LongField(
                label = stringResource(R.string.tracking_step),
                value = config.step,
                onValueChange = { onChange(config.copy(step = it)) }
            )
        }
        is ScaleConfig -> {
            DoubleField(
                label = stringResource(R.string.tracking_minimum),
                value = config.minimum,
                onValueChange = { onChange(config.copy(minimum = it)) }
            )
            DoubleField(
                label = stringResource(R.string.tracking_maximum),
                value = config.maximum,
                onValueChange = { onChange(config.copy(maximum = it)) }
            )
            DoubleField(
                label = stringResource(R.string.tracking_step),
                value = config.step,
                onValueChange = { onChange(config.copy(step = it)) }
            )
        }
        is BooleanConfig -> {
            OutlinedTextField(
                value = config.trueLabel,
                onValueChange = { onChange(config.copy(trueLabel = it)) },
                label = { Text(stringResource(R.string.tracking_true_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = config.falseLabel,
                onValueChange = { onChange(config.copy(falseLabel = it)) },
                label = { Text(stringResource(R.string.tracking_false_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        is DurationConfig -> OptionalLongField(
            label = stringResource(R.string.tracking_maximum_seconds_optional),
            value = config.maximumSeconds,
            onValueChange = { onChange(config.copy(maximumSeconds = it)) }
        )
        is NumberConfig -> {
            OptionalDoubleField(
                label = stringResource(R.string.tracking_minimum_optional),
                value = config.minimum,
                onValueChange = { onChange(config.copy(minimum = it)) }
            )
            OptionalDoubleField(
                label = stringResource(R.string.tracking_maximum_optional),
                value = config.maximum,
                onValueChange = { onChange(config.copy(maximum = it)) }
            )
            OptionalDoubleField(
                label = stringResource(R.string.tracking_step_optional),
                value = config.step,
                onValueChange = { onChange(config.copy(step = it)) }
            )
            OptionalLongField(
                label = stringResource(R.string.tracking_decimal_places_optional),
                value = config.decimalPlaces?.toLong(),
                onValueChange = { onChange(config.copy(decimalPlaces = it?.toInt())) }
            )
        }
        is TextConfig -> {
            OptionalLongField(
                label = stringResource(R.string.tracking_maximum_length_optional),
                value = config.maximumLength?.toLong(),
                onValueChange = { onChange(config.copy(maximumLength = it?.toInt())) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.tracking_multiline),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = config.multiline,
                    onCheckedChange = { onChange(config.copy(multiline = it)) }
                )
            }
        }
    }
}

@Composable
private fun OptionsEditor(
    fieldIndex: Int,
    field: TrackingFieldDraft,
    onChange: (TrackingFieldDraft) -> Unit
) {
    val activeOptions = field.tracker.options.withIndex().filter { it.value.isActive }
    Text(
        stringResource(R.string.tracking_options),
        style = MaterialTheme.typography.labelLarge
    )
    activeOptions.forEachIndexed { activeIndex, indexed ->
        val optionIndex = indexed.index
        val option = indexed.value
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = option.label,
                onValueChange = { label ->
                    onChange(
                        field.copy(
                            tracker = field.tracker.copy(
                                options = field.tracker.options.updated(optionIndex) {
                                    it.copy(label = label)
                                }
                            )
                        )
                    )
                },
                label = {
                    Text(stringResource(R.string.tracking_option_number, activeIndex + 1))
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    onChange(
                        field.copy(
                            tracker = field.tracker.copy(
                                options = field.tracker.options.moveActive(optionIndex, -1)
                            )
                        )
                    )
                },
                enabled = activeIndex > 0
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(
                onClick = {
                    onChange(
                        field.copy(
                            tracker = field.tracker.copy(
                                options = field.tracker.options.moveActive(optionIndex, 1)
                            )
                        )
                    )
                },
                enabled = activeIndex < activeOptions.lastIndex
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            }
            IconButton(
                onClick = {
                    val nextOptions = if (option.id?.startsWith(DRAFT_ID_PREFIX) == true) {
                        field.tracker.options.filterIndexed { index, _ -> index != optionIndex }
                    } else {
                        field.tracker.options.updated(optionIndex) {
                            it.copy(isActive = false)
                        }
                    }
                    onChange(
                        field.copy(
                            tracker = field.tracker.copy(options = nextOptions)
                        )
                    )
                }
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
            }
        }
    }
    TextButton(
        onClick = {
            val activeCount = field.tracker.options.count { it.isActive }
            onChange(
                field.copy(
                    tracker = field.tracker.copy(
                        options = field.tracker.options + TrackingOptionDraft(
                            id = "$DRAFT_ID_PREFIX${UUID.randomUUID()}",
                            label = "",
                            displayOrder = activeCount
                        )
                    )
                )
            )
        },
        modifier = Modifier.testTag(trackingTemplateEditorAddOptionTag(fieldIndex))
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Text(
            stringResource(R.string.tracking_add_option),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun PreviewCard(fields: List<TrackingFieldDraft>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.tracking_preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            fields.forEach { field ->
                TrackingFieldInput(
                    field = field,
                    input = field.tracker.config.trackerType.emptyInput(),
                    onInputChange = {},
                    enabled = false,
                    showValidationErrors = false
                )
            }
        }
    }
}

@Composable
private fun EditorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LongField(label: String, value: Long, onValueChange: (Long) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toLongOrNull()?.let(onValueChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OptionalLongField(
    label: String,
    value: Long?,
    onValueChange: (Long?) -> Unit
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            if (text.isBlank()) onValueChange(null) else text.toLongOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DoubleField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    OutlinedTextField(
        value = value.compactString(),
        onValueChange = { it.toDoubleOrNull()?.let(onValueChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OptionalDoubleField(
    label: String,
    value: Double?,
    onValueChange: (Double?) -> Unit
) {
    OutlinedTextField(
        value = value?.compactString().orEmpty(),
        onValueChange = { text ->
            if (text.isBlank()) onValueChange(null) else text.toDoubleOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TrackerType.displayName(): String = stringResource(
    when (this) {
        TrackerType.MULTI_SELECT -> R.string.tracking_type_multi_select
        TrackerType.SINGLE_SELECT -> R.string.tracking_type_single_select
        TrackerType.COUNTER -> R.string.tracking_type_counter
        TrackerType.SCALE -> R.string.tracking_type_scale
        TrackerType.BOOLEAN -> R.string.tracking_type_boolean
        TrackerType.DURATION -> R.string.tracking_type_duration
        TrackerType.NUMBER -> R.string.tracking_type_number
        TrackerType.TEXT -> R.string.tracking_type_text
    }
)

private fun TrackerType.defaultConfig(): TrackerConfig = when (this) {
    TrackerType.MULTI_SELECT -> MultiSelectConfig()
    TrackerType.SINGLE_SELECT -> SingleSelectConfig
    TrackerType.COUNTER -> CounterConfig()
    TrackerType.SCALE -> ScaleConfig()
    TrackerType.BOOLEAN -> BooleanConfig()
    TrackerType.DURATION -> DurationConfig()
    TrackerType.NUMBER -> NumberConfig()
    TrackerType.TEXT -> TextConfig()
}

private fun TrackerType.supportsUnit() = this in setOf(
    TrackerType.COUNTER,
    TrackerType.SCALE,
    TrackerType.DURATION,
    TrackerType.NUMBER
)

private fun TrackerType.emptyInput(): TrackerInputValue = when (this) {
    TrackerType.MULTI_SELECT -> TrackerInputValue.MultiSelect(emptySet())
    TrackerType.SINGLE_SELECT -> TrackerInputValue.SingleSelect(null)
    TrackerType.COUNTER -> TrackerInputValue.Counter(null)
    TrackerType.SCALE -> TrackerInputValue.Scale(null)
    TrackerType.BOOLEAN -> TrackerInputValue.BooleanValue(null)
    TrackerType.DURATION -> TrackerInputValue.Duration(null)
    TrackerType.NUMBER -> TrackerInputValue.NumberValue(null)
    TrackerType.TEXT -> TrackerInputValue.Text("")
}

private fun TrackingTemplateDraft.normalizeOrder() = copy(
    fields = fields.mapIndexed { index, field ->
        field.copy(
            displayOrder = index,
            tracker = field.tracker.copy(
                options = field.tracker.options.mapIndexed { optionIndex, option ->
                    option.copy(displayOrder = optionIndex)
                }
            )
        )
    }
)

private fun <T> List<T>.moved(index: Int, direction: Int): List<T> {
    val target = index + direction
    if (index !in indices || target !in indices) return this
    return toMutableList().apply {
        val value = removeAt(index)
        add(target, value)
    }
}

private fun <T> List<T>.updated(index: Int, update: (T) -> T): List<T> =
    mapIndexed { currentIndex, value ->
        if (currentIndex == index) update(value) else value
    }

private fun List<TrackingOptionDraft>.moveActive(
    optionIndex: Int,
    direction: Int
): List<TrackingOptionDraft> {
    val active = filter(TrackingOptionDraft::isActive).toMutableList()
    val selected = getOrNull(optionIndex) ?: return this
    val activeIndex = active.indexOfFirst { it.id == selected.id }
    val target = activeIndex + direction
    if (activeIndex !in active.indices || target !in active.indices) return this
    val option = active.removeAt(activeIndex)
    active.add(target, option)
    return (active + filterNot(TrackingOptionDraft::isActive)).mapIndexed { index, value ->
        value.copy(displayOrder = index)
    }
}

private fun List<TrackingOptionDraft>.ensureOptionsFor(
    type: TrackerType
): List<TrackingOptionDraft> {
    if (type !in setOf(TrackerType.MULTI_SELECT, TrackerType.SINGLE_SELECT)) return this
    if (any(TrackingOptionDraft::isActive)) return this
    return this + TrackingOptionDraft(
        id = "$DRAFT_ID_PREFIX${UUID.randomUUID()}",
        label = "",
        displayOrder = size
    )
}

private fun TrackingTemplateDraftErrorCode.messageResource(): Int = when (this) {
    TrackingTemplateDraftErrorCode.TEMPLATE_NAME_REQUIRED ->
        R.string.tracking_template_name_required
    TrackingTemplateDraftErrorCode.FIELD_NAME_REQUIRED ->
        R.string.tracking_field_name_required
    TrackingTemplateDraftErrorCode.SELECT_OPTIONS_REQUIRED ->
        R.string.tracking_select_options_required
    TrackingTemplateDraftErrorCode.OPTION_LABEL_REQUIRED ->
        R.string.tracking_option_label_required
    TrackingTemplateDraftErrorCode.DUPLICATE_OPTION_LABEL ->
        R.string.tracking_duplicate_option_label
    TrackingTemplateDraftErrorCode.INVALID_CONFIGURATION ->
        R.string.tracking_invalid_field_configuration
}

private fun Double.compactString(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

private const val DRAFT_ID_PREFIX = "draft-"

private val TEMPLATE_COLORS = listOf(
    0xFF4F6BED,
    0xFF6750A4,
    0xFF386A20,
    0xFF006A6A,
    0xFFB3261E,
    0xFF7D5260
)
