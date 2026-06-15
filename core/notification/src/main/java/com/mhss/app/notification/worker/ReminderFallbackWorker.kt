package com.mhss.app.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.mhss.app.alarm.use_case.TriggerReminderUseCase
import org.koin.android.annotation.KoinWorker

@KoinWorker
class ReminderFallbackWorker(
    private val triggerReminder: TriggerReminderUseCase,
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, INVALID_REMINDER_ID)
        val expectedTriggerAt = inputData.getLong(
            KEY_EXPECTED_TRIGGER_AT,
            INVALID_TRIGGER_AT
        )
        if (reminderId == INVALID_REMINDER_ID || expectedTriggerAt == INVALID_TRIGGER_AT) {
            return Result.failure()
        }
        if (expectedTriggerAt > System.currentTimeMillis()) return Result.retry()

        return runCatching {
            triggerReminder(reminderId, expectedTriggerAt)
            Result.success()
        }.getOrElse {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val KEY_REMINDER_ID = "reminder_id"
        private const val KEY_EXPECTED_TRIGGER_AT = "expected_trigger_at"
        private const val INVALID_REMINDER_ID = -1L
        private const val INVALID_TRIGGER_AT = -1L
        private const val WORK_NAME_PREFIX = "reminder_fallback_"
        private const val MAX_RETRIES = 3

        fun workName(reminderId: Long): String = "$WORK_NAME_PREFIX$reminderId"

        fun inputData(reminderId: Long, expectedTriggerAt: Long): Data {
            return Data.Builder()
                .putLong(KEY_REMINDER_ID, reminderId)
                .putLong(KEY_EXPECTED_TRIGGER_AT, expectedTriggerAt)
                .build()
        }
    }
}
