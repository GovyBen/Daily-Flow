package com.mhss.app.mybrain.content.domain

import kotlinx.coroutines.flow.Flow

interface ContentLibraryRepository {
    fun observeItems(
        type: ContentType? = null,
        query: String = ""
    ): Flow<List<ContentItem>>
}
