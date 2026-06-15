package com.mhss.app.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.database.migrations.MIGRATION_7_8
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderMigrationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MyBrainDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationFrom7To8PreservesLegacyDataAndAllowsMultipleReminders() {
        helper.createDatabase(TEST_DATABASE, 7).use { database ->
            database.execSQL(
                """
                INSERT INTO tasks
                (title, description, is_completed, priority, created_date, updated_date,
                 sub_tasks, dueDate, recurring, frequency, frequency_amount, alarmId, id)
                VALUES ('Keep task', 'legacy', 0, 1, 100, 200, '[]', 300, 0, 0, 1, 7, 'task')
                """.trimIndent()
            )
            database.execSQL("INSERT INTO alarms (id, time) VALUES (7, 123456)")
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            8,
            true,
            MIGRATION_7_8
        ).use { database ->
            assertEquals("Keep task", database.singleString("SELECT title FROM tasks"))
            assertEquals(123456L, database.singleLong("SELECT time FROM alarms WHERE id = 7"))

            database.execSQL(
                """
                INSERT INTO reminders
                (target_type, target_id, absolute_trigger_at, relative_offset_minutes,
                 enabled, status, created_at, updated_at)
                VALUES ('TASK', 'task', 200000, NULL, 1, 'PENDING', 1000, 1000)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO reminders
                (target_type, target_id, absolute_trigger_at, relative_offset_minutes,
                 enabled, status, created_at, updated_at)
                VALUES ('TASK', 'task', NULL, 15, 1, 'PENDING', 1000, 1000)
                """.trimIndent()
            )

            assertEquals(
                2L,
                database.singleLong(
                    "SELECT COUNT(DISTINCT id) FROM reminders WHERE target_id = 'task'"
                )
            )
        }
    }

    private companion object {
        const val TEST_DATABASE = "reminder-migration-test"
    }
}

private fun androidx.sqlite.db.SupportSQLiteDatabase.singleString(query: String): String {
    return query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getString(0)
    }
}

private fun androidx.sqlite.db.SupportSQLiteDatabase.singleLong(query: String): Long {
    return query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }
}
