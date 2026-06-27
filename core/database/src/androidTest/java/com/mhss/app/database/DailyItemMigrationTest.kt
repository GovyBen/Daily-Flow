package com.mhss.app.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.database.migrations.MIGRATION_8_9
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyItemMigrationTest {

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
    fun migrationFrom8To9MigratesTasksAndRetargetsReminders() {
        helper.createDatabase(TEST_DATABASE, 8).use { database ->
            database.insertTask(
                id = "task-1",
                title = "Migrated task",
                description = "description",
                isCompleted = true,
                priority = 2,
                dueDate = 300
            )
            database.execSQL(
                """
                INSERT INTO reminders
                (target_type, target_id, absolute_trigger_at, relative_offset_minutes,
                 enabled, status, created_at, updated_at)
                VALUES ('TASK', 'task-1', 300, NULL, 1, 'PENDING', 100, 100)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            MIGRATION_8_9
        ).use { database ->
            assertEquals("Migrated task", database.dailySingleString("SELECT title FROM daily_items"))
            assertEquals("COMPLETED", database.dailySingleString("SELECT status FROM daily_items"))
            assertEquals(200L, database.dailySingleLong("SELECT completed_at FROM daily_items"))
            assertEquals("HIGH", database.dailySingleString("SELECT priority FROM daily_items"))
            assertEquals(
                "DAILY_ITEM",
                database.dailySingleString("SELECT target_type FROM reminders WHERE target_id = 'task-1'")
            )
            assertEquals(1L, database.dailySingleLong("SELECT COUNT(*) FROM tasks"))
        }
    }

    @Test
    fun migrationFrom8To9CreatesNewTablesForEmptyDatabase() {
        helper.createDatabase(TEST_DATABASE, 8).close()

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            MIGRATION_8_9
        ).use { database ->
            assertEquals(1L, database.dailyTableExists("daily_items"))
            assertEquals(1L, database.dailyTableExists("daily_item_calendar_sync"))
            assertEquals(1L, database.dailyTableExists("dashboard_panels"))
            assertEquals(0L, database.dailySingleLong("SELECT COUNT(*) FROM daily_items"))
            assertEquals(0L, database.dailySingleLong("SELECT COUNT(*) FROM dashboard_panels"))
        }
    }

    @Test
    fun migrationFrom8To9MigratesIncompleteNoDueTaskAsActiveNoDateItem() {
        helper.createDatabase(TEST_DATABASE, 8).use { database ->
            database.insertTask(
                id = "task-no-date",
                title = "No date task",
                isCompleted = false,
                priority = 1,
                dueDate = 0
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            MIGRATION_8_9
        ).use { database ->
            assertEquals("ACTIVE", database.dailySingleString("SELECT status FROM daily_items"))
            assertEquals("MEDIUM", database.dailySingleString("SELECT priority FROM daily_items"))
            assertEquals(true, database.dailySingleIsNull("SELECT due_at FROM daily_items"))
            assertEquals(true, database.dailySingleIsNull("SELECT completed_at FROM daily_items"))
        }
    }

    @Test
    fun migrationFrom8To9CalendarSyncRowsCascadeWhenDailyItemIsDeleted() {
        helper.createDatabase(TEST_DATABASE, 8).use { database ->
            database.insertTask(id = "task-cascade", title = "Cascade task")
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            MIGRATION_8_9
        ).use { database ->
            database.execSQL("PRAGMA foreign_keys=ON")
            database.execSQL(
                """
                INSERT INTO daily_item_calendar_sync
                (item_id, enabled, system_calendar_id, system_event_id, state, updated_at)
                VALUES ('task-cascade', 1, 1, 99, 'SYNCED', 500)
                """.trimIndent()
            )
            database.execSQL("DELETE FROM daily_items WHERE id = 'task-cascade'")

            assertEquals(0L, database.dailySingleLong("SELECT COUNT(*) FROM daily_item_calendar_sync"))
        }
    }

    private companion object {
        const val TEST_DATABASE = "daily-item-migration-test"
    }
}

private fun SupportSQLiteDatabase.insertTask(
    id: String,
    title: String,
    description: String = "",
    isCompleted: Boolean = false,
    priority: Int = 0,
    createdDate: Long = 100,
    updatedDate: Long = 200,
    subTasks: String = "[]",
    dueDate: Long = 0,
    recurring: Boolean = false,
    frequency: Int = 2,
    frequencyAmount: Int = 1
) {
    execSQL(
        """
        INSERT INTO tasks
        (title, description, is_completed, priority, created_date, updated_date,
         sub_tasks, dueDate, recurring, frequency, frequency_amount, alarmId, id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            title,
            description,
            if (isCompleted) 1 else 0,
            priority,
            createdDate,
            updatedDate,
            subTasks,
            dueDate,
            if (recurring) 1 else 0,
            frequency,
            frequencyAmount,
            id
        )
    )
}

private fun SupportSQLiteDatabase.dailySingleString(query: String): String {
    return query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getString(0)
    }
}

private fun SupportSQLiteDatabase.dailySingleLong(query: String): Long {
    return query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }
}

private fun SupportSQLiteDatabase.dailySingleIsNull(query: String): Boolean {
    return query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.isNull(0)
    }
}

private fun SupportSQLiteDatabase.dailyTableExists(tableName: String): Long {
    return dailySingleLong(
        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$tableName'"
    )
}
