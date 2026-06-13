package com.mhss.app.mybrain.content.data

import com.mhss.app.domain.model.Bookmark
import com.mhss.app.domain.model.DiaryEntry
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.repository.BookmarkRepository
import com.mhss.app.domain.repository.DiaryRepository
import com.mhss.app.domain.repository.NoteRepository
import com.mhss.app.mybrain.content.domain.ContentItem
import com.mhss.app.mybrain.content.domain.ContentLibraryRepository
import com.mhss.app.mybrain.content.domain.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class DefaultContentLibraryRepository(
    private val noteRepository: NoteRepository,
    private val diaryRepository: DiaryRepository,
    private val bookmarkRepository: BookmarkRepository
) : ContentLibraryRepository {

    override fun observeItems(
        type: ContentType?,
        query: String
    ): Flow<List<ContentItem>> {
        return combine(
            noteRepository.getAllNotes(),
            diaryRepository.getAllEntries(),
            bookmarkRepository.getAllBookmarks()
        ) { notes, diaryEntries, bookmarks ->
            buildList {
                addAll(notes.map(Note::toContentItem))
                addAll(diaryEntries.map(DiaryEntry::toContentItem))
                addAll(bookmarks.map(Bookmark::toContentItem))
            }
        }.map { items ->
            val normalizedQuery = query.trim()
            items.asSequence()
                .filter { type == null || it.type == type }
                .filter { normalizedQuery.isBlank() || it.matches(normalizedQuery) }
                .sortedWith(
                    compareByDescending<ContentItem> { it.pinned }
                        .thenByDescending(ContentItem::sortDate)
                )
                .toList()
        }
    }
}

private fun Note.toContentItem() = ContentItem(
    sourceId = id,
    type = ContentType.NOTE,
    title = title,
    preview = content.toPreview(),
    createdDate = createdDate,
    updatedDate = updatedDate,
    pinned = pinned,
    folderId = folderId
)

private fun DiaryEntry.toContentItem() = ContentItem(
    sourceId = id,
    type = ContentType.DIARY,
    title = title,
    preview = content.toPreview(),
    createdDate = createdDate,
    updatedDate = updatedDate,
    moodValue = mood.value
)

private fun Bookmark.toContentItem() = ContentItem(
    sourceId = id,
    type = ContentType.LINK,
    title = title,
    preview = description.ifBlank { url }.toPreview(),
    createdDate = createdDate,
    updatedDate = updatedDate,
    url = url
)

private fun ContentItem.matches(query: String): Boolean {
    return title.contains(query, ignoreCase = true) ||
        preview.contains(query, ignoreCase = true) ||
        url.orEmpty().contains(query, ignoreCase = true)
}

private fun String.toPreview(): String {
    return lineSequence()
        .map(String::trim)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
}
