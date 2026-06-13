package com.mhss.app.tracking.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.DeleteRecordSessionUseCase
import com.mhss.app.tracking.domain.usecase.ObserveRecordHistoryUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.tracking.domain.usecase.UpdateRecordSessionUseCase
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TrackingHistoryViewModel(
    observeTemplates: ObserveTemplatesUseCase,
    observeRecordHistory: ObserveRecordHistoryUseCase,
    private val updateRecordSession: UpdateRecordSessionUseCase,
    private val deleteRecordSession: DeleteRecordSessionUseCase
) : ViewModel() {

    private val loaded = MutableStateFlow(false)
    private val requestedTemplateId = MutableStateFlow<String?>(null)
    private val selectedDayEpochMilli = MutableStateFlow(System.currentTimeMillis())
    private val editor = MutableStateFlow<TrackingRecordEditorState?>(null)
    private val pendingDeleteId = MutableStateFlow<String?>(null)
    private val saveInFlight = AtomicBoolean(false)
    private val deleteInFlight = AtomicBoolean(false)

    val events = MutableSharedFlow<TrackingHistoryEvent>(extraBufferCapacity = 1)

    private val templates = observeTemplates().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val selectedTemplate = combine(
        templates,
        requestedTemplateId,
        loaded
    ) { available, requestedId, isLoaded ->
        if (!isLoaded) {
            null
        } else {
            available.firstOrNull { it.id == requestedId } ?: available.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val records = combine(
        selectedTemplate,
        selectedDayEpochMilli
    ) { template, selectedDay ->
        template to trackingDayRange(selectedDay)
    }.flatMapLatest { (template, range) ->
        if (template == null) {
            flowOf(emptyList())
        } else {
            observeRecordHistory(
                template.id,
                range.startInclusive,
                range.endExclusive
            )
        }
    }.map { history -> history.sortedByDescending(TrackingRecordHistory::occurredAtEpochMilli) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val listState = combine(
        templates,
        selectedTemplate,
        selectedDayEpochMilli,
        records,
        pendingDeleteId
    ) { available, selected, day, history, deletingId ->
        TrackingHistoryUiState(
            isLoading = !loaded.value,
            templates = available,
            selectedTemplate = selected,
            selectedDayEpochMilli = day,
            records = history.filterNot { it.id == deletingId },
            pendingDeleteId = deletingId
        )
    }

    val uiState = combine(listState, editor) { list, editorState ->
        list.copy(editor = editorState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingHistoryUiState()
    )

    fun load(initialTemplateId: String?) {
        if (loaded.value) return
        requestedTemplateId.value = initialTemplateId
        loaded.value = true
    }

    fun selectTemplate(templateId: String) {
        if (templateId == requestedTemplateId.value) return
        requestedTemplateId.value = templateId
        editor.value = null
    }

    fun selectDay(epochMilli: Long) {
        selectedDayEpochMilli.value = epochMilli
        editor.value = null
    }

    fun moveDay(days: Long) {
        selectDay(moveTrackingDay(selectedDayEpochMilli.value, days))
    }

    fun beginEdit(sessionId: String) {
        val template = selectedTemplate.value ?: return
        val record = records.value.firstOrNull { it.id == sessionId } ?: return
        editor.value = record.toEditorState(template)
    }

    fun closeEditor() {
        if (editor.value?.isSaving == true) return
        editor.value = null
    }

    fun updateEditorOccurredAt(epochMilli: Long) {
        editor.update { it?.takeUnless(TrackingRecordEditorState::isSaving)?.copy(
            occurredAtEpochMilli = epochMilli
        ) ?: it }
    }

    fun updateEditorNote(note: String) {
        editor.update {
            it?.takeUnless(TrackingRecordEditorState::isSaving)?.copy(note = note) ?: it
        }
    }

    fun updateEditorInput(fieldId: String, input: TrackerInputValue) {
        editor.update { current ->
            if (current == null || current.isSaving || fieldId !in current.inputs) {
                current
            } else {
                current.copy(inputs = current.inputs + (fieldId to input))
            }
        }
    }

    fun saveEditor() {
        if (!saveInFlight.compareAndSet(false, true)) return
        val currentEditor = editor.value
        val template = selectedTemplate.value
        if (
            currentEditor == null ||
            template == null ||
            !currentEditor.hasValidInputs(template)
        ) {
            editor.update { it?.copy(showValidationErrors = true) }
            saveInFlight.set(false)
            return
        }

        editor.value = currentEditor.copy(isSaving = true, showValidationErrors = true)
        viewModelScope.launch {
            runCatching {
                updateRecordSession(
                    currentEditor.toCommand(template),
                    System.currentTimeMillis()
                )
            }.onSuccess {
                editor.value = null
                events.emit(TrackingHistoryEvent.Updated)
            }.onFailure {
                editor.update { it?.copy(isSaving = false) }
                events.emit(TrackingHistoryEvent.OperationFailed)
            }
            saveInFlight.set(false)
        }
    }

    fun requestDelete(sessionId: String) {
        if (pendingDeleteId.value != null) return
        if (records.value.none { it.id == sessionId }) return
        pendingDeleteId.value = sessionId
    }

    fun undoDelete(sessionId: String) {
        pendingDeleteId.compareAndSet(sessionId, null)
    }

    fun commitDelete(sessionId: String) {
        if (pendingDeleteId.value != sessionId) return
        if (!deleteInFlight.compareAndSet(false, true)) return
        viewModelScope.launch {
            runCatching {
                deleteRecordSession(sessionId)
            }.onSuccess {
                pendingDeleteId.compareAndSet(sessionId, null)
                events.emit(TrackingHistoryEvent.Deleted)
            }.onFailure {
                pendingDeleteId.compareAndSet(sessionId, null)
                events.emit(TrackingHistoryEvent.OperationFailed)
            }
            deleteInFlight.set(false)
        }
    }
}

data class TrackingHistoryUiState(
    val isLoading: Boolean = true,
    val templates: List<TrackingTemplateSummary> = emptyList(),
    val selectedTemplate: TrackingTemplateSummary? = null,
    val selectedDayEpochMilli: Long = System.currentTimeMillis(),
    val records: List<TrackingRecordHistory> = emptyList(),
    val pendingDeleteId: String? = null,
    val editor: TrackingRecordEditorState? = null
)

data class TrackingRecordEditorState(
    val sessionId: String,
    val occurredAtEpochMilli: Long,
    val note: String,
    val source: RecordSource,
    val inputs: Map<String, TrackerInputValue>,
    val hasArchivedFields: Boolean = false,
    val showValidationErrors: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface TrackingHistoryEvent {
    data object Updated : TrackingHistoryEvent
    data object Deleted : TrackingHistoryEvent
    data object OperationFailed : TrackingHistoryEvent
}
