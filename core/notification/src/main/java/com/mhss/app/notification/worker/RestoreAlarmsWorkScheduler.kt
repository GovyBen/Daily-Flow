package com.mhss.app.notification.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object RestoreAlarmsWorkScheduler {

    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            RestoreAlarmsWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RestoreAlarmsWorker>().build()
        )
    }
}
