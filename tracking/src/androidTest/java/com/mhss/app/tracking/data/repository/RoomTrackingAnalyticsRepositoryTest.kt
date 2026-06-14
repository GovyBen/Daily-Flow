package com.mhss.app.tracking.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.domain.model.TrackerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTrackingAnalyticsRepositoryTest {

    private lateinit var database: MyBrainDatabase
    private lateinit var repository: RoomTrackingAnalyticsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MyBrainDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomTrackingAnalyticsRepository(
            trackerDao = database.trackerDao(),
            dataPointDao = database.dataPointDao(),
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readsInactiveTrackerWithHalfOpenRange() = runBlocking {
        database.trackerDao().insertTracker(
            TrackerEntity(
                id = "tracker",
                name = "Archived number",
                type = TrackerType.NUMBER.name,
                unit = "kg",
                configJson = "{}",
                isActive = false,
                createdAtEpochMilli = 1,
                updatedAtEpochMilli = 1
            )
        )
        database.dataPointDao().insertDataPoints(
            listOf(
                point("before", 99, 1.0),
                point("start", 100, 2.0),
                point("last", 199, 3.0),
                point("end", 200, 4.0)
            )
        )

        val source = repository.getTrackerData("tracker", 100, 200)

        requireNotNull(source)
        assertFalse(source.isActive)
        assertEquals(TrackerType.NUMBER, source.trackerType)
        assertEquals("kg", source.unit)
        assertEquals(listOf(2.0, 3.0), source.points.map { it.value })

        val allHistory = repository.getTrackerData(
            "tracker",
            Long.MIN_VALUE,
            Long.MAX_VALUE
        )
        assertEquals(4, allHistory?.points?.size)
    }

    @Test
    fun missingTrackerReturnsNull() = runBlocking {
        assertNull(repository.getTrackerData("missing", 0, 1))
    }

    private fun point(
        id: String,
        epochMilli: Long,
        value: Double
    ) = DataPointEntity(
        id = id,
        trackerId = "tracker",
        epochMilli = epochMilli,
        utcOffsetSeconds = 0,
        value = value,
        createdAtEpochMilli = epochMilli,
        updatedAtEpochMilli = epochMilli
    )
}
