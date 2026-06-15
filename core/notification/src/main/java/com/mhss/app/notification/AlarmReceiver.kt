package com.mhss.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mhss.app.alarm.use_case.DeleteAlarmUseCase
import com.mhss.app.domain.use_case.GetTaskByAlarmUseCase
import com.mhss.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val deleteAlarmUseCase: DeleteAlarmUseCase by inject()
    private val getTaskByAlarm: GetTaskByAlarmUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val pendingResult = goAsync()

        scope.launch {
            val alarmId = intent?.getAlarmIdBackwardsCompat()
            if (alarmId == null) {
                pendingResult.finish()
                return@launch
            }
            try {
                val task = getTaskByAlarm(alarmId)
                if (task != null) {
                    val manager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.sendNotification(task, context, alarmId)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to deliver task reminder.", error)
            } finally {
                runCatching { deleteAlarmUseCase(alarmId) }
                    .onFailure { Log.e(TAG, "Failed to clear delivered reminder.", it) }
                pendingResult.finish()
            }
        }
    }


    // Newly used name is alarm id but previous versions use task id name
    private fun Intent.getAlarmIdBackwardsCompat(): Int? {
        return getIntExtra(Constants.ALARM_ID_EXTRA, -1).takeIf { it != -1 }
            ?: getIntExtra(Constants.TASK_ID_EXTRA, -1).takeIf { it != -1 }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
