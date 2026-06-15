package com.mhss.app.mybrain.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.model.AlarmDeliveryMode
import com.mhss.app.alarm.model.AlarmSchedulePolicy
import com.mhss.app.alarm.repository.AlarmScheduler
import com.mhss.app.alarm.repository.ReminderScheduler
import com.mhss.app.notification.AlarmReceiver
import com.mhss.app.notification.ReminderReceiver
import com.mhss.app.notification.worker.AlarmFallbackWorker
import com.mhss.app.notification.worker.ReminderFallbackWorker
import com.mhss.app.util.Constants
import org.koin.core.annotation.Factory
import java.util.concurrent.TimeUnit

@Factory
class AlarmSchedulerImpl(
    private val context: Context
): AlarmScheduler, ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun scheduleAlarm(alarm: Alarm) {
        cancelAlarm(alarm.id)
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra(Constants.ALARM_ID_EXTRA, alarm.id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val plan = AlarmSchedulePolicy.create(canScheduleExactAlarms())
        when (plan.deliveryMode) {
            AlarmDeliveryMode.EXACT -> AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                alarm.time,
                pendingIntent
            )

            AlarmDeliveryMode.INEXACT -> AlarmManagerCompat.setAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                alarm.time,
                pendingIntent
            )
        }
        val fallbackDelay = (alarm.time - System.currentTimeMillis())
            .coerceAtLeast(0L) + plan.fallbackDelayMillis
        val fallback = OneTimeWorkRequestBuilder<AlarmFallbackWorker>()
            .setInputData(AlarmFallbackWorker.inputData(alarm.id, alarm.time))
            .setInitialDelay(fallbackDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            AlarmFallbackWorker.workName(alarm.id),
            ExistingWorkPolicy.REPLACE,
            fallback
        )
    }

    override fun cancelAlarm(schedulerId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedulerId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        workManager.cancelUniqueWork(AlarmFallbackWorker.workName(schedulerId))
    }

    override fun scheduleReminder(reminderId: Long, triggerAtEpochMilli: Long) {
        val requestCode = reminderId.toRequestCode()
        cancelReminder(reminderId)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(Constants.REMINDER_ID_EXTRA, reminderId)
            .putExtra(Constants.REMINDER_TRIGGER_AT_EXTRA, triggerAtEpochMilli)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val plan = AlarmSchedulePolicy.create(canScheduleExactAlarms())
        when (plan.deliveryMode) {
            AlarmDeliveryMode.EXACT -> AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMilli,
                pendingIntent
            )

            AlarmDeliveryMode.INEXACT -> AlarmManagerCompat.setAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMilli,
                pendingIntent
            )
        }
        val fallbackDelay = (triggerAtEpochMilli - System.currentTimeMillis())
            .coerceAtLeast(0L) + plan.fallbackDelayMillis
        val fallback = OneTimeWorkRequestBuilder<ReminderFallbackWorker>()
            .setInputData(
                ReminderFallbackWorker.inputData(
                    reminderId,
                    triggerAtEpochMilli
                )
            )
            .setInitialDelay(fallbackDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            ReminderFallbackWorker.workName(reminderId),
            ExistingWorkPolicy.REPLACE,
            fallback
        )
    }

    override fun cancelReminder(reminderId: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toRequestCode(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        pendingIntent?.let(alarmManager::cancel)
        workManager.cancelUniqueWork(ReminderFallbackWorker.workName(reminderId))
    }

    override fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun Long.toRequestCode(): Int {
        require(this in 1..Int.MAX_VALUE.toLong()) {
            "A persisted reminder ID is required for scheduling"
        }
        return toInt()
    }
}

