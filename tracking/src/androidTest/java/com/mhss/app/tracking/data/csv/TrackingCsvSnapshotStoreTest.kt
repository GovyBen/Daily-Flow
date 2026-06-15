package com.mhss.app.tracking.data.csv

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.serialization.TrackerConfigJson
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingCsvSnapshotStoreTest {

    private lateinit var database: MyBrainDatabase
    private lateinit var store: TrackingCsvSnapshotStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MyBrainDatabase::class.java
        ).allowMainThreadQueries().build()
        store = TrackingCsvSnapshotStore(
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
    fun importAndReadSnapshotAreEquivalent() = runBlocking {
        val snapshot = snapshot()

        store.import(snapshot)

        assertEquals(snapshot, store.readSnapshot())
    }

    @Test
    fun failedImportRollsBackAllRows() = runBlocking {
        val valid = snapshot()
        val invalid = valid.copy(
            dataPoints = valid.dataPoints + valid.dataPoints.first().copy(
                id = "broken-point",
                trackerId = "missing-tracker"
            )
        )

        val failed = runCatching { store.import(invalid) }.isFailure

        assertTrue(failed)
        assertTrue(database.templateDao().getAllTemplates().isEmpty())
        assertTrue(database.trackerDao().getAllTrackers().isEmpty())
        assertNull(database.sessionDao().getSession("session-1"))
        assertTrue(database.dataPointDao().getAllDataPoints().isEmpty())
    }

    private fun snapshot(): TrackingCsvSnapshot {
        val template = RecordTemplateEntity(
            id = "template-1",
            name = "Health",
            color = 1,
            createdAtEpochMilli = 100,
            updatedAtEpochMilli = 100
        )
        val tracker = TrackerEntity(
            id = "tracker-1",
            name = "Weight",
            type = TrackerType.NUMBER.name,
            unit = "kg",
            configJson = TrackerConfigJson.encode(NumberConfig()),
            createdAtEpochMilli = 100,
            updatedAtEpochMilli = 100
        )
        val field = TemplateFieldEntity(
            id = "field-1",
            templateId = template.id,
            trackerId = tracker.id,
            required = true
        )
        val session = RecordSessionEntity(
            id = "session-1",
            templateId = template.id,
            occurredAtEpochMilli = 1_000,
            zoneId = "UTC",
            source = RecordSource.IMPORT.name,
            createdAtEpochMilli = 1_000,
            updatedAtEpochMilli = 1_000
        )
        val point = DataPointEntity(
            id = "point-1",
            sessionId = session.id,
            trackerId = tracker.id,
            epochMilli = 1_000,
            utcOffsetSeconds = 0,
            value = 72.5,
            createdAtEpochMilli = 1_000,
            updatedAtEpochMilli = 1_000
        )
        return TrackingCsvSnapshot(
            templates = listOf(template),
            trackers = listOf(tracker),
            fields = listOf(field),
            sessions = listOf(session),
            dataPoints = listOf(point)
        )
    }
}
