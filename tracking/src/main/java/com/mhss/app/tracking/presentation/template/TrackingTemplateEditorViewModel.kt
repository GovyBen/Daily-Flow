package com.mhss.app.tracking.presentation.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.data.repository.IncompatibleTrackerTypeChangeException
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.CreateTemplateUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.tracking.domain.usecase.UpdateTemplateUseCase
import com.mhss.app.tracking.domain.validation.TrackingTemplateDraftError
import com.mhss.app.tracking.domain.validation.TrackingTemplateDraftValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TrackingTemplateEditorViewModel(
    observeTemplates: ObserveTemplatesUseCase,
    private val createTemplate: CreateTemplateUseCase,
    private val updateTemplate: UpdateTemplateUseCase
) : ViewModel() {

    private val target = MutableStateFlow<EditorTarget>(EditorTarget.Uninitialized)
    private val isSaving = MutableStateFlow(false)
    val events = MutableSharedFlow<TrackingTemplateEditorEvent>(extraBufferCapacity = 1)

    val uiState = combine(
        observeTemplates(),
        target,
        isSaving
    ) { templates, editorTarget, saving ->
        editorTarget.toUiState(templates, saving)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingTemplateEditorUiState()
    )

    fun load(templateId: String?) {
        if (target.value != EditorTarget.Uninitialized) return
        target.value = templateId?.let(EditorTarget::Existing) ?: EditorTarget.New
    }

    fun save(draft: TrackingTemplateDraft) {
        if (isSaving.value) return
        val errors = TrackingTemplateDraftValidator.validate(draft)
        if (errors.isNotEmpty()) {
            events.tryEmit(TrackingTemplateEditorEvent.ValidationFailed(errors))
            return
        }

        val editorTarget = target.value
        if (editorTarget == EditorTarget.Uninitialized) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching {
                val now = System.currentTimeMillis()
                when (editorTarget) {
                    EditorTarget.New -> createTemplate(draft, now)
                    is EditorTarget.Existing -> {
                        updateTemplate(editorTarget.id, draft, now)
                        editorTarget.id
                    }
                    EditorTarget.Uninitialized -> error("Editor target is not initialized")
                }
            }.onSuccess { templateId ->
                events.emit(TrackingTemplateEditorEvent.Saved(templateId))
            }.onFailure { error ->
                events.emit(
                    if (error is IncompatibleTrackerTypeChangeException) {
                        TrackingTemplateEditorEvent.IncompatibleType
                    } else {
                        TrackingTemplateEditorEvent.Failed
                    }
                )
            }
            isSaving.value = false
        }
    }

    private fun EditorTarget.toUiState(
        templates: List<TrackingTemplateSummary>,
        saving: Boolean
    ): TrackingTemplateEditorUiState = when (this) {
        EditorTarget.Uninitialized -> TrackingTemplateEditorUiState(isLoading = true)
        EditorTarget.New -> TrackingTemplateEditorUiState(
            sourceDraft = TrackingTemplateDraft(
                name = "",
                color = DEFAULT_TEMPLATE_COLOR,
                fields = emptyList()
            ),
            isNew = true,
            isSaving = saving
        )
        is EditorTarget.Existing -> {
            val template = templates.firstOrNull { it.id == id }
            TrackingTemplateEditorUiState(
                sourceDraft = template?.toDraft(),
                isLoading = template == null,
                notFound = template == null,
                isSaving = saving
            )
        }
    }

    private companion object {
        const val DEFAULT_TEMPLATE_COLOR = 0xFF4F6BED
    }
}

data class TrackingTemplateEditorUiState(
    val sourceDraft: TrackingTemplateDraft? = null,
    val isNew: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface TrackingTemplateEditorEvent {
    data class Saved(val templateId: String) : TrackingTemplateEditorEvent
    data class ValidationFailed(
        val errors: List<TrackingTemplateDraftError>
    ) : TrackingTemplateEditorEvent
    data object IncompatibleType : TrackingTemplateEditorEvent
    data object Failed : TrackingTemplateEditorEvent
}

private sealed interface EditorTarget {
    data object Uninitialized : EditorTarget
    data object New : EditorTarget
    data class Existing(val id: String) : EditorTarget
}

private fun TrackingTemplateSummary.toDraft() = TrackingTemplateDraft(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    displayOrder = displayOrder,
    fields = fields
)
