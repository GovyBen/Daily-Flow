package com.mhss.app.domain.model

/**
 * Represents an AI-generated write proposal that must be confirmed by the user
 * before any data is modified. All write tools return proposals instead of
 * executing directly (DF-502).
 */
sealed interface ActionProposal {
    val proposalId: String
    val sourceText: String
    val summary: String
}

data class CreateTaskProposal(
    override val proposalId: String,
    override val sourceText: String,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.LOW,
    val dueDateEpochMilli: Long = 0L,
    val subTasks: List<SubTaskInput> = emptyList(),
    val recurring: Boolean = false,
    val frequency: TaskFrequency = TaskFrequency.DAILY,
    val frequencyAmount: Int = 1,
    val missingFields: List<String> = emptyList()
) : ActionProposal {
    override val summary: String get() = "Create task: $title"
}

data class CreateCalendarEventProposal(
    override val proposalId: String,
    override val sourceText: String,
    val title: String,
    val description: String? = null,
    val startEpochMilli: Long,
    val endEpochMilli: Long,
    val location: String? = null,
    val allDay: Boolean = false,
    val calendarId: Long = 0,
    val recurring: Boolean = false,
    val frequency: CalendarEventFrequency = CalendarEventFrequency.NEVER,
    val interval: Int = 1,
    val weekDays: List<String> = emptyList(),
    val missingFields: List<String> = emptyList()
) : ActionProposal {
    override val summary: String get() = "Create event: $title"
}

data class CreateDiaryEntryProposal(
    override val proposalId: String,
    override val sourceText: String,
    val title: String,
    val content: String,
    val moodValue: Int = 2, // Default to NEUTRAL=2; matches Mood enum ordinal
    val missingFields: List<String> = emptyList()
) : ActionProposal {
    override val summary: String get() = "Create diary entry: $title"
}

data class CreateRecordSessionProposal(
    override val proposalId: String,
    override val sourceText: String,
    val templateId: String,
    val templateName: String = "",
    val occurredAtEpochMilli: Long,
    val note: String? = null,
    val fieldValues: List<ProposedFieldValue> = emptyList(),
    val missingFields: List<String> = emptyList()
) : ActionProposal {
    override val summary: String get() = "Record entry in: ${templateName.ifBlank { templateId }}"
}

data class CompleteTaskProposal(
    override val proposalId: String,
    override val sourceText: String,
    val taskId: String,
    val taskTitle: String = "",
    val completed: Boolean = true
) : ActionProposal {
    override val summary: String get() = if (completed) "Complete: $taskTitle" else "Reopen: $taskTitle"
}

data class ClarificationRequest(
    override val proposalId: String,
    override val sourceText: String,
    val question: String,
    val missingField: String,
    val options: List<String> = emptyList()
) : ActionProposal {
    override val summary: String get() = question
    val needsClarification: Boolean = true
}

data class ProposedFieldValue(
    val trackerId: String,
    val trackerName: String = "",
    val value: Double? = null,
    val label: String? = null,
    val optionId: String? = null
)

data class SubTaskInput(
    val title: String,
    val isCompleted: Boolean = false
)
