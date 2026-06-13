package com.mhss.app.tracking.presentation.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.CreateTemplateUseCase
import com.mhss.app.tracking.domain.usecase.DeactivateTemplateUseCase
import com.mhss.app.tracking.domain.usecase.DuplicateTemplateUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.tracking.domain.usecase.ReorderTemplatesUseCase
import com.mhss.app.tracking.domain.usecase.SetTemplatePinnedUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TrackingTemplateListViewModel(
    observeTemplates: ObserveTemplatesUseCase,
    private val createTemplate: CreateTemplateUseCase,
    private val duplicateTemplate: DuplicateTemplateUseCase,
    private val reorderTemplates: ReorderTemplatesUseCase,
    private val setTemplatePinned: SetTemplatePinnedUseCase,
    private val deactivateTemplate: DeactivateTemplateUseCase
) : ViewModel() {

    private val isWorking = MutableStateFlow(false)
    val events = MutableSharedFlow<TrackingTemplateListEvent>(extraBufferCapacity = 1)

    val uiState = combine(
        observeTemplates(),
        isWorking
    ) { templates, working ->
        TrackingTemplateListUiState(
            templates = templates,
            isWorking = working
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingTemplateListUiState()
    )

    fun create(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        runOperation(TrackingTemplateListEvent.Created) {
            createTemplate(
                draft = TrackingTemplateDraft(
                    name = trimmedName,
                    color = DEFAULT_TEMPLATE_COLOR,
                    displayOrder = uiState.value.templates.size,
                    fields = emptyList()
                ),
                nowEpochMilli = System.currentTimeMillis()
            )
        }
    }

    fun duplicate(templateId: String) {
        runOperation(TrackingTemplateListEvent.Duplicated) {
            duplicateTemplate(templateId, System.currentTimeMillis())
        }
    }

    fun togglePinned(template: TrackingTemplateSummary) {
        runOperation() {
            setTemplatePinned(
                templateId = template.id,
                isPinned = !template.isPinned,
                nowEpochMilli = System.currentTimeMillis()
            )
        }
    }

    fun move(templateId: String, direction: Int) {
        if (direction !in setOf(-1, 1)) return
        val current = uiState.value.templates
        val index = current.indexOfFirst { it.id == templateId }
        if (index < 0) return
        val targetIndex = index + direction
        if (targetIndex !in current.indices) return
        if (current[index].isPinned != current[targetIndex].isPinned) return

        val reordered = current.toMutableList().apply {
            val template = removeAt(index)
            add(targetIndex, template)
        }
        runOperation {
            reorderTemplates(
                reordered.map(TrackingTemplateSummary::id),
                System.currentTimeMillis()
            )
        }
    }

    fun deactivate(templateId: String) {
        runOperation(TrackingTemplateListEvent.Deactivated) {
            deactivateTemplate(templateId, System.currentTimeMillis())
        }
    }

    private fun runOperation(
        successEvent: TrackingTemplateListEvent? = null,
        action: suspend () -> Unit
    ) {
        if (isWorking.value) return
        viewModelScope.launch {
            isWorking.value = true
            runCatching { action() }
                .onSuccess {
                    if (successEvent != null) events.emit(successEvent)
                }
                .onFailure {
                    events.emit(TrackingTemplateListEvent.Failed)
                }
            isWorking.value = false
        }
    }

    private companion object {
        const val DEFAULT_TEMPLATE_COLOR = 0xFF4F6BED
    }
}

data class TrackingTemplateListUiState(
    val templates: List<TrackingTemplateSummary> = emptyList(),
    val isWorking: Boolean = false
)

sealed interface TrackingTemplateListEvent {
    data object Created : TrackingTemplateListEvent
    data object Duplicated : TrackingTemplateListEvent
    data object Deactivated : TrackingTemplateListEvent
    data object Failed : TrackingTemplateListEvent
}
