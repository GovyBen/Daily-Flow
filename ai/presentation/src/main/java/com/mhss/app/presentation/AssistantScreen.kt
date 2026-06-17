@file:Suppress("AssignedValueIsNeverRead")

package com.mhss.app.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mhss.app.domain.model.AiMessage
import com.mhss.app.domain.model.AiMessageAttachment
import com.mhss.app.presentation.components.AssistantChatBar
import com.mhss.app.presentation.components.AttachNoteSheet
import com.mhss.app.presentation.components.AttachTaskSheet
import com.mhss.app.presentation.components.AttachmentDropDownMenu
import com.mhss.app.presentation.components.AttachmentMenuItem
import com.mhss.app.presentation.components.MessageCard
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.LeftToRight
import com.mhss.app.ui.components.common.MyBrainAppBar
import com.mhss.app.ui.navigation.Screen
import com.mhss.app.ui.theme.MyBrainTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.koin.androidx.compose.koinViewModel

@Composable
fun AssistantScreen(
    navController: NavHostController,
    viewModel: AssistantViewModel = koinViewModel(),
) {
    AssistantScreenContent(
        uiState = viewModel.uiState,
        messages = viewModel.messages,
        attachments = viewModel.attachments,
        aiEnabled = viewModel.aiEnabled,
        onEvent = viewModel::onEvent,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreenContent(
    uiState: AssistantViewModel.UiState,
    messages: List<AiMessage>,
    attachments: List<AiMessageAttachment>,
    aiEnabled: Boolean,
    onEvent: (AssistantEvent) -> Unit,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val loading = uiState.loading
    val error = uiState.error
    var text by rememberSaveable { mutableStateOf("") }
    var attachmentsMenuExpanded by remember {
        mutableStateOf(false)
    }
    val noteSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val taskSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var openNoteSheet by remember { mutableStateOf(false) }
    var openTaskSheet by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val liquidState = rememberLiquidState()
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) focusManager.clearFocus()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            MyBrainAppBar(stringResource(id = R.string.assistant))
        },
        bottomBar = {
            AssistantChatBar(
                text = text,
                enabled = aiEnabled && !loading && text.isNotBlank(),
                attachments = attachments,
                onTextChange = { text = it },
                onAttachClick = { attachmentsMenuExpanded = true },
                onRemoveAttachment = {
                    onEvent(AssistantEvent.RemoveAttachment(it))
                },
                loading = loading,
                onSend = {
                    onEvent(
                        AssistantEvent.SendMessage(
                            content = text,
                            attachments = attachments.toList()
                        )
                    )
                    text = ""
                    keyboardController?.hide()
                },
                onCancel = {
                    onEvent(AssistantEvent.CancelMessage)
                },
                liquidState = liquidState,
            )
            val excludedItems by remember {
                derivedStateOf {
                    if (attachments.contains(AiMessageAttachment.CalenderEvents)) {
                        listOf(AttachmentMenuItem.CalendarEvents)
                    } else {
                        emptyList()
                    }
                }
            }
            AttachmentDropDownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = attachmentsMenuExpanded,
                liquidState = liquidState,
                onDismiss = { attachmentsMenuExpanded = false },
                excludedItems = excludedItems,
                onItemClick = {
                    when (it) {
                        AttachmentMenuItem.Note -> openNoteSheet = true
                        AttachmentMenuItem.Task -> openTaskSheet = true
                        AttachmentMenuItem.CalendarEvents -> onEvent(AssistantEvent.AddAttachmentEvents)
                    }
                    attachmentsMenuExpanded = false
                }
            )
        },
        modifier = Modifier.imePadding()
    ) { paddingValues ->
        if (openNoteSheet) AttachNoteSheet(
            state = noteSheetState,
            onDismissRequest = { openNoteSheet = false },
            notes = uiState.searchNotes,
            view = uiState.noteView,
            onQueryChange = { onEvent(AssistantEvent.SearchNotes(it)) }
        ) {
            onEvent(AssistantEvent.AddAttachmentNote(it.id))
            openNoteSheet = false
        }
        if (openTaskSheet) AttachTaskSheet(
            state = taskSheetState,
            onDismissRequest = { openTaskSheet = false },
            tasks = uiState.searchTasks,
            onQueryChange = { onEvent(AssistantEvent.SearchTasks(it)) }
        ) {
            onEvent(AssistantEvent.AddAttachmentTask(it.id))
            openTaskSheet = false
        }
        Column(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (aiEnabled) {
                LeftToRight {
                    LazyColumn(
                        state = lazyListState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize().liquefiable(liquidState)
                    ) {
                        item(key = "initial_spacer") {
                            Spacer(
                                Modifier
                                    .padding(bottom = paddingValues.calculateBottomPadding())
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            )
                        }
                        error?.let { error ->
                            item(key = "error_message") {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    colors = CardDefaults.cardColors(
                                        contentColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = error.toUserMessage(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .align(Alignment.CenterHorizontally),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        items(messages, key = { it.uuid }) { message ->
                            MessageCard(
                                message = message,
                                onCopy = { content ->
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("label", content)
                                    clipboard.setPrimaryClip(clip)
                                },
                                onNoteClick = { note ->
                                    navController.navigate(Screen.NoteDetailsScreen(noteId = note.id, folderId = note.folderId))
                                },
                                onTaskClick = { task ->
                                    navController.navigate(Screen.TaskDetailScreen(taskId = task.id))
                                },
                                onEventClick = { event ->
                                    navController.navigate(
                                        Screen.CalendarEventDetailsScreen(
                                            event.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onErrorContainer
                    ),
                    colors = CardDefaults.cardColors(
                        contentColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.ai_not_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // --- AI Proposal Confirmation Dialog ---
    val pendingProposal = uiState.pendingProposal
    if (pendingProposal != null) {
        val confirmLabel = stringResource(R.string.confirm)
        val cancelLabel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { onEvent(AssistantEvent.DismissProposal) },
            title = {
                Text(
                    text = stringResource(R.string.confirm_ai_action),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = pendingProposal.summary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.tool_name)}: ${pendingProposal.toolName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onEvent(AssistantEvent.ConfirmProposal) }) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onEvent(AssistantEvent.DismissProposal) }) {
                    Text(cancelLabel)
                }
            }
        )
    }
}

// === Preview ===

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssistantScreenContentPreview() {
    MyBrainTheme {
        AssistantScreenContent(
            uiState = AssistantViewModel.UiState(),
            messages = listOf(
                AiMessage.AssistantMessage(
                    content = "After carefully reviewing the collection of notes you provided, I detected several recurring themes and insights that could be valuable for your upcoming projects. In addition to the summary I mentioned, I can also suggest specific actionable steps, categorize the information by priority, and highlight any hidden patterns that might inform your strategy. Let me know if you’d like a detailed report, a visual diagram, or a concise bullet‑point overview.",
                    time = 5,
                    uuid = "5"
                ),
                AiMessage.ToolCall(
                    uuid = "4",
                    id = "4",
                    name = "searchNotes",
                    rawContent = "",
                    resultRawContent = "",
                    time = 4
                ),
                AiMessage.UserMessage(
                    content = "I'm juggling a tight deadline for the project next week and feel a bit overwhelming. Could you help me break down my tasks, prioritize them, and suggest an organized plan to ensure I meet all milestones on time?",
                    time = 3,
                    uuid = "3"
                ),
                AiMessage.AssistantMessage(
                    content = "Welcome! I'm your AI assistant, ready to support you with managing notes, creating and tracking tasks, and handling calendar events. I can help you set up smart reminders, generate project outlines, automate repetitive workflows, and even suggest productivity techniques tailored to your habits. Just tell me what you need—whether it's organizing information, setting reminders, or anything else to boost your efficiency—and I’ll get started right away.",
                    time = 2,
                    uuid = "2"
                ),
                AiMessage.UserMessage(
                    content = "Hello! I'm just getting started on improving my workflow and would love some guidance on how to set up an effective productivity system. Can you walk me through the steps to organize my tasks, set up a reliable routine, and keep everything synchronized across my devices?",
                    time = 1,
                    uuid = "1"
                )
            ),
            attachments = emptyList(),
            aiEnabled = true,
            onEvent = {},
            navController = rememberNavController()
        )
    }
}