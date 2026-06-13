package com.mhss.app.mybrain.content.domain

enum class ContentType {
    NOTE,
    DIARY,
    LINK
}

data class ContentItem(
    val sourceId: String,
    val type: ContentType,
    val title: String,
    val preview: String,
    val createdDate: Long,
    val updatedDate: Long,
    val pinned: Boolean = false,
    val folderId: String? = null,
    val url: String? = null,
    val moodValue: Int? = null
) {
    val stableId: String
        get() = "${type.name}:$sourceId"

    val sortDate: Long
        get() = maxOf(createdDate, updatedDate)
}
