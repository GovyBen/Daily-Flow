package com.mhss.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhss.app.domain.model.*
import com.mhss.app.ui.R

/**
 * Confirmation card displayed when the AI proposes a write operation (DF-506).
 * Shows the proposal details and lets the user edit, confirm, or cancel.
 */
@Composable
fun ConfirmationCard(
    proposal: ActionProposal,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text(
                text = proposal.summary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            when (proposal) {
                is CreateTaskProposal -> TaskProposalContent(proposal)
                is CreateCalendarEventProposal -> CalendarProposalContent(proposal)
                is CreateDiaryEntryProposal -> DiaryProposalContent(proposal)
                is CreateRecordSessionProposal -> RecordProposalContent(proposal)
                is CompleteTaskProposal -> CompleteTaskContent(proposal)
                is ClarificationRequest -> ClarificationContent(proposal)
            }

            if (proposal.missingFields.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Missing: ${proposal.missingFields.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                if (onEdit != null) {
                    OutlinedButton(onClick = onEdit) {
                        Text("Edit")
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun TaskProposalContent(p: CreateTaskProposal) {
    Text("Title: ${p.title}")
    if (p.description.isNotBlank()) Text("Description: ${p.description}")
    Text("Priority: ${p.priority.name}")
    if (p.dueDateEpochMilli > 0) Text("Due: $p.dueDateEpochMilli")
    if (p.subTasks.isNotEmpty()) Text("Sub-tasks: ${p.subTasks.joinToString { it.title }}")
}

@Composable
private fun CalendarProposalContent(p: CreateCalendarEventProposal) {
    Text("Title: ${p.title}")
    p.description?.let { Text("Description: $it") }
    Text("Start: ${p.startEpochMilli}")
    Text("End: ${p.endEpochMilli}")
    p.location?.let { Text("Location: $it") }
}

@Composable
private fun DiaryProposalContent(p: CreateDiaryEntryProposal) {
    Text("Title: ${p.title}")
    Text("Content: ${p.content.take(200)}")
}

@Composable
private fun RecordProposalContent(p: CreateRecordSessionProposal) {
    Text("Template: ${p.templateName.ifBlank { p.templateId }}")
    p.note?.let { Text("Note: $it") }
    if (p.fieldValues.isNotEmpty()) {
        Text("Fields: ${p.fieldValues.joinToString { "${it.trackerName}=${it.value ?: it.label}" }}")
    }
}

@Composable
private fun CompleteTaskContent(p: CompleteTaskProposal) {
    Text("Task: ${p.taskTitle.ifBlank { p.taskId }}")
    Text("Mark as: ${if (p.completed) "Completed" else "Reopened"}")
}

@Composable
private fun ClarificationContent(p: ClarificationRequest) {
    Text(p.question, color = MaterialTheme.colorScheme.error)
}
