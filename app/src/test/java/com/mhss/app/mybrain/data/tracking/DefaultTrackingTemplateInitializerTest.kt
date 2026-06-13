package com.mhss.app.mybrain.data.tracking

import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.PrefsKey
import com.mhss.app.preferences.domain.repository.PreferenceRepository
import com.mhss.app.tracking.domain.defaults.DefaultTrackingTemplates
import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingSuggestedValue
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTrackingTemplateInitializerTest {

    @Test
    fun initializesOnceAndMarksCompletionAfterAllTemplatesSucceed() = runBlocking {
        val preferences = FakePreferenceRepository()
        val tracking = FakeTrackingRepository()
        val initializer = DefaultTrackingTemplateInitializer(preferences, tracking)

        initializer.initialize(1_000)
        initializer.initialize(2_000)

        assertEquals(
            DefaultTrackingTemplates.templates.map { it.id },
            tracking.createdTemplateIds
        )
        assertTrue(
            preferences.values[
                PrefsConstants.DEFAULT_TRACKING_TEMPLATES_INITIALIZED
            ] as Boolean
        )
    }

    @Test
    fun failedInitializationRetriesWithoutRecreatingExistingTemplates() = runBlocking {
        val preferences = FakePreferenceRepository()
        val tracking = FakeTrackingRepository(
            failOnceAtId = DefaultTrackingTemplates.MOOD_TEMPLATE_ID
        )
        val initializer = DefaultTrackingTemplateInitializer(preferences, tracking)

        assertTrue(runCatching { initializer.initialize(1_000) }.isFailure)
        initializer.initialize(2_000)

        assertEquals(
            DefaultTrackingTemplates.templates.map { it.id },
            tracking.createdTemplateIds
        )
        assertTrue(
            preferences.values[
                PrefsConstants.DEFAULT_TRACKING_TEMPLATES_INITIALIZED
            ] as Boolean
        )
    }
}

private class FakePreferenceRepository : PreferenceRepository {
    val values = mutableMapOf<String, Any>()

    override suspend fun <T> savePreference(key: PrefsKey<T>, value: T) {
        values[key.name] = value as Any
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPreference(key: PrefsKey<T>, defaultValue: T): Flow<T> =
        MutableStateFlow(values[key.name] as T? ?: defaultValue)
}

private class FakeTrackingRepository(
    private val failOnceAtId: String? = null
) : TrackingRepository {
    val createdTemplateIds = mutableListOf<String?>()
    private val existingTemplateIds = mutableSetOf<String>()
    private var hasFailed = false

    override suspend fun createTemplateIfAbsent(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): Boolean {
        val id = requireNotNull(draft.id)
        if (id == failOnceAtId && !hasFailed) {
            hasFailed = true
            error("Simulated interrupted initialization")
        }
        if (!existingTemplateIds.add(id)) return false
        createdTemplateIds += id
        return true
    }

    override suspend fun createTemplate(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): String = error("Not used")

    override suspend fun updateTemplate(
        templateId: String,
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ) = error("Not used")

    override suspend fun duplicateTemplate(templateId: String, nowEpochMilli: Long): String =
        error("Not used")

    override suspend fun reorderTemplates(
        orderedTemplateIds: List<String>,
        nowEpochMilli: Long
    ) = error("Not used")

    override suspend fun setTemplatePinned(
        templateId: String,
        isPinned: Boolean,
        nowEpochMilli: Long
    ) = error("Not used")

    override suspend fun deactivateTemplate(
        templateId: String,
        nowEpochMilli: Long
    ) = error("Not used")

    override suspend fun saveRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ): String = error("Not used")

    override suspend fun updateRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ) = error("Not used")

    override suspend fun deleteRecordSession(sessionId: String) = error("Not used")

    override fun observeTemplates(): Flow<List<TrackingTemplateSummary>> = emptyFlow()

    override fun observeRecordHistory(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TrackingRecordHistory>> = emptyFlow()

    override suspend fun getSuggestedValues(
        trackerId: String,
        limit: Int
    ): List<TrackingSuggestedValue> = emptyList()
}
