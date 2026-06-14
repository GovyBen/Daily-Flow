package com.mhss.app.tracking.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingFieldValue
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.suggestion.TrackingValueSuggestions
import com.mhss.app.tracking.domain.usecase.GetTrackingValueSuggestionsUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.tracking.domain.usecase.SaveRecordSessionUseCase
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackerValueValidator
import com.mhss.app.tracking.domain.validation.emptyInputValue
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TrackingQuickRecordViewModel(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val saveRecordSession: SaveRecordSessionUseCase,
    private val getSuggestions: GetTrackingValueSuggestionsUseCase
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(TrackingQuickRecordUiState())
    val uiState = mutableUiState.asStateFlow()
    val events = MutableSharedFlow<TrackingQuickRecordEvent>(extraBufferCapacity = 1)

    private val saveInFlight = AtomicBoolean(false)
    private var loadedTemplateId: String? = null

    fun load(templateId: String, initialOccurredAtEpochMilli: Long? = null) {
        if (loadedTemplateId != null) return
        loadedTemplateId = templateId
        viewModelScope.launch {
            val template = observeTemplates().first().firstOrNull { it.id == templateId }
            if (template == null) {
                mutableUiState.update { it.copy(isLoading = false, templateMissing = true) }
                return@launch
            }

            val fields = template.fields.associate { field ->
                checkNotNull(field.id) to TrackingQuickRecordFieldState(
                    input = field.tracker.config.trackerType.emptyInputValue()
                )
            }
            mutableUiState.value = TrackingQuickRecordUiState(
                isLoading = false,
                template = template,
                fields = fields,
                occurredAtEpochMilli = initialOccurredAtEpochMilli ?: System.currentTimeMillis()
            )
            loadSuggestions(template)
        }
    }

    fun updateOccurredAt(epochMilli: Long) {
        mutableUiState.update { state ->
            if (state.isSaving) state else state.copy(occurredAtEpochMilli = epochMilli)
        }
    }

    fun updateNote(note: String) {
        mutableUiState.update { state ->
            if (state.isSaving) state else state.copy(note = note)
        }
    }

    fun updateInput(fieldId: String, input: TrackerInputValue) {
        mutableUiState.update { state ->
            if (state.isSaving || fieldId !in state.fields) {
                state
            } else {
                state.copy(
                    fields = state.fields + (
                        fieldId to checkNotNull(state.fields[fieldId]).copy(input = input)
                    )
                )
            }
        }
    }

    fun save() {
        if (!saveInFlight.compareAndSet(false, true)) return
        val state = mutableUiState.value
        val template = state.template
        if (template == null || !state.hasValidInputs()) {
            mutableUiState.update { it.copy(showValidationErrors = true) }
            saveInFlight.set(false)
            return
        }

        mutableUiState.update { it.copy(isSaving = true, showValidationErrors = true) }
        viewModelScope.launch {
            runCatching {
                val command = state.toRecordSessionCommand()
                saveRecordSession(command, System.currentTimeMillis())
            }.onSuccess { sessionId ->
                mutableUiState.update {
                    it.copy(
                        savedResult = TrackingQuickRecordResult(
                            sessionId = sessionId,
                            templateName = template.name,
                            occurredAtEpochMilli = state.occurredAtEpochMilli
                        )
                    )
                }
            }.onFailure {
                events.emit(TrackingQuickRecordEvent.SaveFailed)
            }
            mutableUiState.update { it.copy(isSaving = false) }
            saveInFlight.set(false)
        }
    }

    fun recordAnother() {
        val state = mutableUiState.value
        val resetFields = state.fields.mapValues { (_, fieldState) ->
            fieldState.copy(
                input = fieldState.suggestions.defaultValue
                    ?: fieldState.input.trackerType.emptyInputValue()
            )
        }
        mutableUiState.update {
            it.copy(
                fields = resetFields,
                note = "",
                occurredAtEpochMilli = System.currentTimeMillis(),
                showValidationErrors = false,
                savedResult = null
            )
        }
    }

    private suspend fun loadSuggestions(template: TrackingTemplateSummary) {
        template.fields.forEach { field ->
            val fieldId = checkNotNull(field.id)
            val suggestions = runCatching {
                getSuggestions(field, mutableUiState.value.fields[fieldId]?.input)
            }.getOrDefault(TrackingValueSuggestions())
            mutableUiState.update { state ->
                val current = state.fields[fieldId] ?: return@update state
                val emptyInput = field.tracker.config.trackerType.emptyInputValue()
                val input = if (current.input == emptyInput) {
                    suggestions.defaultValue ?: current.input
                } else {
                    current.input
                }
                state.copy(
                    fields = state.fields + (
                        fieldId to current.copy(input = input, suggestions = suggestions)
                    )
                )
            }
        }
    }
}

data class TrackingQuickRecordUiState(
    val isLoading: Boolean = true,
    val templateMissing: Boolean = false,
    val template: TrackingTemplateSummary? = null,
    val fields: Map<String, TrackingQuickRecordFieldState> = emptyMap(),
    val occurredAtEpochMilli: Long = 0,
    val note: String = "",
    val showValidationErrors: Boolean = false,
    val isSaving: Boolean = false,
    val savedResult: TrackingQuickRecordResult? = null
)

data class TrackingQuickRecordFieldState(
    val input: TrackerInputValue,
    val suggestions: TrackingValueSuggestions = TrackingValueSuggestions()
)

data class TrackingQuickRecordResult(
    val sessionId: String,
    val templateName: String,
    val occurredAtEpochMilli: Long
)

sealed interface TrackingQuickRecordEvent {
    data object SaveFailed : TrackingQuickRecordEvent
}

internal fun TrackingQuickRecordUiState.hasValidInputs(): Boolean {
    val currentTemplate = template ?: return false
    return currentTemplate.fields.all { field ->
        val fieldId = field.id ?: return@all false
        val input = fields[fieldId]?.input ?: return@all false
        TrackerValueValidator.validate(
            config = field.tracker.config,
            input = input,
            required = field.required,
            activeOptionIds = field.activeOptionIds()
        ).isValid
    }
}

internal fun TrackingQuickRecordUiState.toRecordSessionCommand(
    zoneId: ZoneId = ZoneId.systemDefault()
): RecordSessionCommand {
    val currentTemplate = checkNotNull(template)
    val instant = Instant.ofEpochMilli(occurredAtEpochMilli)
    return RecordSessionCommand(
        templateId = currentTemplate.id,
        occurredAtEpochMilli = occurredAtEpochMilli,
        zoneId = zoneId.id,
        utcOffsetSeconds = zoneId.rules.getOffset(instant).totalSeconds,
        note = note.trim().takeIf(String::isNotEmpty),
        values = currentTemplate.fields.map { field ->
            val fieldId = checkNotNull(field.id)
            TrackingFieldValue(
                fieldId = fieldId,
                input = checkNotNull(fields[fieldId]?.input)
            )
        }
    )
}

private fun TrackingFieldDraft.activeOptionIds(): Set<String> = tracker.options
    .filter { it.isActive }
    .mapNotNullTo(mutableSetOf()) { it.id }
