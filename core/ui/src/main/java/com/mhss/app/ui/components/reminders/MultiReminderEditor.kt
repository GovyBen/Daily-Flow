package com.mhss.app.ui.components.reminders

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.TimeDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReminderDraft(
    val id: Long = 0,
    val label: String,
    val absoluteTriggerAt: Long?,
    val relativeOffsetMinutes: Int?,
    val isExisting: Boolean = false
)

@Composable
fun MultiReminderEditor(
    existingReminders: List<ReminderDraft>,
    targetTime: Long?,
    onRemindersChanged: (List<ReminderDraft>) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val drafts = remember { mutableStateListOf<ReminderDraft>() }
    var showQuickMenu by remember { mutableStateOf(false) }
    var showCustomPicker by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val reminderAtTimeLabel = stringResource(R.string.reminder_at_time)

    if (drafts.isEmpty() && existingReminders.isNotEmpty()) {
        drafts.addAll(existingReminders)
    }

    Column(modifier = modifier) {
        drafts.forEachIndexed { index, draft ->
            DraftRow(draft, index, drafts, onRemindersChanged, ctx)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showQuickMenu = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add_reminder))
        }

        DropdownMenu(
            expanded = showQuickMenu,
            onDismissRequest = { showQuickMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reminder_at_time)) },
                onClick = {
                    showQuickMenu = false
                    targetTime?.let { t ->
                        if (drafts.none { it.absoluteTriggerAt == t }) {
                            val label = reminderAtTimeLabel +
                                " " + timeFormat.format(Date(t))
                            drafts.add(ReminderDraft(label = label, absoluteTriggerAt = t, relativeOffsetMinutes = null))
                            onRemindersChanged(drafts.toList())
                        }
                    }
                }
            )
            QuickReminderItem(R.string.reminder_before_5min, 5, drafts, onRemindersChanged) { showQuickMenu = false }
            QuickReminderItem(R.string.reminder_before_15min, 15, drafts, onRemindersChanged) { showQuickMenu = false }
            QuickReminderItem(R.string.reminder_before_30min, 30, drafts, onRemindersChanged) { showQuickMenu = false }
            QuickReminderItem(R.string.reminder_before_1hour, 60, drafts, onRemindersChanged) { showQuickMenu = false }
            QuickReminderItem(R.string.reminder_before_1day, 1440, drafts, onRemindersChanged) { showQuickMenu = false }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reminder_custom_time)) },
                onClick = { showQuickMenu = false; showCustomPicker = true }
            )
        }

        if (showCustomPicker) {
            TimeDialog(
                initialDate = System.currentTimeMillis(),
                onDismissRequest = { showCustomPicker = false },
                onTimePicked = { epoch ->
                    showCustomPicker = false
                    if (drafts.none { it.absoluteTriggerAt == epoch }) {
                        drafts.add(ReminderDraft(label = timeFormat.format(Date(epoch)), absoluteTriggerAt = epoch, relativeOffsetMinutes = null))
                        onRemindersChanged(drafts.toList())
                    }
                }
            )
        }
    }
}

@Composable
private fun DraftRow(draft: ReminderDraft, index: Int, drafts: MutableList<ReminderDraft>,
                     onChanged: (List<ReminderDraft>) -> Unit, ctx: Context) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Notifications, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(draft.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(
            onClick = { drafts.removeAt(index); onChanged(drafts.toList()) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Close, stringResource(R.string.delete_task), Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuickReminderItem(
    labelRes: Int, minutes: Int,
    drafts: MutableList<ReminderDraft>,
    onRemindersChanged: (List<ReminderDraft>) -> Unit,
    onDismiss: () -> Unit
) {
    val label = stringResource(labelRes)
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onDismiss()
            if (drafts.none { it.relativeOffsetMinutes == minutes }) {
                drafts.add(ReminderDraft(label = label, absoluteTriggerAt = null, relativeOffsetMinutes = minutes))
                onRemindersChanged(drafts.toList())
            }
        }
    )
}
