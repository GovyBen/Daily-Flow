package com.mhss.app.database

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.database.entity.toReminder
import com.mhss.app.database.entity.toReminderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var database: MyBrainDatabase

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MyBrainDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun multipleRemindersForOneTargetKeepDistinctIdsAndRequestCodes() = runBlocking {
        val dao = database.reminderDao()
        val firstId = dao.insert(reminder(absoluteTriggerAt = 2_000).toReminderEntity())
        val secondId = dao.insert(
            reminder(
                absoluteTriggerAt = null,
                relativeOffsetMinutes = 15
            ).toReminderEntity()
        )

        val reminders = dao.getByTarget(ReminderTargetType.TASK.name, "task-1")
            .map { it.toReminder() }

        assertEquals(2, reminders.size)
        assertEquals(listOf(firstId, secondId), reminders.map { it.id })
        assertNotEquals(reminders[0].requestCode(), reminders[1].requestCode())
    }

    private fun reminder(
        absoluteTriggerAt: Long?,
        relativeOffsetMinutes: Int? = null
    ) = Reminder(
        targetType = ReminderTargetType.TASK,
        targetId = "task-1",
        absoluteTriggerAt = absoluteTriggerAt,
        relativeOffsetMinutes = relativeOffsetMinutes,
        createdAt = 1_000,
        updatedAt = 1_000
    )
}
