package com.mhss.app.mybrain.content.data

import com.mhss.app.domain.model.Bookmark
import com.mhss.app.domain.model.DiaryEntry
import com.mhss.app.domain.model.Mood
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.NoteFolder
import com.mhss.app.domain.repository.BookmarkRepository
import com.mhss.app.domain.repository.DiaryRepository
import com.mhss.app.domain.repository.NoteRepository
import com.mhss.app.mybrain.content.domain.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultContentLibraryRepositoryTest {

    @Test
    fun combinesExistingRepositoriesAndSortsNewestFirst() = runBlocking {
        val repository = repository(
            notes = listOf(note(id = "note", updatedDate = 20L)),
            diary = listOf(diary(id = "diary", updatedDate = 30L)),
            bookmarks = listOf(bookmark(id = "link", updatedDate = 10L))
        )

        val items = repository.observeItems().first()

        assertEquals(
            listOf(ContentType.DIARY, ContentType.NOTE, ContentType.LINK),
            items.map { it.type }
        )
    }

    @Test
    fun filtersByTypeAndSearchesAcrossTitlePreviewAndUrl() = runBlocking {
        val repository = repository(
            notes = listOf(note(id = "note", title = "Shopping", content = "Buy milk")),
            diary = listOf(diary(id = "diary", title = "Monday", content = "Quiet day")),
            bookmarks = listOf(
                bookmark(
                    id = "link",
                    title = "Reference",
                    url = "https://example.com/moon"
                )
            )
        )

        val notes = repository.observeItems(ContentType.NOTE, "milk").first()
        val links = repository.observeItems(query = "moon").first()

        assertEquals(listOf("note"), notes.map { it.sourceId })
        assertEquals(listOf("link"), links.map { it.sourceId })
    }

    private fun repository(
        notes: List<Note>,
        diary: List<DiaryEntry>,
        bookmarks: List<Bookmark>
    ) = DefaultContentLibraryRepository(
        noteRepository = FakeNoteRepository(notes),
        diaryRepository = FakeDiaryRepository(diary),
        bookmarkRepository = FakeBookmarkRepository(bookmarks)
    )
}

private fun note(
    id: String,
    title: String = "",
    content: String = "",
    updatedDate: Long = 0L
) = Note(
    id = id,
    title = title,
    content = content,
    createdDate = updatedDate,
    updatedDate = updatedDate
)

private fun diary(
    id: String,
    title: String = "",
    content: String = "",
    updatedDate: Long = 0L
) = DiaryEntry(
    id = id,
    title = title,
    content = content,
    createdDate = updatedDate,
    updatedDate = updatedDate,
    mood = Mood.GOOD
)

private fun bookmark(
    id: String,
    title: String = "",
    url: String = "https://example.com",
    updatedDate: Long = 0L
) = Bookmark(
    id = id,
    title = title,
    url = url,
    createdDate = updatedDate,
    updatedDate = updatedDate
)

private class FakeNoteRepository(notes: List<Note>) : NoteRepository {
    private val items = MutableStateFlow(notes)
    override fun getAllFolderlessNotes(): Flow<List<Note>> = items
    override fun getAllNotes(): Flow<List<Note>> = items
    override suspend fun getNote(id: String): Note? = items.value.firstOrNull { it.id == id }
    override suspend fun searchNotes(query: String): List<Note> = emptyList()
    override fun getNotesByFolder(folderId: String): Flow<List<Note>> = items
    override suspend fun upsertNote(note: Note, currentFolderId: String?): String = note.id
    override suspend fun upsertNotes(notes: List<Note>): List<String> = notes.map(Note::id)
    override suspend fun deleteNote(note: Note) = Unit
    override suspend fun insertNoteFolder(folderName: String): String = folderName
    override suspend fun updateNoteFolder(folder: NoteFolder) = Unit
    override suspend fun deleteNoteFolder(folder: NoteFolder) = Unit
    override fun getAllNoteFolders(): Flow<List<NoteFolder>> = MutableStateFlow(emptyList())
    override suspend fun getNoteFolder(folderId: String): NoteFolder? = null
    override suspend fun searchFoldersByName(name: String): List<NoteFolder> = emptyList()
}

private class FakeDiaryRepository(entries: List<DiaryEntry>) : DiaryRepository {
    private val items = MutableStateFlow(entries)
    override fun getAllEntries(): Flow<List<DiaryEntry>> = items
    override suspend fun getEntry(id: String): DiaryEntry? = items.value.firstOrNull { it.id == id }
    override suspend fun searchEntries(title: String): List<DiaryEntry> = emptyList()
    override suspend fun addEntry(diary: DiaryEntry) = Unit
    override suspend fun updateEntry(diary: DiaryEntry) = Unit
    override suspend fun deleteEntry(diary: DiaryEntry) = Unit
}

private class FakeBookmarkRepository(bookmarks: List<Bookmark>) : BookmarkRepository {
    private val items = MutableStateFlow(bookmarks)
    override fun getAllBookmarks(): Flow<List<Bookmark>> = items
    override suspend fun getBookmark(id: String): Bookmark = items.value.first { it.id == id }
    override suspend fun searchBookmarks(query: String): List<Bookmark> = emptyList()
    override suspend fun addBookmark(bookmark: Bookmark): Long = 0L
    override suspend fun deleteBookmark(bookmark: Bookmark) = Unit
    override suspend fun updateBookmark(bookmark: Bookmark) = Unit
}
