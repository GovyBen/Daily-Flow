package com.mhss.app.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.database.migrations.MIGRATION_5_6
import com.mhss.app.database.migrations.MIGRATION_6_7
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingMigrationTest {

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
    fun freshInstallCreatesTrackingTables() {
        val database = Room.inMemoryDatabaseBuilder(context, MyBrainDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val tables = database.openHelper.writableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            ).use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }

            assertTrue(tables.containsAll(TRACKING_TABLES))
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationFrom5To6PreservesLegacyRowsAndAcceptsTrackingRows() {
        helper.createDatabase(TEST_DATABASE, 5).use { database ->
            database.execSQL(
                """
                INSERT INTO tasks
                (title, description, is_completed, priority, created_date, updated_date,
                 sub_tasks, dueDate, recurring, frequency, frequency_amount, alarmId, id)
                VALUES ('Keep task', 'legacy', 0, 1, 100, 200, '[]', 300, 0, 0, 1, 7, 'task')
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO diary
                (title, content, created_date, updated_date, mood, id)
                VALUES ('Keep diary', 'legacy', 100, 200, 4, 'diary')
                """.trimIndent()
            )
            database.execSQL("INSERT INTO alarms (id, time) VALUES (7, 123456)")
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            MIGRATION_5_6
        ).use { database ->
            assertEquals("Keep task", database.singleString("SELECT title FROM tasks"))
            assertEquals("Keep diary", database.singleString("SELECT title FROM diary"))
            assertEquals(123456L, database.singleLong("SELECT time FROM alarms WHERE id = 7"))

            database.execSQL(
                """
                INSERT INTO tracking_templates
                (id, name, description, icon, color, is_active, display_order,
                 created_at_epoch_milli, updated_at_epoch_milli)
                VALUES ('template', 'Health', '', '', 1, 1, 0, 1000, 1000)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO tracking_trackers
                (id, name, type, unit, config_json, is_active,
                 created_at_epoch_milli, updated_at_epoch_milli)
                VALUES ('tracker', 'Mood', 'SCALE', NULL, '{}', 1, 1000, 1000)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO tracking_template_fields
                (id, template_id, tracker_id, display_order, required,
                 display_name_override, default_value_json)
                VALUES ('field', 'template', 'tracker', 0, 1, NULL, NULL)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO tracking_record_sessions
                (id, template_id, occurred_at_epoch_milli, zone_id, note, source,
                 created_at_epoch_milli, updated_at_epoch_milli)
                VALUES ('session', 'template', 1000, 'Asia/Shanghai', NULL, 'MANUAL', 1000, 1000)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO tracking_data_points
                (id, session_id, tracker_id, epoch_milli, utc_offset_seconds, value,
                 label, note, option_id, created_at_epoch_milli, updated_at_epoch_milli)
                VALUES ('point', 'session', 'tracker', 1000, 28800, 5.0,
                        NULL, NULL, NULL, 1000, 1000)
                """.trimIndent()
            )

            assertEquals(1L, database.singleLong("SELECT COUNT(*) FROM tracking_data_points"))
            val foreignKeyViolations = database.query("PRAGMA foreign_key_check").use {
                it.count
            }
            assertEquals(0, foreignKeyViolations)
        }
    }

    @Test
    fun migrationFrom6To7PreservesTemplatesAndAddsPinnedState() {
        helper.createDatabase(TEST_DATABASE, 6).use { database ->
            database.execSQL(
                """
                INSERT INTO tracking_templates
                (id, name, description, icon, color, is_active, display_order,
                 created_at_epoch_milli, updated_at_epoch_milli)
                VALUES ('template', 'Health', '', '', 1, 1, 0, 1000, 1000)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            MIGRATION_6_7
        ).use { database ->
            assertEquals(
                0L,
                database.singleLong(
                    "SELECT is_pinned FROM tracking_templates WHERE id = 'template'"
                )
            )
            database.execSQL(
                "UPDATE tracking_templates SET is_pinned = 1 WHERE id = 'template'"
            )
            assertEquals(
                1L,
                database.singleLong(
                    "SELECT is_pinned FROM tracking_templates WHERE id = 'template'"
                )
            )
        }
    }

    private companion object {
        const val TEST_DATABASE = "tracking-migration-test"

        val TRACKING_TABLES = setOf(
            "tracking_templates",
            "tracking_trackers",
            "tracking_template_fields",
            "tracking_tracker_options",
            "tracking_record_sessions",
            "tracking_data_points"
        )
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
