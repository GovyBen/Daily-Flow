package com.mhss.app.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import com.mhss.app.domain.model.AiMessage
import com.mhss.app.domain.model.AiMessageAttachment
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.model.ToolCallResultObject
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.defaultMarkdownTypography
import com.mhss.app.ui.theme.MyBrainTheme
import com.mhss.app.util.date.formatTime
import com.mikepenz.markdown.coil2.Coil2ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor

@Composable
fun LazyItemScope.MessageCard(
    message: AiMessage,
    onCopy: (String) -> Unit,
    onNoteClick: (Note) -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onEventClick: (CalendarEvent) -> Unit = {},
) {
    when (message) {
        is AiMessage.UserMessage -> UserMessageCard(message = message, onCopy = onCopy)
        is AiMessage.AssistantMessage -> AssistantMessageCard(message = message, onCopy = onCopy)
        is AiMessage.ToolCall -> {
            Column {
                ToolCallCard(toolCall = message)
                message.resultObject?.let {
                    ToolCallResultPreview(
                        resultObject = it,
                        onNoteClick = onNoteClick,
                        onTaskClick = onTaskClick,
                        onEventClick = onEventClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.UserMessageCard(
    message: AiMessage.UserMessage,
    onCopy: (String) -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val formattedTime by remember(message.time) {
        derivedStateOf { message.time.formatTime(context) }
    }
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, start = 48.dp, bottom = 4.dp, top = 8.dp)
            .messageCardAnimatedPlacement()
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 4.dp,
                bottomStart = 24.dp,
                bottomEnd = 14.dp
            ),
            elevation = CardDefaults.cardElevation(5.dp),
            onClick = { showContextMenu = true }
        ) {
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            Column(
                modifier = Modifier
                    .drawBehind {
                        drawAiGradientRadials(surfaceVariant, radius = size.minDimension * 1.2f)
                    }
            ) {
                Markdown(
                    content = message.content,
                    modifier = Modifier.padding(top = 7.dp, start = 12.dp, end = 8.dp),
                    imageTransformer = Coil2ImageTransformerImpl,
                    colors = markdownColor(text = MaterialTheme.colorScheme.onSurfaceVariant),
                    typography = defaultMarkdownTypography()
                )
                if (message.attachments.isNotEmpty()) {
                    AiAttachmentsSection(
                        attachments = message.attachments,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.copy)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy)
                            )
                        },
                        onClick = {
                            showContextMenu = false
                            onCopy(message.content)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.AssistantMessageCard(
    message: AiMessage.AssistantMessage,
    onCopy: (String) -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val formattedTime by remember(message.time) {
        derivedStateOf { message.time.formatTime(context) }
    }
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp, start = 12.dp, bottom = 4.dp, top = 8.dp)
            .messageCardAnimatedPlacement()
            .clickable { showContextMenu = true }
    ) {
        Column {
            Markdown(
                content = message.content,
                imageTransformer = Coil2ImageTransformerImpl,
                colors = markdownColor(text = MaterialTheme.colorScheme.onSurfaceVariant),
                typography = defaultMarkdownTypography()
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.copy)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = stringResource(id = R.string.copy)
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onCopy(message.content)
                    }
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.ToolCallCard(
    toolCall: AiMessage.ToolCall,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 48.dp, start = 8.dp)
            .messageCardAnimatedPlacement()
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (toolCall.isFailed) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer
            ),
            onClick = { expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tools),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = toolCall.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textDecoration = if (toolCall.isFailed) TextDecoration.LineThrough else null
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = stringResource(R.string.tool_call_content),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = toolCall.rawContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.tool_call_result),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = toolCall.resultRawContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCallResultPreview(
    resultObject: ToolCallResultObject,
    onNoteClick: (Note) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    when (resultObject) {
        is ToolCallResultObject.Notes -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resultObject.notes) { note ->
                    AiNoteCard(
                        note = note,
                        onClick = onNoteClick,
                        modifier = Modifier.widthIn(max = 290.dp)
                    )
                }
            }
        }

        is ToolCallResultObject.Tasks -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resultObject.tasks) { task ->
                    AiTaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        modifier = Modifier.widthIn(max = 260.dp)
                    )
                }
            }
        }

        is ToolCallResultObject.CalendarEvents -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resultObject.events) { event ->
                    AiCalendarEventCard(
                        event = event,
                        onClick = onEventClick,
                        modifier = Modifier.widthIn(max = 260.dp)
                    )
                }
            }
        }

        is ToolCallResultObject.Proposal -> {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    text = "📋 Proposal: ${resultObject.summary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to review and confirm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
context(scope: LazyItemScope)
private fun Modifier.messageCardAnimatedPlacement() = with(scope) {
    this@messageCardAnimatedPlacement.animateItem(
        fadeInSpec = spring(
            dampingRatio = 0.95f,
            stiffness = Spring.StiffnessMedium
        ),
        placementSpec = spring(
            dampingRatio = 0.95f,
            stiffness = Spring.StiffnessMedium
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun MessageCardPreview() {
    MyBrainTheme {
        val demoText = remember {
            LoremIpsum(60).values.first()
        }
        LazyColumn(
            Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            item {
                MessageCard(
                    message = AiMessage.UserMessage(
                        content = demoText,
                        time = 1111111111,
                        uuid = "123",
                        attachments = listOf(
                            AiMessageAttachment.Note(
                                Note(
                                    "This is a test tile for the note",
                                    "Description",
                                    1111111111,
                                    id = "1"
                                )
                            ),
                            AiMessageAttachment.CalenderEvents,
                        )
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
            item {
                MessageCard(
                    message = AiMessage.ToolCall(
                        id = "test-id",
                        name = "searchNotes",
                        rawContent = "{\"query\": \"meeting notes\"}",
                        resultRawContent = "[{\"id\": 1, \"title\": \"Meeting Notes\", \"content\": \"Discussion about project...\"}]",
                        time = 1111111113,
                        uuid = "890"
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
            item {
                MessageCard(
                    message = AiMessage.ToolCall(
                        id = "test-id-",
                        name = "searchNotes",
                        rawContent = "{\"query\": \"meeting notes\"}",
                        resultRawContent = "[{\"id\": 1, \"title\": \"Meeting Notes\", \"content\": \"Discussion about project...\"}]",
                        time = 1111111113,
                        uuid = "890"
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
            item {
                MessageCard(
                    message = AiMessage.AssistantMessage(
                        content = demoText,
                        time = 1111111112,
                        uuid = "567"
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
            item {
                MessageCard(
                    message = AiMessage.ToolCall(
                        id = "test-id-1",
                        name = "searchNotes",
                        rawContent = "{\"query\": \"meeting notes\"}",
                        resultRawContent = "Found 1 note",
                        time = 1111111113,
                        uuid = "890",
                        resultObject = ToolCallResultObject.Notes(
                            listOf(
                                Note(
                                    title = "Meeting Notes",
                                    content = "Discussion about project architecture and upcoming deadlines.",
                                    createdDate = 1111111111,
                                    updatedDate = 1111111111,
                                    id = "1"
                                )
                            )
                        )
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
            item {
                MessageCard(
                    message = AiMessage.ToolCall(
                        id = "test-id-2",
                        name = "createMultipleTasks",
                        rawContent = "Create 2 tasks",
                        resultRawContent = "Tasks created",
                        time = 1111111114,
                        uuid = "891",
                        resultObject = ToolCallResultObject.Tasks(
                            listOf(
                                Task(
                                    title = "Fix UI bugs",
                                    description = "Check the padding in the message card",
                                    id = "task1"
                                ),
                                Task(
                                    title = "Update documentation",
                                    description = "Add info about AI tools",
                                    id = "task2",
                                    isCompleted = true
                                )
                            )
                        )
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
            item {
                MessageCard(
                    message = AiMessage.ToolCall(
                        id = "test-id-3",
                        name = "createEvent",
                        rawContent = "Create calendar event",
                        resultRawContent = "Event created",
                        time = 1111111115,
                        uuid = "892",
                        resultObject = ToolCallResultObject.CalendarEvents(
                            listOf(
                                CalendarEvent(
                                    title = "Design Sync",
                                    start = 1111111111,
                                    end = 1111111111 + 3600000,
                                    id = 123,
                                    calendarId = 1,
                                    color = 0xFF00FF00.toInt()
                                )
                            )
                        )
                    ),
                    onCopy = {},
                    onNoteClick = {},
                    onTaskClick = {},
                    onEventClick = {}
                )
            }
        }
    }
}