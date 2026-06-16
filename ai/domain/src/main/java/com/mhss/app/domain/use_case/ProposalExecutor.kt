package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.*
import org.koin.core.annotation.Single

/**
 * Executes confirmed ActionProposals (DF-507). The ONLY component that converts
 * proposals into persistent writes. Each proposal can only be executed once.
 */
@Single
class ProposalExecutor(
    private val upsertTask: UpsertTaskUseCase,
    private val addCalendarEvent: AddCalendarEventUseCase,
    private val updateTaskCompleted: UpdateTaskCompletedUseCase,
    private val getTaskById: GetTaskByIdUseCase
) {
    private val executedIds = mutableSetOf<String>()

    suspend fun execute(proposal: ActionProposal): ProposalExecutionResult {
        if (proposal.proposalId in executedIds) {
            return ProposalExecutionResult(proposal.proposalId, false, "Already executed")
        }
        val result = try {
            when (proposal) {
                is CreateTaskProposal -> executeCreateTask(proposal)
                is CreateCalendarEventProposal -> executeCreateEvent(proposal)
                is CompleteTaskProposal -> executeCompleteTask(proposal)
                else -> ProposalExecutionResult(proposal.proposalId, false, "Not supported yet: ${proposal::class.simpleName}")
            }
        } catch (e: Exception) {
            ProposalExecutionResult(proposal.proposalId, false, e.message ?: "Error")
        }
        if (result.success) executedIds.add(proposal.proposalId)
        return result
    }

    private suspend fun executeCreateTask(p: CreateTaskProposal): ProposalExecutionResult {
        val task = Task(id = kotlin.uuid.Uuid.random().toString(),
            title = p.title, description = p.description, priority = p.priority,
            dueDate = p.dueDateEpochMilli,
            subTasks = p.subTasks.map { SubTask(it.title, it.isCompleted) },
            recurring = p.recurring, frequency = p.frequency, frequencyAmount = p.frequencyAmount,
            createdDate = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            updatedDate = kotlin.time.Clock.System.now().toEpochMilliseconds())
        upsertTask(task)
        return ProposalExecutionResult(p.proposalId, true, "Task created: ${task.id}")
    }

    private suspend fun executeCreateEvent(p: CreateCalendarEventProposal): ProposalExecutionResult {
        val event = com.mhss.app.domain.model.CalendarEvent(
            id = 0, title = p.title, description = p.description,
            start = p.startEpochMilli, end = p.endEpochMilli, location = p.location,
            allDay = p.allDay, calendarId = p.calendarId, recurring = p.recurring,
            frequency = p.frequency, interval = p.interval,
            weekDays = emptySet()
        )
        val id = addCalendarEvent(event)
        return if (id != null && id > 0) ProposalExecutionResult(p.proposalId, true, "Event created: $id")
        else ProposalExecutionResult(p.proposalId, false, "Failed to create event")
    }

    private suspend fun executeCompleteTask(p: CompleteTaskProposal): ProposalExecutionResult {
        val task = getTaskById(p.taskId)
            ?: return ProposalExecutionResult(p.proposalId, false, "Task not found")
        updateTaskCompleted(task, p.completed)
        return ProposalExecutionResult(p.proposalId, true,
            "Task ${if (p.completed) "completed" else "reopened"}")
    }
}

data class ProposalExecutionResult(
    val proposalId: String,
    val success: Boolean,
    val message: String
)
