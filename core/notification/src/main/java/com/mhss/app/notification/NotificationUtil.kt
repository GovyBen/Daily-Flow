package com.mhss.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.util.Constants
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.model.Priority
import com.mhss.app.domain.model.Task
import com.mhss.app.ui.R

fun NotificationManager.sendNotification(task: Task, context: Context, id: Int) {
    val completeIntent = Intent(context, TaskActionButtonBroadcastReceiver::class.java).apply {
        action = Constants.ACTION_COMPLETE
        putExtra(Constants.TASK_ID_EXTRA, task.id)
    }
    val completePendingIntent: PendingIntent =
        PendingIntent.getBroadcast(
            context,
            task.alarmId ?: return,
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    val taskDetailIntent = Intent(
        Intent.ACTION_VIEW,
        "${Constants.TASK_DETAILS_URI}/${task.id}".toUri()
    )
    val taskDetailsPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(taskDetailIntent)
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    val notification = NotificationCompat.Builder(context, Constants.REMINDERS_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(task.title)
        .setContentText(task.description)
        .setContentIntent(taskDetailsPendingIntent)
        .setPriority(
            when (task.priority) {
                Priority.LOW -> NotificationCompat.PRIORITY_DEFAULT
                Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
                Priority.HIGH -> NotificationCompat.PRIORITY_MAX
            }
        )
        .addAction(R.drawable.ic_check, context.getString(R.string.complete), completePendingIntent)
        .setAutoCancel(true)
        .build()

    notify(id, notification)
}

/**
 * Sends a task reminder notification keyed by the unified [reminderId].
 * Uses the task's string ID in the complete action so
 * [TaskActionButtonBroadcastReceiver] can look it up without an alarm ID.
 */
fun NotificationManager.sendReminderNotification(
    task: Task,
    context: Context,
    reminderId: Int
) {
    val completeIntent = Intent(context, TaskActionButtonBroadcastReceiver::class.java).apply {
        action = Constants.ACTION_COMPLETE
        putExtra(Constants.TASK_ID_EXTRA, task.id)
    }
    val completePendingIntent = PendingIntent.getBroadcast(
        context,
        reminderId,
        completeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val taskDetailIntent = Intent(
        Intent.ACTION_VIEW,
        "${Constants.TASK_DETAILS_URI}/${task.id}".toUri()
    )
    val taskDetailsPendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(taskDetailIntent)
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    val notification = NotificationCompat.Builder(context, Constants.REMINDERS_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(task.title)
        .setContentText(task.description)
        .setContentIntent(taskDetailsPendingIntent)
        .setPriority(
            when (task.priority) {
                Priority.LOW -> NotificationCompat.PRIORITY_DEFAULT
                Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
                Priority.HIGH -> NotificationCompat.PRIORITY_MAX
            }
        )
        .addAction(
            R.drawable.ic_check,
            context.getString(R.string.complete),
            completePendingIntent
        )
        .setAutoCancel(true)
        .build()

    notify(reminderId, notification)
}

fun NotificationManager.sendDailyItemReminderNotification(
    item: DailyItem,
    context: Context,
    reminderId: Int
) {
    val completeIntent = Intent(context, TaskActionButtonBroadcastReceiver::class.java).apply {
        action = Constants.ACTION_COMPLETE
        putExtra(Constants.DAILY_ITEM_ID_EXTRA, item.id)
        putExtra(Constants.ALARM_ID_EXTRA, reminderId)
    }
    val completePendingIntent = PendingIntent.getBroadcast(
        context,
        reminderId,
        completeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val detailIntent = Intent(
        Intent.ACTION_VIEW,
        "${Constants.DAILY_ITEM_DETAILS_URI}/${item.id}".toUri()
    )
    val detailPendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(detailIntent)
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    val notification = NotificationCompat.Builder(context, Constants.REMINDERS_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(item.title)
        .setContentText(item.description)
        .setContentIntent(detailPendingIntent)
        .setPriority(
            when (item.priority) {
                DailyItemPriority.LOW -> NotificationCompat.PRIORITY_DEFAULT
                DailyItemPriority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
                DailyItemPriority.HIGH -> NotificationCompat.PRIORITY_MAX
            }
        )
        .addAction(
            R.drawable.ic_check,
            context.getString(R.string.complete),
            completePendingIntent
        )
        .setAutoCancel(true)
        .build()

    notify(reminderId, notification)
}

/**
 * Sends a calendar event reminder notification.
 */
fun NotificationManager.sendCalendarReminderNotification(
    event: CalendarEvent,
    context: Context,
    reminderId: Int
) {
    val detailIntent = Intent(
        Intent.ACTION_VIEW,
        "${Constants.CALENDAR_DETAILS_SCREEN_URI}/${event.id}".toUri()
    )
    val detailPendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(detailIntent)
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    val notification = NotificationCompat.Builder(context, Constants.REMINDERS_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(event.title)
        .setContentText(event.description)
        .setContentIntent(detailPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notify(reminderId, notification)
}

/**
 * Sends a record prompt notification that opens the quick record page
 * for the given template.
 */
fun NotificationManager.sendRecordPromptNotification(
    templateId: String,
    context: Context,
    reminderId: Int
) {
    val recordIntent = Intent(
        Intent.ACTION_VIEW,
        "${Constants.TRACKING_QUICK_RECORD_URI}/$templateId".toUri()
    )
    val recordPendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(recordIntent)
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    val notification = NotificationCompat.Builder(context, Constants.REMINDERS_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(context.getString(R.string.record_prompt_title))
        .setContentText(context.getString(R.string.record_prompt_message))
        .setContentIntent(recordPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    notify(reminderId, notification)
}
