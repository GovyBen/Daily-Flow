package com.mhss.app.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom

/**
 * Encrypted JSON backup and selective restore (DF-603, DF-604, DF-605).
 */
object BackupManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // ---- Backup (DF-603) ----

    fun exportEncrypted(
        data: BackupData,
        password: String,
        outputFile: File
    ): Result<Unit> = runCatching {
        val plainJson = json.encodeToString(data)
        val encrypted = EncryptionUtils.encryptString(plainJson, password)
        FileOutputStream(outputFile).use { it.write(encrypted) }
    }

    fun importEncrypted(
        inputFile: File,
        password: String
    ): Result<BackupData> = runCatching {
        val encrypted = inputFile.readBytes()
        val plainJson = EncryptionUtils.decryptString(encrypted, password)
        json.decodeFromString<BackupData>(plainJson)
    }

    // ---- Selective Restore Preview (DF-604, DF-605) ----

    fun previewCategories(data: BackupData): List<CategoryInfo> = listOf(
        CategoryInfo("tasks", "Tasks", data.tasks?.size ?: 0),
        CategoryInfo("calendar_events", "Calendar Events", data.calendarEvents?.size ?: 0),
        CategoryInfo("diary_entries", "Diary Entries", data.diaryEntries?.size ?: 0),
        CategoryInfo("notes", "Notes", data.notes?.size ?: 0),
        CategoryInfo("bookmarks", "Bookmarks", data.bookmarks?.size ?: 0),
        CategoryInfo("record_templates", "Record Templates", data.recordTemplates?.size ?: 0),
        CategoryInfo("record_sessions", "Record Sessions", data.recordSessions?.size ?: 0),
        CategoryInfo("preferences", "Preferences", data.preferences?.size ?: 0)
    ).filter { it.count > 0 }

    fun partialRestore(
        data: BackupData,
        selectedCategories: Set<String>,
        conflictResolution: ConflictResolution = ConflictResolution.SKIP_EXISTING
    ): PartialRestorePlan {
        val plan = mutableListOf<RestoreItem>()
        selectedCategories.forEach { category ->
            when (category) {
                "tasks" -> data.tasks?.forEach { plan.add(RestoreItem("task", it.id, it.title, conflictResolution)) }
                "calendar_events" -> data.calendarEvents?.forEach { plan.add(RestoreItem("calendar_event", it.id.toString(), it.title, conflictResolution)) }
                "diary_entries" -> data.diaryEntries?.forEach { plan.add(RestoreItem("diary_entry", it.id, it.title, conflictResolution)) }
                "notes" -> data.notes?.forEach { plan.add(RestoreItem("note", it.id, it.title, conflictResolution)) }
            }
        }
        return PartialRestorePlan(items = plan, selectedCategories = selectedCategories)
    }
}

// ---- Data Models ----

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAtEpochMilli: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val tasks: List<BackupTask>? = null,
    val calendarEvents: List<BackupCalendarEvent>? = null,
    val diaryEntries: List<BackupDiaryEntry>? = null,
    val notes: List<BackupNote>? = null,
    val bookmarks: List<BackupBookmark>? = null,
    val recordTemplates: List<BackupRecordTemplate>? = null,
    val recordSessions: List<BackupRecordSession>? = null,
    val preferences: List<BackupPreference>? = null
)

@Serializable data class BackupTask(val id: String, val title: String, val description: String = "",
    val priority: Int = 0, val dueDate: Long = 0L, val isCompleted: Boolean = false,
    val createdDate: Long = 0L, val updatedDate: Long = 0L)
@Serializable data class BackupCalendarEvent(val id: Long, val title: String, val description: String? = null,
    val start: Long, val end: Long, val location: String? = null, val calendarId: Long = 0)
@Serializable data class BackupDiaryEntry(val id: String, val title: String, val content: String,
    val moodValue: Int = 2, val createdDate: Long = 0L)
@Serializable data class BackupNote(val id: String, val title: String, val content: String = "",
    val createdDate: Long = 0L, val updatedDate: Long = 0L)
@Serializable data class BackupBookmark(val id: String, val title: String, val url: String = "",
    val createdDate: Long = 0L)
@Serializable data class BackupRecordTemplate(val id: String, val name: String, val fieldsJson: String = "[]")
@Serializable data class BackupRecordSession(val id: String, val templateId: String,
    val occurredAtEpochMilli: Long, val note: String? = null, val valuesJson: String = "[]")
@Serializable data class BackupPreference(val key: String, val value: String)

data class CategoryInfo(val key: String, val label: String, val count: Int)
data class PartialRestorePlan(val items: List<RestoreItem>, val selectedCategories: Set<String>)
data class RestoreItem(val type: String, val id: String, val name: String, val conflictResolution: ConflictResolution)
enum class ConflictResolution { OVERWRITE, SKIP_EXISTING, RENAME }
