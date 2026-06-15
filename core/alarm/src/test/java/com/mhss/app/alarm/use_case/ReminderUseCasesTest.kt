package com.mhss.app.alarm.use_case

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.alarm.repository.ReminderScheduler
import com.mhss.app.alarm.repository.ReminderTargetTimeResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUseCasesTest {

    @Test
    fun schedulesAbsoluteReminderAndPersistsStatus() = runBlocking {
        val fixture = Fixture(reminder(id = 1, absoluteTriggerAt = 20_000))

        val outcome = fixture.schedule(1, NOW)

        assertEquals(ReminderScheduleOutcome.SCHEDULED, outcome)
        assertEquals(mapOf(1L to 20_000L), fixture.scheduler.scheduled)
        assertEquals(ReminderStatus.SCHEDULED, fixture.repository.require(1).status)
    }

    @Test
    fun resolvesRelativeReminderFromTargetTime() = runBlocking {
        val fixture = Fixture(
            reminder(
                id = 2,
                absoluteTriggerAt = null,
                relativeOffsetMinutes = 15
            )
        )
        fixture.resolver.times[ReminderTargetType.TASK to TARGET_ID] = 1_000_000

        val outcome = fixture.schedule(2, NOW)

        assertEquals(ReminderScheduleOutcome.SCHEDULED, outcome)
        assertEquals(100_000L, fixture.scheduler.scheduled[2])
    }

    @Test
    fun repeatedSchedulingReplacesTheSameReminderRequest() = runBlocking {
        val fixture = Fixture(reminder(id = 3, absoluteTriggerAt = 20_000))

        fixture.schedule(3, NOW)
        fixture.repository.save(
            fixture.repository.require(3).copy(
                absoluteTriggerAt = 30_000,
                updatedAt = NOW + 1
            )
        )
        fixture.schedule(3, NOW + 1)

        assertEquals(1, fixture.scheduler.scheduled.size)
        assertEquals(30_000L, fixture.scheduler.scheduled[3])
    }

    @Test
    fun overdueReminderBecomesMissedAndCannotBeRescheduled() = runBlocking {
        val fixture = Fixture(reminder(id = 4, absoluteTriggerAt = NOW))

        assertEquals(ReminderScheduleOutcome.MISSED, fixture.schedule(4, NOW))
        val missedUpdatedAt = fixture.repository.require(4).updatedAt
        assertEquals(ReminderScheduleOutcome.CANCELLED, fixture.schedule(4, NOW + 1))

        val reminder = fixture.repository.require(4)
        assertFalse(reminder.enabled)
        assertEquals(ReminderStatus.MISSED, reminder.status)
        assertEquals(missedUpdatedAt, reminder.updatedAt)
        assertTrue(fixture.scheduler.scheduled.isEmpty())
    }

    @Test
    fun unavailableTargetStaysPendingForLaterReconciliation() = runBlocking {
        val fixture = Fixture(
            reminder(
                id = 5,
                absoluteTriggerAt = null,
                relativeOffsetMinutes = 5
            )
        )

        val outcome = fixture.schedule(5, NOW)

        assertEquals(ReminderScheduleOutcome.TARGET_UNAVAILABLE, outcome)
        val pendingUpdatedAt = fixture.repository.require(5).updatedAt
        assertEquals(
            ReminderScheduleOutcome.TARGET_UNAVAILABLE,
            fixture.schedule(5, NOW + 1)
        )
        assertTrue(fixture.repository.require(5).enabled)
        assertEquals(ReminderStatus.PENDING, fixture.repository.require(5).status)
        assertEquals(pendingUpdatedAt, fixture.repository.require(5).updatedAt)
    }

    @Test
    fun schedulingFailureLeavesReminderPendingForRetry() = runBlocking {
        val fixture = Fixture(reminder(id = 11, absoluteTriggerAt = 20_000))
        fixture.scheduler.throwOnSchedule = true

        val outcome = fixture.schedule(11, NOW)

        assertEquals(ReminderScheduleOutcome.FAILED, outcome)
        assertTrue(fixture.repository.require(11).enabled)
        assertEquals(ReminderStatus.PENDING, fixture.repository.require(11).status)
        assertTrue(fixture.scheduler.scheduled.isEmpty())
    }

    @Test
    fun cancellationIsIdempotent() = runBlocking {
        val fixture = Fixture(reminder(id = 12, absoluteTriggerAt = 20_000))
        val cancel = CancelReminderUseCase(fixture.repository, fixture.scheduler)

        val first = cancel(12, NOW)
        val second = cancel(12, NOW + 1)

        assertEquals(ReminderStatus.CANCELLED, first?.status)
        assertEquals(first?.updatedAt, second?.updatedAt)
        assertFalse(fixture.repository.require(12).enabled)
        assertEquals(ReminderStatus.CANCELLED, fixture.repository.require(12).status)
    }

    @Test
    fun targetRescheduleAndRestoreReportPerReminderOutcomes() = runBlocking {
        val fixture = Fixture(
            reminder(id = 6, absoluteTriggerAt = 20_000),
            reminder(id = 7, absoluteTriggerAt = 5_000),
            reminder(
                id = 8,
                targetId = "other",
                absoluteTriggerAt = 30_000
            )
        )

        val targetResult = fixture.rescheduleTarget(
            ReminderTargetType.TASK,
            TARGET_ID,
            NOW
        )
        val restoreResult = fixture.restore(NOW)

        assertEquals(
            mapOf(
                6L to ReminderScheduleOutcome.SCHEDULED,
                7L to ReminderScheduleOutcome.MISSED
            ),
            targetResult.outcomes
        )
        assertFalse(restoreResult.hasFailures)
        assertEquals(30_000L, fixture.scheduler.scheduled[8])
    }

    @Test
    fun staleTriggerRecalculatesWithoutDelivery() = runBlocking {
        val fixture = Fixture(reminder(id = 9, absoluteTriggerAt = 30_000))

        val delivered = fixture.trigger(
            reminderId = 9,
            expectedTriggerAtEpochMilli = 20_000,
            nowEpochMilli = 20_000
        )

        assertNull(delivered)
        assertEquals(30_000L, fixture.scheduler.scheduled[9])
        assertEquals(ReminderStatus.SCHEDULED, fixture.repository.require(9).status)
    }

    @Test
    fun dueTriggerIsConsumedOnlyOnce() = runBlocking {
        val fixture = Fixture(reminder(id = 10, absoluteTriggerAt = 20_000))

        val first = fixture.trigger(10, 20_000, 20_000)
        val second = fixture.trigger(10, 20_000, 20_001)

        assertEquals(ReminderStatus.DELIVERED, first?.status)
        assertNull(second)
        assertFalse(fixture.repository.require(10).enabled)
        assertEquals(ReminderStatus.DELIVERED, fixture.repository.require(10).status)
    }

    private class Fixture(vararg reminders: Reminder) {
        val repository = FakeReminderRepository(reminders.toList())
        val scheduler = FakeReminderScheduler()
        val resolver = FakeTargetTimeResolver()
        val schedule = ScheduleReminderUseCase(repository, scheduler, resolver)
        val rescheduleTarget = RescheduleTargetRemindersUseCase(repository, schedule)
        val reconcile = ReconcileScheduledRemindersUseCase(repository, schedule)
        val restore = RestoreAllRemindersUseCase(reconcile)
        val trigger = TriggerReminderUseCase(repository, scheduler, resolver, schedule)
    }

    private class FakeReminderRepository(
        reminders: List<Reminder>
    ) : ReminderRepository {
        private val values = reminders.associateByTo(linkedMapOf()) { it.id }

        override suspend fun getAll(): List<Reminder> = values.values.toList()

        override suspend fun getById(id: Long): Reminder? = values[id]

        override suspend fun getByTarget(
            targetType: ReminderTargetType,
            targetId: String
        ): List<Reminder> = values.values.filter {
            it.targetType == targetType && it.targetId == targetId
        }

        override suspend fun getEnabled(): List<Reminder> = values.values.filter { it.enabled }

        override suspend fun save(reminder: Reminder): Reminder {
            values[reminder.id] = reminder
            return reminder
        }

        override suspend fun delete(id: Long) {
            values.remove(id)
        }

        fun require(id: Long): Reminder = requireNotNull(values[id])
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val scheduled = linkedMapOf<Long, Long>()
        val cancelled = mutableListOf<Long>()
        var throwOnSchedule = false

        override fun scheduleReminder(reminderId: Long, triggerAtEpochMilli: Long) {
            if (throwOnSchedule) error("scheduler failed")
            scheduled[reminderId] = triggerAtEpochMilli
        }

        override fun cancelReminder(reminderId: Long) {
            scheduled.remove(reminderId)
            cancelled += reminderId
        }
    }

    private class FakeTargetTimeResolver : ReminderTargetTimeResolver {
        val times = mutableMapOf<Pair<ReminderTargetType, String>, Long>()

        override suspend fun resolveTargetTime(
            targetType: ReminderTargetType,
            targetId: String
        ): Long? = times[targetType to targetId]
    }

    private fun reminder(
        id: Long,
        targetId: String = TARGET_ID,
        absoluteTriggerAt: Long?,
        relativeOffsetMinutes: Int? = null
    ) = Reminder(
        id = id,
        targetType = ReminderTargetType.TASK,
        targetId = targetId,
        absoluteTriggerAt = absoluteTriggerAt,
        relativeOffsetMinutes = relativeOffsetMinutes,
        createdAt = 1_000,
        updatedAt = 1_000
    )

    private companion object {
        const val NOW = 10_000L
        const val TARGET_ID = "task-1"
    }
}
