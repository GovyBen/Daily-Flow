package com.mhss.app.tracking.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingTransactionStoreTest {

    private lateinit var database: TrackingSchemaTestDatabase
    private lateinit var store: TrackingTransactionStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TrackingSchemaTestDatabase::class.java
        ).allowMainThreadQueries().build()
        store = TrackingTransactionStore(
            database = database,
            templateDao = database.templateDao(),
            trackerDao = database.trackerDao(),
            sessionDao = database.sessionDao(),
            dataPointDao = database.dataPointDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun failedPointInsertRollsBackWholeSession() = runBlocking {
        insertTemplateAndTracker()
        val session = session("session")
        val points = listOf(
            point("point-1", session.id),
            point("point-2", session.id, trackerId = "missing-tracker")
        )

        val failed = runCatching { store.saveSession(session, points) }.isFailure

        assertTrue(failed)
        assertNull(database.sessionDao().getSession(session.id))
        assertTrue(database.dataPointDao().getDataPointsForSession(session.id).isEmpty())
    }

    @Test
    fun failedFieldInsertRollsBackTemplateCreation() = runBlocking {
        database.trackerDao().insertTracker(tracker())
        val template = template()
        val fields = listOf(
            TemplateFieldEntity(
                id = "field",
                templateId = template.id,
                trackerId = "missing-tracker"
            )
        )

        val failed = runCatching { store.createTemplate(template, fields) }.isFailure

        assertTrue(failed)
        assertNull(database.templateDao().getTemplate(template.id))
    }

    @Test
    fun updatingSessionReplacesPointsWithoutOrphans() = runBlocking {
        insertTemplateAndTracker()
        val original = session("session", note = "before")
        store.saveSession(
            original,
            listOf(point("old-1", original.id), point("old-2", original.id))
        )

        val updated = original.copy(note = "after", updatedAtEpochMilli = 2_000)
        store.updateSession(updated, listOf(point("new", updated.id)))

        assertEquals("after", database.sessionDao().getSession(updated.id)?.note)
        assertEquals(
            listOf("new"),
            database.dataPointDao().getDataPointsForSession(updated.id).map { it.id }
        )
    }

    @Test
    fun rangeQueriesUseInclusiveStartAndExclusiveEnd() = runBlocking {
        insertTemplateAndTracker()
        listOf(99L, 100L, 199L, 200L).forEach { time ->
            val session = session("session-$time", occurredAt = time)
            store.saveSession(session, listOf(point("point-$time", session.id, epochMilli = time)))
        }

        val sessions = database.sessionDao().getSessionsInRange("template", 100, 200)
        val points = database.dataPointDao().getDataPointsInRange("tracker", 100, 200)

        assertEquals(listOf(100L, 199L), sessions.map { it.occurredAtEpochMilli })
        assertEquals(listOf(100L, 199L), points.map { it.epochMilli })
    }

    @Test
    fun deactivationPreservesReferencedRows() = runBlocking {
        insertTemplateAndTracker()
        database.trackerDao().insertOptions(listOf(option()))
        val session = session("session")
        store.saveSession(
            session,
            listOf(point("point", session.id, optionId = "option"))
        )

        store.deactivateTemplate("template", 2_000)
        store.deactivateTracker("tracker", 2_000)
        store.deactivateOption("option")

        assertFalse(database.templateDao().getTemplate("template")!!.isActive)
        assertFalse(database.trackerDao().getTracker("tracker")!!.isActive)
        assertTrue(database.dataPointDao().getDataPointsForSession(session.id).isNotEmpty())
    }

    private suspend fun insertTemplateAndTracker() {
        database.templateDao().insertTemplate(template())
        database.trackerDao().insertTracker(tracker())
    }

    private fun template() = RecordTemplateEntity(
        id = "template",
        name = "Health",
        color = 1,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1
    )

    private fun tracker() = TrackerEntity(
        id = "tracker",
        name = "Mood",
        type = TrackerType.SCALE,
        configJson = "{}",
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1
    )

    private fun option() = TrackerOptionEntity(
        id = "option",
        trackerId = "tracker",
        label = "Good"
    )

    private fun session(
        id: String,
        occurredAt: Long = 1_000,
        note: String? = null
    ) = RecordSessionEntity(
        id = id,
        templateId = "template",
        occurredAtEpochMilli = occurredAt,
        zoneId = "Asia/Shanghai",
        note = note,
        source = RecordSource.MANUAL,
        createdAtEpochMilli = 1_000,
        updatedAtEpochMilli = 1_000
    )

    private fun point(
        id: String,
        sessionId: String,
        trackerId: String = "tracker",
        epochMilli: Long = 1_000,
        optionId: String? = null
    ) = DataPointEntity(
        id = id,
        sessionId = sessionId,
        trackerId = trackerId,
        epochMilli = epochMilli,
        utcOffsetSeconds = 28_800,
        value = 1.0,
        optionId = optionId,
        createdAtEpochMilli = 1_000,
        updatedAtEpochMilli = 1_000
    )
}
