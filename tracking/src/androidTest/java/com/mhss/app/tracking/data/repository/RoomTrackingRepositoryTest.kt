package com.mhss.app.tracking.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.tracking.data.database.TrackingTransactionStore
import com.mhss.app.tracking.data.factory.TrackingEntityFactory
import com.mhss.app.tracking.data.mapping.TrackerValueMapper
import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import com.mhss.app.tracking.domain.defaults.DefaultTrackingTemplates
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingFieldValue
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTrackingRepositoryTest {

    private lateinit var database: MyBrainDatabase
    private lateinit var repository: RoomTrackingRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MyBrainDatabase::class.java
        ).allowMainThreadQueries().build()
        val idGenerator = RepositoryIdGenerator()
        val factory = TrackingEntityFactory(idGenerator)
        repository = RoomTrackingRepository(
            templateDao = database.templateDao(),
            trackerDao = database.trackerDao(),
            sessionDao = database.sessionDao(),
            dataPointDao = database.dataPointDao(),
            transactionStore = TrackingTransactionStore(
                database = database,
                templateDao = database.templateDao(),
                trackerDao = database.trackerDao(),
                sessionDao = database.sessionDao(),
                dataPointDao = database.dataPointDao()
            ),
            entityFactory = factory,
            valueMapper = TrackerValueMapper(factory),
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun templateLifecycleStaysBehindRepositoryBoundary() = runBlocking {
        val firstId = repository.createTemplate(templateDraft("Health"), 1_000)
        val secondId = repository.createTemplate(templateDraft("Work"), 2_000)

        val first = repository.observeTemplates().first().first { it.id == firstId }
        assertEquals(2, first.fields.size)
        assertTrue(first.fields.all { it.id != null && it.trackerId != null })
        assertTrue(first.fields.first().tracker.options.all { it.id != null })
        assertEquals(false, first.isPinned)
        assertNull(first.lastRecordedAtEpochMilli)

        repository.updateTemplate(
            firstId,
            first.toDraft().copy(name = "Wellbeing"),
            nowEpochMilli = 3_000
        )
        val duplicateId = repository.duplicateTemplate(firstId, 4_000)
        repository.setTemplatePinned(firstId, true, 4_500)
        repository.reorderTemplates(listOf(duplicateId, secondId, firstId), 5_000)

        val templates = repository.observeTemplates().first()
        assertEquals(
            listOf(firstId, duplicateId, secondId),
            templates.map { it.id }
        )
        assertTrue(templates.first().isPinned)
        assertEquals("Wellbeing", templates.first { it.id == firstId }.name)
        assertNotEquals(
            templates.first().fields.first().trackerId,
            templates.last().fields.first().trackerId
        )
    }

    @Test
    fun saveUpdateHistorySuggestionsAndDeleteUseMappedPoints() = runBlocking {
        val templateId = repository.createTemplate(templateDraft("Health"), 1_000)
        val template = repository.observeTemplates().first().single()
        val selectField = template.fields.first()
        val textField = template.fields.last()
        val chest = selectField.tracker.options.first { it.label == "Chest" }
        val back = selectField.tracker.options.first { it.label == "Back" }

        val sessionId = repository.saveRecordSession(
            command(
                templateId = templateId,
                selectFieldId = selectField.id!!,
                textFieldId = textField.id!!,
                selectedOptions = setOf(chest.id!!, back.id!!),
                note = "First"
            ),
            nowEpochMilli = 2_000
        )

        val savedPoints = database.dataPointDao().getDataPointsForSession(sessionId)
        assertEquals(3, savedPoints.size)
        assertEquals(setOf("Chest", "Back"), savedPoints.mapNotNull { it.label }.toSet())
        assertEquals("First", savedPoints.single { it.note != null }.note)
        assertEquals(1_000L, repository.observeTemplates().first().single().lastRecordedAtEpochMilli)

        val history = repository.observeRecordHistory(templateId, 0, 2_000).first()
        assertEquals(1, history.size)
        assertEquals(3, history.single().points.size)

        repository.updateRecordSession(
            command(
                id = sessionId,
                templateId = templateId,
                selectFieldId = selectField.id,
                textFieldId = textField.id,
                selectedOptions = setOf(back.id),
                note = "Updated"
            ),
            nowEpochMilli = 3_000
        )

        val updatedPoints = database.dataPointDao().getDataPointsForSession(sessionId)
        assertEquals(2, updatedPoints.size)
        assertEquals(listOf("Back"), updatedPoints.mapNotNull { it.label })
        assertEquals("Updated", updatedPoints.single { it.note != null }.note)

        val repeatedSessionId = repository.saveRecordSession(
            command(
                templateId = templateId,
                selectFieldId = selectField.id,
                textFieldId = textField.id,
                selectedOptions = setOf(back.id),
                note = "Repeated"
            ),
            nowEpochMilli = 4_000
        )
        val suggestions = repository.getSuggestedValues(selectField.trackerId!!, 5)
        assertEquals(1, suggestions.size)
        assertEquals("Back", suggestions.first().label)
        assertEquals(2, suggestions.first().usageCount)

        repository.deleteRecordSession(sessionId)
        repository.deleteRecordSession(repeatedSessionId)
        assertNull(database.sessionDao().getSession(sessionId))
        assertNull(database.sessionDao().getSession(repeatedSessionId))
        assertTrue(database.dataPointDao().getDataPointsForSession(sessionId).isEmpty())
    }

    @Test
    fun deactivatedTemplateLeavesTheActiveTemplateFlow() = runBlocking {
        val templateId = repository.createTemplate(templateDraft("Temporary"), 1_000)

        repository.deactivateTemplate(templateId, 2_000)

        assertTrue(repository.observeTemplates().first().isEmpty())
    }

    @Test
    fun calendarRecordsKeepTemplateDetailsAfterTemplateIsDeactivated() = runBlocking {
        val templateId = repository.createTemplate(templateDraft("Archived health"), 1_000)
        val template = repository.observeTemplates().first().single()
        val selectField = template.fields.first()
        val textField = template.fields.last()
        val option = selectField.tracker.options.first()
        val sessionId = repository.saveRecordSession(
            command(
                templateId = templateId,
                selectFieldId = selectField.id!!,
                textFieldId = textField.id!!,
                selectedOptions = setOf(option.id!!),
                note = "Point note"
            ).copy(note = "Visible in calendar"),
            nowEpochMilli = 2_000
        )
        repository.deactivateTemplate(templateId, 3_000)

        val record = repository.observeCalendarRecords(0, 2_000).first().single()

        assertEquals(sessionId, record.id)
        assertEquals("Archived health", record.templateName)
        assertEquals("Visible in calendar", record.note)
        assertTrue(repository.observeTemplates().first().isEmpty())
    }

    @Test
    fun requiredMissingFieldIsRejectedWithoutSavingSession() = runBlocking {
        val templateId = repository.createTemplate(templateDraft("Health"), 1_000)
        val template = repository.observeTemplates().first().single()
        val textField = template.fields.last()

        val failed = runCatching {
            repository.saveRecordSession(
                RecordSessionCommand(
                    templateId = templateId,
                    occurredAtEpochMilli = 1_000,
                    zoneId = "Asia/Shanghai",
                    utcOffsetSeconds = 28_800,
                    values = listOf(
                        TrackingFieldValue(
                            textField.id!!,
                            TrackerInputValue.Text("Only optional text")
                        )
                    )
                ),
                nowEpochMilli = 2_000
            )
        }.isFailure

        assertTrue(failed)
        assertTrue(database.sessionDao().getSessionsInRange(templateId, 0, 2_000).isEmpty())
    }

    @Test
    fun recordedTrackerRejectsTypeChangeAndRemovedFieldIsDeactivated() = runBlocking {
        val templateId = repository.createTemplate(templateDraft("Health"), 1_000)
        val template = repository.observeTemplates().first().single()
        val selectField = template.fields.first()
        val textField = template.fields.last()
        val chest = selectField.tracker.options.first()

        repository.saveRecordSession(
            command(
                templateId = templateId,
                selectFieldId = selectField.id!!,
                textFieldId = textField.id!!,
                selectedOptions = setOf(chest.id!!),
                note = "Recorded"
            ),
            nowEpochMilli = 2_000
        )

        val changedType = selectField.copy(
            tracker = selectField.tracker.copy(config = ScaleConfig())
        )
        assertTrue(
            runCatching {
                repository.updateTemplate(
                    templateId,
                    template.toDraft().copy(fields = listOf(changedType, textField)),
                    3_000
                )
            }.exceptionOrNull() is IncompatibleTrackerTypeChangeException
        )

        repository.updateTemplate(
            templateId,
            template.toDraft().copy(fields = listOf(textField)),
            4_000
        )

        assertEquals(false, database.trackerDao().getTracker(selectField.trackerId!!)?.isActive)
        assertTrue(
            database.trackerDao().getOptions(selectField.trackerId).all { !it.isActive }
        )
        assertEquals(
            listOf(textField.id),
            database.templateDao().getFields(templateId).map { it.id }
        )
        assertTrue(repository.observeTemplates().first().single().fields.single().hasRecordedData)
    }

    @Test
    fun updatingSessionPreservesPointsFromRemovedFields() = runBlocking {
        val templateId = repository.createTemplate(templateDraft("Health"), 1_000)
        val template = repository.observeTemplates().first().single()
        val selectField = template.fields.first()
        val textField = template.fields.last()
        val chest = selectField.tracker.options.first()
        val sessionId = repository.saveRecordSession(
            command(
                templateId = templateId,
                selectFieldId = selectField.id!!,
                textFieldId = textField.id!!,
                selectedOptions = setOf(chest.id!!),
                note = "Before"
            ),
            nowEpochMilli = 2_000
        )
        repository.updateTemplate(
            templateId,
            template.toDraft().copy(fields = listOf(textField)),
            3_000
        )

        repository.updateRecordSession(
            RecordSessionCommand(
                id = sessionId,
                templateId = templateId,
                occurredAtEpochMilli = 5_000,
                zoneId = "Asia/Shanghai",
                utcOffsetSeconds = 28_800,
                values = listOf(
                    TrackingFieldValue(
                        checkNotNull(textField.id),
                        TrackerInputValue.Text("After")
                    )
                )
            ),
            nowEpochMilli = 6_000
        )

        val points = database.dataPointDao().getDataPointsForSession(sessionId)
        assertEquals(2, points.size)
        assertEquals("Chest", points.single { it.trackerId == selectField.trackerId }.label)
        assertEquals("After", points.single { it.trackerId == textField.trackerId }.note)
        assertTrue(points.all { it.epochMilli == 5_000L })
    }

    @Test
    fun defaultTemplateCreationIsIdempotentAndDoesNotReactivate() = runBlocking {
        val draft = DefaultTrackingTemplates.templates.first()

        assertTrue(repository.createTemplateIfAbsent(draft, 1_000))
        database.templateDao().deactivateTemplate(draft.id!!, 2_000)
        assertTrue(!repository.createTemplateIfAbsent(draft, 3_000))

        val stored = database.templateDao().getTemplate(draft.id)
        assertEquals(false, stored?.isActive)
        assertEquals(
            draft.fields.map { it.trackerId },
            database.templateDao().getFields(draft.id).map { it.trackerId }
        )
    }

    private fun templateDraft(name: String) = TrackingTemplateDraft(
        name = name,
        color = 1,
        fields = listOf(
            TrackingFieldDraft(
                tracker = TrackingTrackerDraft(
                    name = "Exercise",
                    config = MultiSelectConfig(),
                    options = listOf(
                        TrackingOptionDraft(label = "Chest", displayOrder = 0),
                        TrackingOptionDraft(label = "Back", displayOrder = 1)
                    )
                ),
                displayOrder = 0,
                required = true
            ),
            TrackingFieldDraft(
                tracker = TrackingTrackerDraft(
                    name = "Note",
                    config = TextConfig()
                ),
                displayOrder = 1
            )
        )
    )

    private fun command(
        templateId: String,
        selectFieldId: String,
        textFieldId: String,
        selectedOptions: Set<String>,
        note: String,
        id: String? = null
    ) = RecordSessionCommand(
        id = id,
        templateId = templateId,
        occurredAtEpochMilli = 1_000,
        zoneId = "Asia/Shanghai",
        utcOffsetSeconds = 28_800,
        values = listOf(
            TrackingFieldValue(
                selectFieldId,
                TrackerInputValue.MultiSelect(selectedOptions)
            ),
            TrackingFieldValue(
                textFieldId,
                TrackerInputValue.Text(note)
            )
        )
    )
}

private fun com.mhss.app.tracking.domain.model.TrackingTemplateSummary.toDraft() =
    TrackingTemplateDraft(
        id = id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        displayOrder = displayOrder,
        fields = fields
    )

private class RepositoryIdGenerator : TrackingIdGenerator {
    private var next = 1

    override fun newId(): String = "repository-id-${next++}"
}
