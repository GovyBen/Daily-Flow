package com.mhss.app.data.use_case

import android.content.Context
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.room.withTransaction
import com.mhss.app.data.model.JsonBackupData
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.database.entity.toTask
import com.mhss.app.domain.exception.BackupDataException
import com.mhss.app.domain.use_case.UpsertTaskUseCase
import com.mhss.app.domain.use_case.`interface`.ImportJsonDataUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import kotlin.uuid.Uuid

@Factory
class ImportJsonDataUseCaseImpl(
    private val context: Context,
    private val database: MyBrainDatabase,
    private val upsertTaskUseCase: UpsertTaskUseCase,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
): ImportJsonDataUseCase {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun invoke(fileUri: String, encrypted: Boolean, password: String?) {
        withContext(ioDispatcher) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val backupData = context.contentResolver.openInputStream(fileUri.toUri())?.use {
                    json.decodeFromStream<JsonBackupData>(it)
                } ?: throw BackupDataException.CouldNotReadFile

                // Pre-check
                if (backupData.schemaVersion !in 1..3)
                    throw BackupDataException.GenericError()

                database.withTransaction {
                    val noteFolderIdMap = HashMap<String, String>()
                    val updatedFolders = backupData.noteFolders.map { f ->
                        val id = if (f.id.isDigitsOnly()) Uuid.random().toString().also { noteFolderIdMap[f.id] = it } else f.id
                        f.copy(id = id)
                    }
                    database.noteDao().upsertNoteFolders(updatedFolders)

                    val updatedNotes = backupData.notes.map { n ->
                        val fid = if (n.folderId?.isDigitsOnly() == true) noteFolderIdMap[n.folderId] else n.folderId.takeIfNotNull()
                        n.copy(folderId = fid, id = n.id.toUuidIfNumber())
                    }
                    database.noteDao().upsertNotes(updatedNotes)

                    backupData.tasks.forEach {
                        upsertTaskUseCase(task = it.toTask().copy(id = it.id.toUuidIfNumber()), updateWidget = false)
                    }
                    database.diaryDao().upsertEntries(backupData.diary.map { it.copy(id = it.id.toUuidIfNumber()) })
                    database.bookmarkDao().upsertBookmarks(backupData.bookmarks.map { it.copy(id = it.id.toUuidIfNumber()) })

                    if (backupData.schemaVersion >= 3) {
                        database.dailyItemDao().upsertAll(
                            backupData.dailyItems.map { dailyItem ->
                                dailyItem.copy(id = dailyItem.id.toUuidIfNumber())
                            }
                        )
                        database.dailyItemDao().upsertCalendarSync(
                            backupData.dailyItemCalendarSync.map { sync ->
                                sync.copy(
                                    itemId = sync.itemId.toUuidIfNumber(),
                                    enabled = false,
                                    state = if (sync.systemEventId != null) "UNLINKED" else "NOT_SYNCED",
                                    lastError = null
                                )
                            }
                        )
                        database.dashboardPanelDao().upsertAll(backupData.dashboardPanels)
                    }

                    // P6: Import reminders (schema v2+)
                    if (backupData.schemaVersion >= 2) {
                        backupData.reminders.forEach { database.reminderDao().insert(it) }
                    }
                }
            } catch (_: SerializationException) { throw BackupDataException.CouldNotReadFile }
            catch (e: BackupDataException) { throw e }
            catch (_: Exception) { throw BackupDataException.GenericError() }
        }
    }

    private fun String?.takeIfNotNull(): String? = if (this == "null") null else this
    private fun String.toUuidIfNumber(): String = if (isDigitsOnly()) Uuid.random().toString() else this
}
