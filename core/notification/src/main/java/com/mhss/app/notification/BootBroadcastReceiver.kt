package com.mhss.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mhss.app.notification.worker.RestoreAlarmsWorkScheduler

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action !in RESTORE_ACTIONS) return

        RestoreAlarmsWorkScheduler.enqueue(context)
    }

    private companion object {
        val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            EXACT_ALARM_PERMISSION_ACTION
        )
        const val EXACT_ALARM_PERMISSION_ACTION =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
