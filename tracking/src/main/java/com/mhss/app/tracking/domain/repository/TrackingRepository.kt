package com.mhss.app.tracking.domain.repository

import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingSuggestedValue
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {

    suspend fun createTemplate(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): String

    suspend fun createTemplateIfAbsent(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): Boolean

    suspend fun updateTemplate(
        templateId: String,
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    )

    suspend fun duplicateTemplate(
        templateId: String,
        nowEpochMilli: Long
    ): String

    suspend fun reorderTemplates(
        orderedTemplateIds: List<String>,
        nowEpochMilli: Long
    )

    suspend fun setTemplatePinned(
        templateId: String,
        isPinned: Boolean,
        nowEpochMilli: Long
    )

    suspend fun deactivateTemplate(
        templateId: String,
        nowEpochMilli: Long
    )

    suspend fun saveRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ): String

    suspend fun updateRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    )

    suspend fun deleteRecordSession(sessionId: String)

    fun observeTemplates(): Flow<List<TrackingTemplateSummary>>

    fun observeRecordHistory(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TrackingRecordHistory>>

    suspend fun getSuggestedValues(
        trackerId: String,
        limit: Int
    ): List<TrackingSuggestedValue>
}
