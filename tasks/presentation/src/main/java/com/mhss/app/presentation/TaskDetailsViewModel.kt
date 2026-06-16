package com.mhss.app.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.use_case.ScheduleReminderUseCase
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.use_case.CanScheduleAlarmsUseCase
import com.mhss.app.domain.use_case.DeleteTaskUseCase
import com.mhss.app.domain.use_case.GetTaskByIdUseCase
import com.mhss.app.domain.use_case.UpsertTaskUseCase
import com.mhss.app.ui.R
import com.mhss.app.ui.components.reminders.ReminderDraft
import com.mhss.app.ui.snackbar.showSnackbar
import com.mhss.app.util.date.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class TaskDetailsViewModel(
    private val getTask: GetTaskByIdUseCase,
    private val upsertTask: UpsertTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val canScheduleAlarms: CanScheduleAlarmsUseCase,
    private val reminderRepository: ReminderRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val cancelReminder: CancelReminderUseCase,
    @Named("applicationScope") private val applicationScope: CoroutineScope,
    taskId: String
) : ViewModel() {

    private val _taskDetailsUiState = MutableStateFlow(TaskDetailsUiState())
    val taskDetailsUiState = _taskDetailsUiState.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders = _reminders.asStateFlow()

    init {
        viewModelScope.launch {
            val task = getTask(taskId)
            if (taskId.isNotBlank() && task == null) {
                taskDetailsUiState.value.snackbarHostState.showSnackbar(R.string.error_item_not_found)
            }
            _taskDetailsUiState.update { it.copy(task = task) }
        }
        observeReminders(taskId)
    }

    private fun observeReminders(taskId: String) {
        viewModelScope.launch {
            if (taskId.isBlank()) return@launch
            _reminders.value = reminderRepository.getByTarget(
                ReminderTargetType.TASK, taskId
            )
        }
    }

    fun onRemindersChanged(drafts: List<ReminderDraft>) {
        val task = _taskDetailsUiState.value.task ?: return
        viewModelScope.launch {
            val existing = reminderRepository.getByTarget(ReminderTargetType.TASK, task.id)
            val now = now()

            // Cancel removals
            val existingIds = existing.map { it.id }.toSet()
            val keptIds = drafts.mapNotNull { it.id.takeIf { id -> id > 0 } }.toSet()
            for (r in existing) {
                if (r.id !in keptIds && !r.status.isTerminal()) {
                    cancelReminder(r.id, now)
                }
            }

            // Add/update drafts
            for (draft in drafts) {
                if (draft.id > 0 && draft.id in existingIds) continue // unchanged
                val reminder = Reminder(
                    targetType = ReminderTargetType.TASK,
                    targetId = task.id,
                    absoluteTriggerAt = draft.absoluteTriggerAt,
                    relativeOffsetMinutes = draft.relativeOffsetMinutes,
                    enabled = true,
                    status = ReminderStatus.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
                val saved = reminderRepository.save(reminder)
                scheduleReminder(saved.id, now)
            }

            observeReminders(task.id)
        }
    }

    fun onEvent(event: TaskDetailsEvent) {
        when (event) {
            TaskDetailsEvent.ErrorDisplayed -> {
                _taskDetailsUiState.update { it.copy(alarmError = false) }
            }
            is TaskDetailsEvent.ScreenOnStop -> applicationScope.launch {
                if (!taskDetailsUiState.value.navigateUp) {
                    if (taskChanged(taskDetailsUiState.value.task!!, event.task)) {
                        val newTask = taskDetailsUiState.value.task!!.copy(
                            title = event.task.title.ifBlank { "Untitled" },
                            description = event.task.description,
                            dueDate = event.task.dueDate,
                            priority = event.task.priority,
                            subTasks = event.task.subTasks,
                            recurring = event.task.recurring,
                            frequency = event.task.frequency,
                            frequencyAmount = event.task.frequencyAmount,
                            isCompleted = event.task.isCompleted,
                            updatedDate = now()
                        )
                        upsertTask(task = newTask, previousTask = taskDetailsUiState.value.task)
                        _taskDetailsUiState.update { it.copy(task = newTask) }
                    }
                }
            }
            is TaskDetailsEvent.DeleteTask -> viewModelScope.launch {
                deleteTask(taskDetailsUiState.value.task!!)
                _taskDetailsUiState.update { it.copy(navigateUp = true) }
            }
            TaskDetailsEvent.DueDateEnabled -> {
                if (!canScheduleAlarms()) {
                    _taskDetailsUiState.update { it.copy(alarmError = true) }
                }
            }
        }
    }

    data class TaskDetailsUiState(
        val task: Task? = null,
        val navigateUp: Boolean = false,
        val alarmError: Boolean = false,
        val snackbarHostState: SnackbarHostState = SnackbarHostState()
    )

    private fun taskChanged(task: Task, newTask: Task): Boolean =
        task.title != newTask.title ||
        task.description != newTask.description ||
        task.dueDate != newTask.dueDate ||
        task.isCompleted != newTask.isCompleted ||
        task.priority != newTask.priority ||
        task.subTasks != newTask.subTasks ||
        task.recurring != newTask.recurring ||
        task.frequency != newTask.frequency ||
        task.frequencyAmount != newTask.frequencyAmount

    private fun ReminderStatus.isTerminal(): Boolean =
        this == ReminderStatus.DELIVERED ||
        this == ReminderStatus.CANCELLED ||
        this == ReminderStatus.MISSED
}
