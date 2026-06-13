package com.mhss.app.tracking.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingSchemaTest {

    private lateinit var database: TrackingSchemaTestDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TrackingSchemaTestDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun schemaContainsRequiredForeignKeysAndIndexes() {
        val sql = database.openHelper.writableDatabase

        val fieldForeignKeys = sql.foreignKeys("tracking_template_fields")
        val optionForeignKeys = sql.foreignKeys("tracking_tracker_options")

        assertTrue(fieldForeignKeys.contains("tracking_templates:template_id:id:NO ACTION"))
        assertTrue(fieldForeignKeys.contains("tracking_trackers:tracker_id:id:NO ACTION"))
        assertTrue(optionForeignKeys.contains("tracking_trackers:tracker_id:id:NO ACTION"))

        assertTrue(
            sql.indexColumns("tracking_templates")
                .contains(listOf("display_order", "created_at_epoch_milli", "id"))
        )
        assertTrue(
            sql.indexColumns("tracking_trackers")
                .contains(listOf("type", "is_active"))
        )
        assertTrue(sql.indexColumns("tracking_template_fields").contains(listOf("template_id")))
        assertTrue(sql.indexColumns("tracking_template_fields").contains(listOf("tracker_id")))
        assertTrue(
            sql.indexColumns("tracking_template_fields")
                .contains(listOf("template_id", "display_order", "id"))
        )
        assertTrue(sql.indexColumns("tracking_tracker_options").contains(listOf("tracker_id")))
        assertTrue(
            sql.indexColumns("tracking_tracker_options")
                .contains(listOf("tracker_id", "display_order", "id"))
        )
        assertTrue(
            sql.indexColumns("tracking_record_sessions")
                .contains(listOf("template_id", "occurred_at_epoch_milli"))
        )
        assertTrue(sql.indexColumns("tracking_data_points").contains(listOf("session_id")))
        assertTrue(
            sql.indexColumns("tracking_data_points")
                .contains(listOf("tracker_id", "epoch_milli"))
        )
        assertTrue(sql.indexColumns("tracking_data_points").contains(listOf("option_id")))
    }

    @Test
    fun deactivationKeepsReferencesAndParentDeletionIsRejected() {
        val sql = database.openHelper.writableDatabase
        sql.execSQL(
            """
            INSERT INTO tracking_templates
            (id, name, description, icon, color, is_active, display_order,
             created_at_epoch_milli, updated_at_epoch_milli)
            VALUES ('template', 'Health', '', '', 1, 1, 0, 1, 1)
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO tracking_trackers
            (id, name, type, unit, config_json, is_active,
             created_at_epoch_milli, updated_at_epoch_milli)
            VALUES ('tracker', 'Mood', 'SCALE', NULL, '{}', 1, 1, 1)
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO tracking_template_fields
            (id, template_id, tracker_id, display_order, required,
             display_name_override, default_value_json)
            VALUES ('field', 'template', 'tracker', 0, 1, NULL, NULL)
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO tracking_tracker_options
            (id, tracker_id, label, numeric_value, color, display_order, is_active)
            VALUES ('option', 'tracker', 'Good', 4.0, NULL, 0, 1)
            """.trimIndent()
        )

        sql.execSQL("UPDATE tracking_templates SET is_active = 0 WHERE id = 'template'")
        sql.execSQL("UPDATE tracking_trackers SET is_active = 0 WHERE id = 'tracker'")
        sql.execSQL("UPDATE tracking_tracker_options SET is_active = 0 WHERE id = 'option'")

        assertEquals(1, sql.rowCount("tracking_template_fields"))
        assertEquals(1, sql.rowCount("tracking_tracker_options"))

        val deleteFailed = runCatching {
            sql.execSQL("DELETE FROM tracking_trackers WHERE id = 'tracker'")
        }.isFailure
        assertTrue(deleteFailed)
        assertEquals(1, sql.rowCount("tracking_trackers"))
    }

    @Test
    fun multipleSelectionsCanShareSessionTrackerAndTimestamp() {
        val sql = database.openHelper.writableDatabase
        sql.insertTemplateAndTracker()
        sql.execSQL(
            """
            INSERT INTO tracking_tracker_options
            (id, tracker_id, label, numeric_value, color, display_order, is_active)
            VALUES
            ('option-1', 'tracker', 'Chest', 1.0, NULL, 0, 1),
            ('option-2', 'tracker', 'Back', 1.0, NULL, 1, 1)
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO tracking_record_sessions
            (id, template_id, occurred_at_epoch_milli, zone_id, note, source,
             created_at_epoch_milli, updated_at_epoch_milli)
            VALUES ('session', 'template', 1000, 'Asia/Shanghai', NULL, 'MANUAL', 1000, 1000)
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO tracking_data_points
            (id, session_id, tracker_id, epoch_milli, utc_offset_seconds, value,
             label, note, option_id, created_at_epoch_milli, updated_at_epoch_milli)
            VALUES
            ('point-1', 'session', 'tracker', 1000, 28800, 1.0,
             'Chest', NULL, 'option-1', 1000, 1000),
            ('point-2', 'session', 'tracker', 1000, 28800, 1.0,
             'Back', NULL, 'option-2', 1000, 1000)
            """.trimIndent()
        )

        assertEquals(2, sql.rowCount("tracking_data_points"))
    }
}

