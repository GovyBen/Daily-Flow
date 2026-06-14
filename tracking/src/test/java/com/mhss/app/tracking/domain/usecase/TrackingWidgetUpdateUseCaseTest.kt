package com.mhss.app.tracking.domain.usecase

import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.TrackingCalendarRecord
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingSuggestedValue
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.repository.TrackingRepository
import com.mhss.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingWidgetUpdateUseCaseTest {

    @Test
    fun pinningTemplateRefreshesTrackingWidget() = runBlocking {
        val repository = FakeTrackingRepository()
        val widgetUpdater = RecordingWidgetUpdater()

        SetTemplatePinnedUseCase(repository, widgetUpdater)(
            templateId = "fitness",
            isPinned = true,
            nowEpochMilli = 100
        )

        assertEquals(Triple("fitness", true, 100L), repository.pinned)
        assertEquals(
            listOf(WidgetUpdater.WidgetType.Tracking),
            widgetUpdater.updatedTypes
        )
    }

    @Test
    fun deactivatingTemplateRefreshesTrackingWidget() = runBlocking {
        val repository = FakeTrackingRepository()
        val widgetUpdater = RecordingWidgetUpdater()

        DeactivateTemplateUseCase(repository, widgetUpdater)(
            templateId = "fitness",
            nowEpochMilli = 200
        )

        assertEquals("fitness" to 200L, repository.deactivated)
        assertEquals(
            listOf(WidgetUpdater.WidgetType.Tracking),
            widgetUpdater.updatedTypes
        )
    }
}

private class RecordingWidgetUpdater : WidgetUpdater {
    val updatedTypes = mutableListOf<WidgetUpdater.WidgetType>()

    override suspend fun updateAll(type: WidgetUpdater.WidgetType) {
        updatedTypes += type
    }
}

private class FakeTrackingRepository : TrackingRepository {
    var pinned: Triple<String, Boolean, Long>? = null
    var deactivated: Pair<String, Long>? = null

    override suspend fun createTemplate(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ) = requireNotNull(draft.id)

    override suspend fun createTemplateIfAbsent(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ) = true

    override suspend fun updateTemplate(
        templateId: String,
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ) = Unit

    override suspend fun duplicateTemplate(
        templateId: String,
        nowEpochMilli: Long
    ) = "$templateId-copy"

    override suspend fun reorderTemplates(
        orderedTemplateIds: List<String>,
        nowEpochMilli: Long
    ) = Unit

    override suspend fun setTemplatePinned(
        templateId: String,
        isPinned: Boolean,
        nowEpochMilli: Long
    ) {
        pinned = Triple(templateId, isPinned, nowEpochMilli)
    }

    override suspend fun deactivateTemplate(
        templateId: String,
        nowEpochMilli: Long
    ) {
        deactivated = templateId to nowEpochMilli
    }

    override suspend fun saveRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ) = requireNotNull(command.id)

    override suspend fun updateRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ) = Unit

    override suspend fun deleteRecordSession(sessionId: String) = Unit

    override fun observeTemplates(): Flow<List<TrackingTemplateSummary>> =
        flowOf(emptyList())

    override fun observeRecordHistory(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TrackingRecordHistory>> = flowOf(emptyList())

    override fun observeCalendarRecords(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TrackingCalendarRecord>> = flowOf(emptyList())

    override suspend fun getSuggestedValues(
        trackerId: String,
        limit: Int
    ): List<TrackingSuggestedValue> = emptyList()
}
