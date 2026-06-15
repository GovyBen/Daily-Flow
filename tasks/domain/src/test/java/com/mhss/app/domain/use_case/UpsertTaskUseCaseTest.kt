package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.repository.ReminderScheduler
import com.mhss.app.alarm.use_case.CancelReminderUseCase
import com.mhss.app.alarm.use_case.RescheduleTargetRemindersUseCase
import com.mhss.app.alarm.use_case.ScheduleReminderUseCase
import com.mhss.app.alarm.repository.ReminderTargetTimeResolver
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import com.mhss.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertTaskUseCaseTest {

    @Test
    fun newFutureTaskCreatesDefaultAbsoluteReminder() = runBlocking {
        val fixture = Fixture()
        val task = task(dueDate = futureTime())

        fixture.useCase(task)

        val saved = fixture.taskRepository.savedTask
        assertNull(saved?.alarmId) // alarmId cleared after migration to reminders
        assertEquals(1, fixture.reminderRepository.savedReminders.size)
        val reminder = fixture.reminderRepository.savedReminders[0]
        assertEquals(ReminderTargetType.TASK, reminder.targetType)
        assertEquals(task.id, reminder.targetId)
        assertEquals(task.dueDate, reminder.absoluteTriggerAt)
        assertEquals(1, fixture.widgetUpdater.updateCount)
        assertEquals(1, fixture.reminderScheduler.scheduledIds.size)
    }

    @Test
    fun editingFutureDueDateReschedulesExistingReminder() = runBlocking {
        val fixture = Fixture()
        val previous = task(dueDate = futureTime())
        // First save creates the default reminder
        fixture.useCase(previous)
        fixture.reminderScheduler.scheduledIds.clear()

        // Edit due date
        val newDueDate = previous.dueDate + 3_600_000
        val updated = previous.copy(dueDate = newDueDate)
        fixture.useCase(updated, previous)

        // The existing reminder should be rescheduled
        assertTrue(fixture.reminderScheduler.scheduledIds.isNotEmpty())
    }

    @Test
    fun removingDueDateCancelsAllReminders() = runBlocking {
        val fixture = Fixture()
        val previous = task(dueDate = futureTime())
        fixture.useCase(previous) // Create reminder
        fixture.reminderScheduler.scheduledIds.clear()

        fixture.useCase(previous.copy(dueDate = 0L), previous)

        // Scheduler should have received cancel calls
        assertTrue(fixture.reminderScheduler.cancelledIds.isNotEmpty())
        // Reminders for this task should be in terminal state
        val reminders = fixture.reminderRepository.getByTarget(
            ReminderTargetType.TASK, previous.id
        )
        assertTrue(reminders.all { it.status == ReminderStatus.CANCELLED })
    }

    @Test
    fun completingTaskCancelsAllReminders() = runBlocking {
        val fixture = Fixture()
        val previous = task(dueDate = futureTime())
        fixture.useCase(previous) // Create reminder
        fixture.reminderScheduler.scheduledIds.clear()

        fixture.useCase(previous.copy(isCompleted = true), previous)

        assertTrue(fixture.reminderScheduler.cancelledIds.isNotEmpty())
    }

    @Test
    fun recurringTaskCompletionAdvancesDueDateAndReschedules() = runBlocking {
        val fixture = Fixture()
        val previous = task(
            dueDate = futureTime(),
            recurring = true
        )
        fixture.useCase(previous) // initial
        fixture.reminderScheduler.scheduledIds.clear()
        fixture.reminderRepository.savedReminders.clear()

        val completed = previous.copy(isCompleted = true)
        fixture.useCase(completed, previous) // complete + roll forward

        // Should have created a new reminder at the rolled-forward due date
        val saved = fixture.reminderRepository.savedReminders
        assertTrue(saved.isNotEmpty())
        val newTask = fixture.taskRepository.savedTask
        assertTrue(newTask?.dueDate ?: 0 > previous.dueDate)
    }

    @Test
    fun schedulingFailureDoesNotCrashAndTaskIsStillSaved() = runBlocking {
        val fixture = Fixture(throwOnSchedule = true)
        val task = task(dueDate = futureTime())

        fixture.useCase(task)

        // Task should still be saved even if scheduling failed
        assertTrue(fixture.taskRepository.savedTask != null)
        assertNull(fixture.taskRepository.savedTask?.alarmId)
    }

    private class Fixture(
        throwOnSchedule: Boolean = false
    ) {
        val taskRepository = FakeTaskRepository()
        val reminderRepository = FakeReminderRepository()
        val reminderScheduler = FakeReminderScheduler(throwOnSchedule)
        val widgetUpdater = FakeWidgetUpdater()

        val scheduleReminder = ScheduleReminderUseCase(
            reminderRepository, reminderScheduler,
            FakeTargetTimeResolver()
        )
        val cancelReminder = CancelReminderUseCase(
            reminderRepository, reminderScheduler
        )
        val rescheduleReminders = RescheduleTargetRemindersUseCase(
            reminderRepository, scheduleReminder
        )

        val useCase = UpsertTaskUseCase(
            taskRepository = taskRepository,
            reminderRepository = reminderRepository,
            scheduleReminder = scheduleReminder,
            cancelReminder = cancelReminder,
            rescheduleTargetReminders = rescheduleReminders,
            widgetUpdater = widgetUpdater
        )
    }

    private class FakeTaskRepository : TaskRepository {
        var savedTask: Task? = null

        override fun getAllTasks(): Flow<List<Task>> = flowOf(listOfNotNull(savedTask))
        override suspend fun getTaskById(id: String): Task? = savedTask?.takeIf { it.id == id }
        override suspend fun getTaskByAlarm(alarmId: Int): Task? = null
        override fun searchTasks(title: String): Flow<List<Task>> =
            flowOf(listOfNotNull(savedTask).filter { title in it.title })
        override suspend fun upsertTask(task: Task) { savedTask = task }
        override suspend fun upsertTasks(tasks: List<Task>) { savedTask = tasks.lastOrNull() }
        override suspend fun updateTask(task: Task) { savedTask = task }
        override suspend fun completeTask(id: String, completed: Boolean) {
            savedTask = savedTask?.takeIf { it.id == id }?.copy(isCompleted = completed)
        }
        override suspend fun deleteTask(task: Task) { if (savedTask == task) savedTask = null }
    }

    private class FakeReminderRepository : ReminderRepository {
        val savedReminders = mutableListOf<Reminder>()
        val cancelledReminders = mutableListOf<Reminder>()
        var nextId = 1L

        override suspend fun getAll(): List<Reminder> = savedReminders.toList()
        override suspend fun getById(id: Long): Reminder? = savedReminders.find { it.id == id }
        override suspend fun getByTarget(
            targetType: ReminderTargetType, targetId: String
        ): List<Reminder> = savedReminders.filter {
            it.targetType == targetType && it.targetId == targetId
        }
        override suspend fun getEnabled(): List<Reminder> =
            savedReminders.filter { it.enabled }
        override suspend fun save(reminder: Reminder): Reminder {
            val saved = if (reminder.id == 0L) {
                reminder.copy(id = nextId++)
            } else {
                savedReminders.removeAll { it.id == reminder.id }
                reminder
            }
            savedReminders.add(saved)
            return saved
        }
        override suspend fun delete(id: Long) {
            savedReminders.removeAll { it.id == id }
        }
    }

    private class FakeReminderScheduler(
        private val throwOnSchedule: Boolean = false
    ) : ReminderScheduler {
        val scheduledIds = mutableListOf<Long>()
        val cancelledIds = mutableListOf<Long>()

        override fun scheduleReminder(reminderId: Long, triggerAtEpochMilli: Long) {
            if (throwOnSchedule) error("Platform scheduling failed")
            scheduledIds += reminderId
        }

        override fun cancelReminder(reminderId: Long) {
            cancelledIds += reminderId
        }
    }

    private class FakeTargetTimeResolver : ReminderTargetTimeResolver {
        override suspend fun resolveTargetTime(
            targetType: ReminderTargetType,
            targetId: String
        ): Long? = null
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var updateCount = 0
        override suspend fun updateAll(type: WidgetUpdater.WidgetType) { updateCount++ }
    }

    private companion object {
        fun futureTime(): Long = System.currentTimeMillis() + 3_600_000
        fun task(
            dueDate: Long,
            alarmId: Int? = null,
            recurring: Boolean = false
        ) = Task(
            title = "DF-405 reminder test",
            dueDate = dueDate,
            alarmId = alarmId,
            recurring = recurring,
            id = "task-test-1"
        )
    }
}