private fun SupportSQLiteDatabase.insertTemplateAndTracker() {
    execSQL(
        """
        INSERT INTO tracking_templates
        (id, name, description, icon, color, is_active, display_order,
         created_at_epoch_milli, updated_at_epoch_milli)
        VALUES ('template', 'Health', '', '', 1, 1, 0, 1, 1)
        """.trimIndent()
    )
    execSQL(
        """
        INSERT INTO tracking_trackers
        (id, name, type, unit, config_json, is_active,
         created_at_epoch_milli, updated_at_epoch_milli)
        VALUES ('tracker', 'Exercise', 'MULTI_SELECT', NULL, '{}', 1, 1, 1)
        """.trimIndent()
    )
}

private fun SupportSQLiteDatabase.foreignKeys(table: String): Set<String> {
    return buildSet {
        query("PRAGMA foreign_key_list(`$table`)").use { cursor ->
            val tableIndex = cursor.getColumnIndexOrThrow("table")
            val fromIndex = cursor.getColumnIndexOrThrow("from")
            val toIndex = cursor.getColumnIndexOrThrow("to")
            val onDeleteIndex = cursor.getColumnIndexOrThrow("on_delete")
            while (cursor.moveToNext()) {
                add(
                    listOf(
                        cursor.getString(tableIndex),
                        cursor.getString(fromIndex),
                        cursor.getString(toIndex),
                        cursor.getString(onDeleteIndex)
                    ).joinToString(":")
                )
            }
        }
    }
}

private fun SupportSQLiteDatabase.indexColumns(table: String): Set<List<String>> {
    return buildSet {
        query("PRAGMA index_list(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                val indexName = cursor.getString(nameIndex)
                query("PRAGMA index_info(`$indexName`)").use { info ->
                    val columnIndex = info.getColumnIndexOrThrow("name")
                    val columns = buildList {
                        while (info.moveToNext()) {
                            add(info.getString(columnIndex))
                        }
                    }
                    add(columns)
                }
            }
        }
    }
}

private fun SupportSQLiteDatabase.rowCount(table: String): Int {
    return query("SELECT COUNT(*) FROM `$table`").use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }
}

@Database(
    entities = [
        RecordTemplateEntity::class,
        TrackerEntity::class,
        TemplateFieldEntity::class,
        TrackerOptionEntity::class,
        RecordSessionEntity::class,
        DataPointEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TrackingDatabaseConverters::class)
abstract class TrackingSchemaTestDatabase : RoomDatabase()
