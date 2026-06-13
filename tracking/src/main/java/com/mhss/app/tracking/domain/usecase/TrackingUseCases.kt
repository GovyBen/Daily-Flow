package com.mhss.app.tracking.domain.usecase

import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.repository.TrackingRepository
import com.mhss.app.tracking.domain.suggestion.SuggestedValueHelper
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import org.koin.core.annotation.Factory

@Factory
class CreateTemplateUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(draft: TrackingTemplateDraft, nowEpochMilli: Long) =
        repository.createTemplate(draft, nowEpochMilli)
}

@Factory
class UpdateTemplateUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(
        templateId: String,
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ) = repository.updateTemplate(templateId, draft, nowEpochMilli)
}

@Factory
class DuplicateTemplateUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(templateId: String, nowEpochMilli: Long) =
        repository.duplicateTemplate(templateId, nowEpochMilli)
}

@Factory
class ReorderTemplatesUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(ids: List<String>, nowEpochMilli: Long) =
        repository.reorderTemplates(ids, nowEpochMilli)
}

@Factory
class SetTemplatePinnedUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(
        templateId: String,
        isPinned: Boolean,
        nowEpochMilli: Long
    ) = repository.setTemplatePinned(templateId, isPinned, nowEpochMilli)
}

@Factory
class DeactivateTemplateUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(templateId: String, nowEpochMilli: Long) =
        repository.deactivateTemplate(templateId, nowEpochMilli)
}

@Factory
class SaveRecordSessionUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(command: RecordSessionCommand, nowEpochMilli: Long) =
        repository.saveRecordSession(command, nowEpochMilli)
}

@Factory
class UpdateRecordSessionUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(command: RecordSessionCommand, nowEpochMilli: Long) =
        repository.updateRecordSession(command, nowEpochMilli)
}

@Factory
class DeleteRecordSessionUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(sessionId: String) =
        repository.deleteRecordSession(sessionId)
}

@Factory
class ObserveTemplatesUseCase(private val repository: TrackingRepository) {
    operator fun invoke() = repository.observeTemplates()
}

@Factory
class ObserveRecordHistoryUseCase(private val repository: TrackingRepository) {
    operator fun invoke(templateId: String, startInclusive: Long, endExclusive: Long) =
        repository.observeRecordHistory(templateId, startInclusive, endExclusive)
}

@Factory
class GetSuggestedValuesUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(trackerId: String, limit: Int) =
        repository.getSuggestedValues(trackerId, limit)
}

@Factory
class GetTrackingValueSuggestionsUseCase(
    private val helper: SuggestedValueHelper
) {
    suspend operator fun invoke(
        field: TrackingFieldDraft,
        currentInput: TrackerInputValue? = null,
        limit: Int = SuggestedValueHelper.DEFAULT_LIMIT,
        includeTextHistory: Boolean = false
    ) = helper.getSuggestions(
        field = field,
        currentInput = currentInput,
        limit = limit,
        includeTextHistory = includeTextHistory
    )
}
