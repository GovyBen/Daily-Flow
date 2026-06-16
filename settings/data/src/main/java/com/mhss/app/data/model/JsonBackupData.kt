package com.mhss.app.data.model

import com.mhss.app.database.entity.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON backup data model (DF-605).
 * Schema version 2 adds reminders. Tracking data uses the existing CSV format.
 */
@Serializable
data class JsonBackupData(
    @SerialName("schemaVersion") val schemaVersion: Int = 2,
    @SerialName("notes") val notes: List<NoteEntity> = emptyList(),
    @SerialName("noteFolders") val noteFolders: List<NoteFolderEntity> = emptyList(),
    @SerialName("tasks") val tasks: List<TaskEntity> = emptyList(),
    @SerialName("diary") val diary: List<DiaryEntryEntity> = emptyList(),
    @SerialName("bookmarks") val bookmarks: List<BookmarkEntity> = emptyList(),
    @SerialName("reminders") val reminders: List<ReminderEntity> = emptyList()
)
