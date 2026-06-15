package com.mhss.app.notification.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.mhss.app.alarm.use_case.GetAllAlarmsUseCase
import com.mhss.app.notification.AlarmReceiver
import com.mhss.app.util.Constants
import org.koin.android.annotation.KoinWorker

@KoinWorker
class AlarmFallbackWorker(
    private val getAllAlarms: GetAllAlarmsUseCase,
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val alarmId = inputData.getInt(KEY_ALARM_ID, INVALID_ALARM_ID)
        val expectedTime = inputData.getLong(KEY_EXPECTED_TIME, INVALID_EXPECTED_TIME)
        if (alarmId == INVALID_ALARM_ID || expectedTime == INVALID_EXPECTED_TIME) {
            return Result.failure()
        }
        if (expectedTime > System.currentTimeMillis()) return Result.retry()

        return runCatching {
            val alarmStillPending = getAllAlarms().any {
                it.id == alarmId && it.time == expectedTime
            }
            if (alarmStillPending) {
                applicationContext.sendBroadcast(
                    Intent(applicationContext, AlarmReceiver::class.java)
                        .putExtra(Constants.ALARM_ID_EXTRA, alarmId)
                )
            }
            Result.success()
        }.getOrElse {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val KEY_ALARM_ID = "alarm_id"
        private const val KEY_EXPECTED_TIME = "expected_time"
        private const val INVALID_ALARM_ID = -1
        private const val INVALID_EXPECTED_TIME = -1L
        private const val WORK_NAME_PREFIX = "alarm_fallback_"
        private const val MAX_RETRIES = 3

        fun workName(alarmId: Int): String = "$WORK_NAME_PREFIX$alarmId"

        fun inputData(alarmId: Int, expectedTime: Long): Data {
            return Data.Builder()
                .putInt(KEY_ALARM_ID, alarmId)
                .putLong(KEY_EXPECTED_TIME, expectedTime)
                .build()
        }
    }
}
