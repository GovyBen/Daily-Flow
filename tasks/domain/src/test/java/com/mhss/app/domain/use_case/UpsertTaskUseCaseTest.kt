package com.mhss.app.domain.use_case

import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.repository.AlarmRepository
import com.mhss.app.alarm.repository.AlarmScheduler
import com.mhss.app.alarm.use_case.DeleteAlarmUseCase
import com.mhss.app.alarm.use_case.UpsertAlarmUseCase
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import com.mhss.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertTaskUseCaseTest {

    @Test
    fun newFutureTaskUsesGeneratedAlarmIdForSchedulingAndTaskLink() = runBlocking {
        val fixture = Fixture(nextAlarmId = 41)
        val task = task(dueDate = futureTime())

        val result = fixture.useCase(task)

        assertTrue(result)
        assertEquals(41, fixture.taskRepository.savedTask?.alarmId)
        assertEquals(listOf(Alarm(41, task.dueDate)), fixture.alarmScheduler.scheduled)
        assertEquals(listOf(Alarm(0, task.dueDate)), fixture.alarmRepository.upserted)
        assertEquals(1, fixture.widgetUpdater.updateCount)
    }

    @Test
    fun editingFutureTaskReusesExistingAlarmIdAndRequestCode() = runBlocking {
        val fixture = Fixture()
        val previous = task(dueDate = futureTime(), alarmId = 73)
        val updated = previous.copy(dueDate = previous.dueDate + 60_000)

        val result = fixture.useCase(updated, previous)

        assertTrue(result)
        assertEquals(73, fixture.taskRepository.savedTask?.alarmId)
        assertEquals(listOf(Alarm(73, updated.dueDate)), fixture.alarmScheduler.scheduled)
        assertEquals(listOf(Alarm(73, updated.dueDate)), fixture.alarmRepository.upserted)
    }

    @Test
    fun removingDueDateCancelsAlarmAndClearsTaskLink() = runBlocking {
        val fixture = Fixture()
        val previous = task(dueDate = futureTime(), alarmId = 19)

        val result = fixture.useCase(previous.copy(dueDate = 0L), previous)

        assertTrue(result)
        assertEquals(listOf(19), fixture.alarmScheduler.cancelled)
        assertEquals(listOf(19), fixture.alarmRepository.deletedIds)
        assertNull(fixture.taskRepository.savedTask?.alarmId)
    }

    @Test
    fun completingTaskCancelsItsOutstandingAlarm() = runBlocking {
        val fixture = Fixture()
        val previous = task(dueDate = futureTime(), alarmId = 23)

        val result = fixture.useCase(previous.copy(isCompleted = true), previous)

        assertTrue(result)
        assertEquals(listOf(23), fixture.alarmScheduler.cancelled)
        assertNull(fixture.taskRepository.savedTask?.alarmId)
    }

    @Test
    fun deniedExactAlarmPermissionKeepsTaskButReportsUnscheduledReminder() = runBlocking {
        val fixture = Fixture(canScheduleExact = false)
        val task = task(dueDate = futureTime())

        val result = fixture.useCase(task)

        assertFalse(result)
        assertNull(fixture.taskRepository.savedTask?.alarmId)
        assertTrue(fixture.alarmRepository.upserted.isEmpty())
        assertTrue(fixture.alarmScheduler.scheduled.isEmpty())
    }

    private class Fixture(
        canScheduleExact: Boolean = true,
        nextAlarmId: Int = 100
    ) {
        val taskRepository = FakeTaskRepository()
        val alarmRepository = FakeAlarmRepository(nextAlarmId)
        val alarmScheduler = FakeAlarmScheduler(canScheduleExact)
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = UpsertTaskUseCase(
            tasksRepository = taskRepository,
            upsertAlarm = UpsertAlarmUseCase(alarmRepository, alarmScheduler),
            deleteAlarmUseCase = DeleteAlarmUseCase(alarmRepository, alarmScheduler),
            widgetUpdater = widgetUpdater
        )
    }

    private class FakeAlarmRepository(
        private val nextAlarmId: Int
    ) : AlarmRepository {
        val upserted = mutableListOf<Alarm>()
        val deletedIds = mutableListOf<Int>()

        override suspend fun getAlarms(): List<Alarm> = upserted

        override suspend fun upsertAlarm(alarm: Alarm): Long {
            upserted += alarm
            return if (alarm.id == 0) nextAlarmId.toLong() else -1L
        }

        override suspend fun deleteAlarm(alarm: Alarm) {
            deletedIds += alarm.id
        }

        override suspend fun deleteAlarm(id: Int) {
            deletedIds += id
        }
    }

    private class FakeAlarmScheduler(
        private val canScheduleExact: Boolean
    ) : AlarmScheduler {
        val scheduled = mutableListOf<Alarm>()
        val cancelled = mutableListOf<Int>()

        override fun scheduleAlarm(alarm: Alarm) {
            scheduled += alarm
        }

        override fun cancelAlarm(schedulerId: Int) {
            cancelled += schedulerId
        }

        override fun canScheduleExactAlarms(): Boolean = canScheduleExact
    }

    private class FakeTaskRepository : TaskRepository {
        var savedTask: Task? = null

        override fun getAllTasks(): Flow<List<Task>> = flowOf(listOfNotNull(savedTask))

        override suspend fun getTaskById(id: String): Task? = savedTask?.takeIf { it.id == id }

        override suspend fun getTaskByAlarm(alarmId: Int): Task? =
            savedTask?.takeIf { it.alarmId == alarmId }

        override fun searchTasks(title: String): Flow<List<Task>> =
            flowOf(listOfNotNull(savedTask).filter { title in it.title })

        override suspend fun upsertTask(task: Task) {
            savedTask = task
        }

        override suspend fun upsertTasks(tasks: List<Task>) {
            savedTask = tasks.lastOrNull()
        }

        override suspend fun updateTask(task: Task) {
            savedTask = task
        }

        override suspend fun completeTask(id: String, completed: Boolean) {
            savedTask = savedTask?.takeIf { it.id == id }?.copy(isCompleted = completed)
        }

        override suspend fun deleteTask(task: Task) {
            if (savedTask == task) savedTask = null
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var updateCount = 0

        override suspend fun updateAll(type: WidgetUpdater.WidgetType) {
            updateCount++
        }
    }

    private companion object {
        fun futureTime(): Long = System.currentTimeMillis() + 3_600_000

        fun task(
            dueDate: Long,
            alarmId: Int? = null
        ) = Task(
            title = "Reminder baseline",
            dueDate = dueDate,
            alarmId = alarmId,
            id = "task-1"
        )
    }
}
