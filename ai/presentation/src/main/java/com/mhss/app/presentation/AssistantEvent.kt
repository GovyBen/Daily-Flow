package com.mhss.app.presentation

import com.mhss.app.domain.model.AiMessageAttachment

sealed interface AssistantEvent {
    data class SendMessage(
        val content: String,
        val attachments: List<AiMessageAttachment>
    ): AssistantEvent
    data class SearchNotes(val query: String) : AssistantEvent
    data class SearchTasks(val query: String) : AssistantEvent
    data class AddAttachmentNote(val id: String): AssistantEvent
    data class AddAttachmentTask(val id: String): AssistantEvent
    data object AddAttachmentEvents: AssistantEvent
    data class RemoveAttachment(val index: Int): AssistantEvent
    data object CancelMessage: AssistantEvent
    data object ConfirmProposal: AssistantEvent
    data object DismissProposal: AssistantEvent
}
