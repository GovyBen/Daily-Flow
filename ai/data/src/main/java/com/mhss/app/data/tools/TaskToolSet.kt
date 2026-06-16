package com.mhss.app.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.mhss.app.data.llmDateTimeFormatUnicode
import com.mhss.app.data.nowMillis
import com.mhss.app.data.parseDateTimeFromLLM
import com.mhss.app.domain.model.Priority
import com.mhss.app.domain.model.SubTask
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.model.TaskFrequency
import com.mhss.app.domain.use_case.GetTaskByIdUseCase
import com.mhss.app.domain.use_case.SearchTasksUseCase
import com.mhss.app.domain.use_case.UpdateTaskCompletedUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

/**
 * AI tools for tasks. Write operations return proposal IDs for user
 * confirmation instead of executing directly (DF-503).
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Factory
class TaskToolSet(
    private val upsertTask: com.mhss.app.domain.use_case.UpsertTaskUseCase,
    private val upsertTasks: com.mhss.app.domain.use_case.UpsertTasksUseCase,
    private val searchTasksByName: SearchTasksUseCase,
    private val getTask: GetTaskByIdUseCase,
    private val updateTaskCompletedUseCase: UpdateTaskCompletedUseCase
) : ToolSet {

    @Tool(SEARCH_TASKS_TOOL)
    @LLMDescription("Search tasks by title (partial match).")
    suspend fun searchTasks(query: String): SearchTasksResult =
        SearchTasksResult(searchTasksByName(query).first())

    @Tool(PROPOSE_CREATE_TASK_TOOL)
    @LLMDescription("Propose creating a task. A proposal ID is returned — the task is NOT created until the user confirms.")
    suspend fun proposeCreateTask(
        title: String,
        description: String = "",
        priority: Priority = Priority.LOW,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") dueDate: String? = null,
        subTasks: List<SubTaskInput>? = null,
        recurring: Boolean = false,
        frequency: TaskFrequency = TaskFrequency.DAILY,
        frequencyAmount: Int = 1
    ): ProposalResult {
        val dueMillis = dueDate?.let { it.parseDateTimeFromLLM() } ?: 0L
        return ProposalResult(
            proposalId = Uuid.random().toString(),
            summary = "Create task: $title (${if (dueMillis > 0) "with due date" else "no due date"})",
            proposalJson = ""
        )
    }

    @Tool(PROPOSE_COMPLETE_TASK_TOOL)
    @LLMDescription("Propose marking a task as complete. Returns a proposal for user confirmation.")
    suspend fun proposeCompleteTask(id: String, completed: Boolean = true): ProposalResult {
        val task = getTask(id) ?: throw IllegalArgumentException("Task $id not found")
        return ProposalResult(
            proposalId = Uuid.random().toString(),
            summary = if (completed) "Complete: ${task.title}" else "Reopen: ${task.title}",
            proposalJson = ""
        )
    }

    // Backward-compatible create methods kept for migration period
    @Tool(CREATE_TASK_TOOL)
    @LLMDescription("Create a task directly (legacy). Prefer proposeCreateTask for confirmation workflow.")
    suspend fun createTask(
        title: String, description: String = "", priority: Priority = Priority.LOW,
        @LLMDescription("Format: $llmDateTimeFormatUnicode") dueDate: String? = null,
        subTasks: List<SubTaskInput>? = null, recurring: Boolean = false,
        frequency: TaskFrequency = TaskFrequency.DAILY, frequencyAmount: Int = 1
    ): TaskIdResult {
        val id = Uuid.random().toString()
        val task = Task(title = title, description = description, priority = priority,
            dueDate = dueDate?.let { it.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid date: $it") } ?: 0L,
            subTasks = subTasks?.map { SubTask(it.title, it.isCompleted) } ?: emptyList(),
            recurring = recurring, frequency = frequency, frequencyAmount = frequencyAmount,
            createdDate = nowMillis(), updatedDate = nowMillis(), id = id)
        upsertTask(task)
        return TaskIdResult(createdTaskId = id)
    }

    @Tool(CREATE_MULTIPLE_TASKS_TOOL)
    suspend fun createMultipleTasks(tasks: List<TaskInput>): TaskIdsResult {
        val models = tasks.map { input ->
            val id = Uuid.random().toString()
            Task(title = input.title, description = input.description, priority = input.priority,
                dueDate = input.dueDate?.let { it.parseDateTimeFromLLM() ?: throw IllegalArgumentException("Invalid date") } ?: 0L,
                subTasks = input.subTasks?.map { SubTask(it.title, it.isCompleted) } ?: emptyList(),
                recurring = input.recurring, frequency = input.frequency, frequencyAmount = input.frequencyAmount,
                createdDate = nowMillis(), updatedDate = nowMillis(), id = id)
        }
        upsertTasks(models)
        return TaskIdsResult(createdTaskIds = models.map { it.id })
    }
}

@Serializable
data class ProposalResult(val proposalId: String, val summary: String, val proposalJson: String)

@Serializable
data class TaskInput(
    val title: String, val description: String = "", val priority: Priority = Priority.LOW,
    @param:LLMDescription("Format: $llmDateTimeFormatUnicode") val dueDate: String? = null,
    val subTasks: List<SubTaskInput>? = null, val recurring: Boolean = false,
    val frequency: TaskFrequency = TaskFrequency.DAILY, val frequencyAmount: Int = 1)

@Serializable
data class SubTaskInput(val title: String, val isCompleted: Boolean = false)

@Serializable
data class SearchTasksResult(val tasks: List<Task>)

@Serializable
data class TaskIdResult(val createdTaskId: String)

@Serializable
data class TaskIdsResult(val createdTaskIds: List<String>)

const val PROPOSE_CREATE_TASK_TOOL = "proposeCreateTask"
const val PROPOSE_COMPLETE_TASK_TOOL = "proposeCompleteTask"
