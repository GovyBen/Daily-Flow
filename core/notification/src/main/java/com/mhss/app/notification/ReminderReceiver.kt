package com.mhss.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mhss.app.alarm.use_case.TriggerReminderUseCase
import com.mhss.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val triggerReminder: TriggerReminderUseCase by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val reminderId = intent?.getLongExtra(Constants.REMINDER_ID_EXTRA, -1L)
            ?.takeIf { it > 0 }
            ?: return
        val expectedTriggerAt = intent.getLongExtra(
            Constants.REMINDER_TRIGGER_AT_EXTRA,
            -1L
        ).takeIf { it >= 0 } ?: return
        val pendingResult = goAsync()

        scope.launch {
            try {
                triggerReminder(reminderId, expectedTriggerAt)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to process reminder trigger.", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReminderReceiver"
    }
}
