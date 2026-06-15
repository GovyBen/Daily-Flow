package com.mhss.app.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mhss.app.alarm.repository.AlarmScheduler
import com.mhss.app.alarm.use_case.GetAllAlarmsUseCase
import com.mhss.app.alarm.use_case.RestoreAllRemindersUseCase
import org.koin.android.annotation.KoinWorker

@KoinWorker
class RestoreAlarmsWorker(
    private val getAllAlarms: GetAllAlarmsUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val restoreAllReminders: RestoreAllRemindersUseCase,
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            var schedulingFailed = false
            getAllAlarms().forEach { alarm ->
                runCatching { alarmScheduler.scheduleAlarm(alarm) }
                    .onFailure { schedulingFailed = true }
            }
            if (restoreAllReminders().hasFailures) {
                schedulingFailed = true
            }
            if (schedulingFailed) retryOrFail() else Result.success()
        }.getOrElse {
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result {
        return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    companion object {
        const val WORK_NAME = "restore_alarms"
        private const val MAX_RETRIES = 3
    }
}
