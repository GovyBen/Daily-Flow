package com.mhss.app.mybrain.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.ui.R
import com.mhss.app.ui.navigation.Screen

@Composable
fun PendingRemindersCard(
    reminders: List<Reminder>,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val noRemindersLabel = stringResource(R.string.no_upcoming_reminders)
    val titleLabel = stringResource(R.string.pending_reminders)
    val taskLabel = stringResource(R.string.reminder_task_label)
    val eventLabel = stringResource(R.string.reminder_event_label)
    val promptLabel = stringResource(R.string.reminder_prompt_label)
    val pastDueLabel = stringResource(R.string.reminder_past_due)

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = titleLabel,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = titleLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
            }

            if (reminders.isEmpty()) {
                Text(
                    text = noRemindersLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reminders.forEachIndexed { index, reminder ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    ReminderItem(
                        reminder = reminder,
                        taskLabel = taskLabel,
                        eventLabel = eventLabel,
                        promptLabel = promptLabel,
                        pastDueLabel = pastDueLabel,
                        onClick = {
                            val route = when (reminder.targetType) {
                                ReminderTargetType.TASK -> Screen.TaskDetailScreen(reminder.targetId)
                                ReminderTargetType.DAILY_ITEM -> Screen.DailyItemDetailsScreen(
                                    reminder.targetId
                                )
                                ReminderTargetType.CALENDAR_EVENT -> {
                                    val eventId = reminder.targetId.toLongOrNull()
                                    Screen.CalendarEventDetailsScreen(eventId)
                                }
                                ReminderTargetType.RECORD_PROMPT -> Screen.TrackingQuickRecordScreen(
                                    reminder.targetId
                                )
                            }
                            navController.navigate(route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: Reminder,
    taskLabel: String,
    eventLabel: String,
    promptLabel: String,
    pastDueLabel: String,
    onClick: () -> Unit
) {
    val (icon, label) = when (reminder.targetType) {
        ReminderTargetType.TASK -> Icons.Outlined.CheckCircle to taskLabel
        ReminderTargetType.DAILY_ITEM -> Icons.Outlined.CheckCircle to taskLabel
        ReminderTargetType.CALENDAR_EVENT -> Icons.Outlined.CalendarToday to eventLabel
        ReminderTargetType.RECORD_PROMPT -> Icons.Outlined.Edit to promptLabel
    }

    val timeLabel = reminderTimeLabel(reminder, pastDueLabel)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            if (timeLabel != null) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun reminderTimeLabel(reminder: Reminder, pastDueLabel: String): String? {
    val now = System.currentTimeMillis()
    val absTrigger = reminder.absoluteTriggerAt
    val relOffset = reminder.relativeOffsetMinutes

    return if (absTrigger != null) {
        val diff = absTrigger - now
        when {
            diff < 0 -> pastDueLabel
            diff < 60_000 -> stringResource(R.string.reminder_less_than_minute)
            diff < 3_600_000 -> stringResource(R.string.reminder_in_minutes, (diff / 60_000).toInt())
            diff < 86_400_000 -> stringResource(R.string.reminder_in_hours, (diff / 3_600_000).toInt())
            else -> stringResource(R.string.reminder_in_days, (diff / 86_400_000).toInt())
        }
    } else if (relOffset != null) {
        stringResource(R.string.reminder_relative_before, relOffset.toInt())
    } else {
        null
    }
}
