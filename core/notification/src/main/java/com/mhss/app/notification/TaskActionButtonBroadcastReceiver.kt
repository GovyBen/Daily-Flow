package com.mhss.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mhss.app.daily.domain.usecase.CompleteDailyItemUseCase
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.use_case.GetTaskByAlarmUseCase
import com.mhss.app.domain.use_case.GetTaskByIdUseCase
import com.mhss.app.domain.use_case.UpdateTaskCompletedUseCase
import com.mhss.app.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TaskActionButtonBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val updateTaskCompleted: UpdateTaskCompletedUseCase by inject()
    private val completeDailyItem: CompleteDailyItemUseCase by inject()
    private val getTaskById: GetTaskByIdUseCase by inject()
    private val getTaskByAlarm: GetTaskByAlarmUseCase by inject()
    private val ioDispatcher: CoroutineDispatcher by inject(named("ioDispatcher"))
    private val scope = CoroutineScope(ioDispatcher)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Constants.ACTION_COMPLETE) {
            val pendingResult = goAsync()
            scope.launch(ioDispatcher) {
                try {
                    val dailyItemId = intent.getStringExtra(Constants.DAILY_ITEM_ID_EXTRA)
                    if (dailyItemId != null) {
                        completeDailyItem(dailyItemId)
                        val manager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val notificationId = intent.getIntExtra(
                            Constants.ALARM_ID_EXTRA,
                            -1
                        ).takeIf { it != -1 } ?: return@launch
                        manager.cancel(notificationId)
                        return@launch
                    }
                    val task = intent.getTaskBackwardsCompat() ?: return@launch
                    updateTaskCompleted(task, true)
                    val manager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    // Cancel by whatever ID was used for the notification.
                    // For old alarm-based notifications: task.alarmId
                    // For new reminder-based notifications: the notification ID
                    // equals the PendingIntent request code (which is the alarm/reminder id).
                    val notificationId = intent.getIntExtra(
                        Constants.ALARM_ID_EXTRA, -1
                    ).takeIf { it != -1 }
                        ?: task.alarmId
                        ?: return@launch
                    manager.cancel(notificationId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    // The new is uuid string but previously it was an int which is same as alarm id
    private suspend fun Intent.getTaskBackwardsCompat(): Task? {
        val taskId = getStringExtra(Constants.TASK_ID_EXTRA)
        taskId?.let { return getTaskById(it) }
        val alarmId = getIntExtra(Constants.TASK_ID_EXTRA, -1).takeIf { it != -1 }
        return alarmId?.let { getTaskByAlarm(it) }
    }
}
