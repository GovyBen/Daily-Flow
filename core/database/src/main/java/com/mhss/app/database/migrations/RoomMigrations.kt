@file:OptIn(ExperimentalUuidApi::class)
package com.mhss.app.database.migrations

import androidx.core.database.getIntOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Added note folders
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE note_folders (name TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `notes_new` (`title` TEXT NOT NULL, `content` TEXT NOT NULL, `created_date` INTEGER NOT NULL, `updated_date` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `folder_id` INTEGER, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, FOREIGN KEY (folder_id) REFERENCES note_folders (id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO notes_new (title, content, created_date, updated_date, pinned, id) SELECT title, content, created_date, updated_date, pinned, id FROM notes")
        db.execSQL("DROP TABLE notes")
        db.execSQL("ALTER TABLE notes_new RENAME TO notes")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurring INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN frequency INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN frequency_amount INTEGER NOT NULL DEFAULT 1")
    }
}

// Migrating from using auto-incrementing integer IDs to UUIDs
val MIGRATION_4_5 = object : Migration(4, 5) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // Create a mapping of old folder IDs to new UUIDs
        val folderIdMapping = HashMap<Int, String>()

        // Migrate note_folders and notes tables
        db.execSQL("CREATE TABLE note_folders_new (name TEXT NOT NULL, id TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE notes_new (title TEXT NOT NULL, content TEXT NOT NULL, created_date INTEGER NOT NULL, updated_date INTEGER NOT NULL, pinned INTEGER NOT NULL, folder_id TEXT, id TEXT PRIMARY KEY NOT NULL)")

        val folderCursor = db.query("SELECT * FROM note_folders")
        while (folderCursor.moveToNext()) {
            val oldId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow("id"))
            val name = folderCursor.getString(folderCursor.getColumnIndexOrThrow("name"))
            val newId = Uuid.random().toString()

            folderIdMapping[oldId] = newId

            db.execSQL(
                "INSERT INTO note_folders_new (id, name) VALUES (?, ?)",
                arrayOf(newId, name)
            )
        }
        folderCursor.close()

        val notesCursor = db.query("SELECT * FROM notes")
        while (notesCursor.moveToNext()) {
            val title = notesCursor.getString(notesCursor.getColumnIndexOrThrow("title"))
            val content = notesCursor.getString(notesCursor.getColumnIndexOrThrow("content"))
            val createdDate = notesCursor.getLong(notesCursor.getColumnIndexOrThrow("created_date"))
            val updatedDate = notesCursor.getLong(notesCursor.getColumnIndexOrThrow("updated_date"))
            val pinned = notesCursor.getInt(notesCursor.getColumnIndexOrThrow("pinned"))

            val oldFolderId =
                notesCursor.getIntOrNull(notesCursor.getColumnIndexOrThrow("folder_id"))
            val newFolderId = oldFolderId?.let { folderIdMapping[it] }

            val newId = Uuid.random().toString()

            db.execSQL(
                "INSERT INTO notes_new (id, title, content, created_date, updated_date, pinned, folder_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(newId, title, content, createdDate, updatedDate, pinned, newFolderId)
            )
        }
        notesCursor.close()

        db.execSQL("DROP TABLE notes")
        db.execSQL("DROP TABLE note_folders")
        db.execSQL("ALTER TABLE note_folders_new RENAME TO note_folders")
        db.execSQL("ALTER TABLE notes_new RENAME TO notes")

        // Migrate bookmarks table
        db.execSQL("CREATE TABLE bookmarks_new (url TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, created_date INTEGER NOT NULL, updated_date INTEGER NOT NULL, id TEXT PRIMARY KEY NOT NULL)")

        val bookmarksCursor = db.query("SELECT * FROM bookmarks")
        while (bookmarksCursor.moveToNext()) {
            val url = bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow("url"))
            val title = bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow("title"))
            val description =
                bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow("description"))
            val createdDate =
                bookmarksCursor.getLong(bookmarksCursor.getColumnIndexOrThrow("created_date"))
            val updatedDate =
                bookmarksCursor.getLong(bookmarksCursor.getColumnIndexOrThrow("updated_date"))
            val newId = Uuid.random().toString()

            db.execSQL(
                "INSERT INTO bookmarks_new (id, url, title, description, created_date, updated_date) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(newId, url, title, description, createdDate, updatedDate)
            )
        }
        bookmarksCursor.close()

        db.execSQL("DROP TABLE bookmarks")
        db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")

        // Migrate alarms table first and collect alarm ID mapping
        val alarmIdSet = HashSet<Int>()
        db.execSQL("CREATE TABLE alarms_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time INTEGER NOT NULL)")

        val alarmsCursor = db.query("SELECT * FROM alarms")
        while (alarmsCursor.moveToNext()) {
            val oldAlarmId = alarmsCursor.getInt(alarmsCursor.getColumnIndexOrThrow("id"))
            val time = alarmsCursor.getLong(alarmsCursor.getColumnIndexOrThrow("time"))

            db.execSQL(
                "INSERT INTO alarms_new (id, time) VALUES (?, ?)",
                arrayOf<Any?>(oldAlarmId, time)
            )

            alarmIdSet.add(oldAlarmId)
        }
        alarmsCursor.close()

        db.execSQL("DROP TABLE alarms")
        db.execSQL("ALTER TABLE alarms_new RENAME TO alarms")

        // Migrate tasks table
        db.execSQL("CREATE TABLE tasks_new (title TEXT NOT NULL, description TEXT NOT NULL, is_completed INTEGER NOT NULL, priority INTEGER NOT NULL, created_date INTEGER NOT NULL, updated_date INTEGER NOT NULL, sub_tasks TEXT NOT NULL, dueDate INTEGER NOT NULL, recurring INTEGER NOT NULL, frequency INTEGER NOT NULL, frequency_amount INTEGER NOT NULL, alarmId INTEGER, id TEXT PRIMARY KEY NOT NULL)")

        val tasksCursor = db.query("SELECT * FROM tasks")
        while (tasksCursor.moveToNext()) {
            val oldTaskId = tasksCursor.getInt(tasksCursor.getColumnIndexOrThrow("id"))
            val title = tasksCursor.getString(tasksCursor.getColumnIndexOrThrow("title"))
            val description =
                tasksCursor.getString(tasksCursor.getColumnIndexOrThrow("description"))
            val isCompleted = tasksCursor.getInt(tasksCursor.getColumnIndexOrThrow("is_completed"))
            val priority = tasksCursor.getInt(tasksCursor.getColumnIndexOrThrow("priority"))
            val createdDate = tasksCursor.getLong(tasksCursor.getColumnIndexOrThrow("created_date"))
            val updatedDate = tasksCursor.getLong(tasksCursor.getColumnIndexOrThrow("updated_date"))
            val subTasks = tasksCursor.getString(tasksCursor.getColumnIndexOrThrow("sub_tasks"))
            val dueDate = tasksCursor.getLong(tasksCursor.getColumnIndexOrThrow("dueDate"))
            val recurring = tasksCursor.getInt(tasksCursor.getColumnIndexOrThrow("recurring"))
            val frequency = tasksCursor.getInt(tasksCursor.getColumnIndexOrThrow("frequency"))
            val frequencyAmount =
                tasksCursor.getInt(tasksCursor.getColumnIndexOrThrow("frequency_amount"))

            // old version was using the task id as the alarm id
            val alarmId = if (oldTaskId in alarmIdSet) oldTaskId else null
            val newId = Uuid.random().toString()

            db.execSQL(
                "INSERT INTO tasks_new (id, title, description, is_completed, priority, created_date, updated_date, sub_tasks, dueDate, recurring, frequency, frequency_amount, alarmId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    newId,
                    title,
                    description,
                    isCompleted,
                    priority,
                    createdDate,
                    updatedDate,
                    subTasks,
                    dueDate,
                    recurring,
                    frequency,
                    frequencyAmount,
                    alarmId
                )
            )
        }
        tasksCursor.close()

        db.execSQL("DROP TABLE tasks")
        db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

        // Migrate diary table
        db.execSQL("CREATE TABLE diary_new (title TEXT NOT NULL, content TEXT NOT NULL, created_date INTEGER NOT NULL, updated_date INTEGER NOT NULL, mood INTEGER NOT NULL, id TEXT PRIMARY KEY NOT NULL)")

        val diaryCursor = db.query("SELECT * FROM diary")
        while (diaryCursor.moveToNext()) {
            val title = diaryCursor.getString(diaryCursor.getColumnIndexOrThrow("title"))
            val content = diaryCursor.getString(diaryCursor.getColumnIndexOrThrow("content"))
            val createdDate = diaryCursor.getLong(diaryCursor.getColumnIndexOrThrow("created_date"))
            val updatedDate = diaryCursor.getLong(diaryCursor.getColumnIndexOrThrow("updated_date"))
            val mood = diaryCursor.getInt(diaryCursor.getColumnIndexOrThrow("mood"))
            val newId = Uuid.random().toString()

            db.execSQL(
                "INSERT INTO diary_new (id, title, content, created_date, updated_date, mood) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(newId, title, content, createdDate, updatedDate, mood)
            )
        }
        diaryCursor.close()

        db.execSQL("DROP TABLE diary")
        db.execSQL("ALTER TABLE diary_new RENAME TO diary")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tracking_templates` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `icon` TEXT NOT NULL,
                `color` INTEGER NOT NULL,
                `is_active` INTEGER NOT NULL,
                `display_order` INTEGER NOT NULL,
                `created_at_epoch_milli` INTEGER NOT NULL,
                `updated_at_epoch_milli` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
                `index_tracking_templates_display_order_created_at_epoch_milli_id`
            ON `tracking_templates` (`display_order`, `created_at_epoch_milli`, `id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tracking_trackers` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `unit` TEXT,
                `config_json` TEXT NOT NULL,
                `is_active` INTEGER NOT NULL,
                `created_at_epoch_milli` INTEGER NOT NULL,
                `updated_at_epoch_milli` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_trackers_type_is_active`
            ON `tracking_trackers` (`type`, `is_active`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tracking_template_fields` (
                `id` TEXT NOT NULL,
                `template_id` TEXT NOT NULL,
                `tracker_id` TEXT NOT NULL,
                `display_order` INTEGER NOT NULL,
                `required` INTEGER NOT NULL,
                `display_name_override` TEXT,
                `default_value_json` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`template_id`) REFERENCES `tracking_templates`(`id`)
                    ON UPDATE CASCADE ON DELETE NO ACTION,
                FOREIGN KEY(`tracker_id`) REFERENCES `tracking_trackers`(`id`)
                    ON UPDATE CASCADE ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_template_fields_template_id`
            ON `tracking_template_fields` (`template_id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_template_fields_tracker_id`
            ON `tracking_template_fields` (`tracker_id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
                `index_tracking_template_fields_template_id_display_order_id`
            ON `tracking_template_fields` (`template_id`, `display_order`, `id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tracking_tracker_options` (
                `id` TEXT NOT NULL,
                `tracker_id` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `numeric_value` REAL,
                `color` INTEGER,
                `display_order` INTEGER NOT NULL,
                `is_active` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`tracker_id`) REFERENCES `tracking_trackers`(`id`)
                    ON UPDATE CASCADE ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_tracker_options_tracker_id`
            ON `tracking_tracker_options` (`tracker_id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
                `index_tracking_tracker_options_tracker_id_display_order_id`
            ON `tracking_tracker_options` (`tracker_id`, `display_order`, `id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tracking_record_sessions` (
                `id` TEXT NOT NULL,
                `template_id` TEXT NOT NULL,
                `occurred_at_epoch_milli` INTEGER NOT NULL,
                `zone_id` TEXT NOT NULL,
                `note` TEXT,
                `source` TEXT NOT NULL,
                `created_at_epoch_milli` INTEGER NOT NULL,
                `updated_at_epoch_milli` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`template_id`) REFERENCES `tracking_templates`(`id`)
                    ON UPDATE CASCADE ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
                `index_tracking_record_sessions_template_id_occurred_at_epoch_milli`
            ON `tracking_record_sessions` (`template_id`, `occurred_at_epoch_milli`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tracking_data_points` (
                `id` TEXT NOT NULL,
                `session_id` TEXT,
                `tracker_id` TEXT NOT NULL,
                `epoch_milli` INTEGER NOT NULL,
                `utc_offset_seconds` INTEGER NOT NULL,
                `value` REAL,
                `label` TEXT,
                `note` TEXT,
                `option_id` TEXT,
                `created_at_epoch_milli` INTEGER NOT NULL,
                `updated_at_epoch_milli` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`session_id`) REFERENCES `tracking_record_sessions`(`id`)
                    ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`tracker_id`) REFERENCES `tracking_trackers`(`id`)
                    ON UPDATE CASCADE ON DELETE NO ACTION,
                FOREIGN KEY(`option_id`) REFERENCES `tracking_tracker_options`(`id`)
                    ON UPDATE CASCADE ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_data_points_session_id`
            ON `tracking_data_points` (`session_id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_data_points_tracker_id_epoch_milli`
            ON `tracking_data_points` (`tracker_id`, `epoch_milli`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_tracking_data_points_option_id`
            ON `tracking_data_points` (`option_id`)
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE tracking_templates
            ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `target_type` TEXT NOT NULL,
                `target_id` TEXT NOT NULL,
                `absolute_trigger_at` INTEGER,
                `relative_offset_minutes` INTEGER,
                `enabled` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_reminders_target_type_target_id`
            ON `reminders` (`target_type`, `target_id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_reminders_enabled_status`
            ON `reminders` (`enabled`, `status`)
            """.trimIndent()
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_items` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `kind` TEXT NOT NULL,
                `start_at` INTEGER,
                `end_at` INTEGER,
                `due_at` INTEGER,
                `all_day` INTEGER NOT NULL DEFAULT 0,
                `time_zone_id` TEXT NOT NULL,
                `is_completable` INTEGER NOT NULL DEFAULT 1,
                `completed_at` INTEGER,
                `status` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `recurrence_frequency` TEXT,
                `recurrence_interval` INTEGER,
                `recurrence_week_days` TEXT NOT NULL DEFAULT '[]',
                `color` INTEGER,
                `sub_tasks_json` TEXT NOT NULL DEFAULT '[]',
                `legacy_source_type` TEXT,
                `legacy_source_id` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_daily_items_status_due_at`
            ON `daily_items` (`status`, `due_at`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_daily_items_status_start_at`
            ON `daily_items` (`status`, `start_at`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_daily_items_completed_at`
            ON `daily_items` (`completed_at`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_items_legacy_source_type_legacy_source_id`
            ON `daily_items` (`legacy_source_type`, `legacy_source_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_item_calendar_sync` (
                `item_id` TEXT NOT NULL,
                `enabled` INTEGER NOT NULL DEFAULT 0,
                `system_calendar_id` INTEGER,
                `system_event_id` INTEGER,
                `state` TEXT NOT NULL,
                `last_synced_at` INTEGER,
                `last_local_fingerprint` TEXT,
                `last_provider_fingerprint` TEXT,
                `last_error` TEXT,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`item_id`),
                FOREIGN KEY(`item_id`) REFERENCES `daily_items`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_daily_item_calendar_sync_enabled_state`
            ON `daily_item_calendar_sync` (`enabled`, `state`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_item_calendar_sync_system_event_id`
            ON `daily_item_calendar_sync` (`system_event_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dashboard_panels` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `enabled` INTEGER NOT NULL,
                `display_order` INTEGER NOT NULL,
                `size` TEXT NOT NULL,
                `config_json` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_dashboard_panels_enabled_display_order`
            ON `dashboard_panels` (`enabled`, `display_order`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_dashboard_panels_type`
            ON `dashboard_panels` (`type`)
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO `daily_items` (
                `id`,
                `title`,
                `description`,
                `kind`,
                `due_at`,
                `all_day`,
                `time_zone_id`,
                `is_completable`,
                `completed_at`,
                `status`,
                `priority`,
                `recurrence_frequency`,
                `recurrence_interval`,
                `sub_tasks_json`,
                `legacy_source_type`,
                `legacy_source_id`,
                `created_at`,
                `updated_at`
            )
            SELECT
                `id`,
                `title`,
                `description`,
                'TASK',
                CASE WHEN `dueDate` = 0 THEN NULL ELSE `dueDate` END,
                0,
                'SYSTEM',
                1,
                CASE WHEN `is_completed` = 1 THEN `updated_date` ELSE NULL END,
                CASE WHEN `is_completed` = 1 THEN 'COMPLETED' ELSE 'ACTIVE' END,
                CASE `priority` WHEN 2 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' ELSE 'LOW' END,
                CASE
                    WHEN `recurring` = 1 THEN
                        CASE `frequency`
                            WHEN 0 THEN 'EVERY_MINUTES'
                            WHEN 1 THEN 'HOURLY'
                            WHEN 2 THEN 'DAILY'
                            WHEN 3 THEN 'WEEKLY'
                            WHEN 4 THEN 'MONTHLY'
                            WHEN 5 THEN 'YEARLY'
                            ELSE 'DAILY'
                        END
                    ELSE NULL
                END,
                CASE WHEN `recurring` = 1 THEN `frequency_amount` ELSE NULL END,
                `sub_tasks`,
                'TASK',
                `id`,
                `created_date`,
                `updated_date`
            FROM `tasks`
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE `reminders`
            SET `target_type` = 'DAILY_ITEM'
            WHERE `target_type` = 'TASK'
                AND `target_id` IN (
                    SELECT `id` FROM `daily_items`
                    WHERE `legacy_source_type` = 'TASK'
                )
            """.trimIndent()
        )
    }
}
