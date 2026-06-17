package com.mhss.app.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhss.app.domain.model.AiMessage
import com.mhss.app.domain.model.AiMessageAttachment
import com.mhss.app.domain.model.Note
import com.mhss.app.domain.model.SubTask
import com.mhss.app.domain.model.Task
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.LeftToRight
import com.mhss.app.ui.components.common.clearGlass
import com.mhss.app.ui.components.common.drawGradientRadial
import com.mhss.app.ui.gradientBrushColor
import com.mhss.app.ui.theme.Blue
import com.mhss.app.ui.theme.DarkOrange
import com.mhss.app.ui.theme.LightPurple
import com.mhss.app.ui.theme.MyBrainTheme
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.launch

@Composable
fun AssistantChatBar(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    loading: Boolean,
    liquidState: LiquidState,
    attachments: List<AiMessageAttachment>,
    onTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isExpanded = isFocused || text.isNotBlank()
    val cornerRadius by animateDpAsState(
        if (isExpanded) 24.dp else 99.dp,
        animationSpec = chatBarAnimationSpec(),
    )
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth()
    ) {
        AnimatedVisibility(attachments.isNotEmpty()) {
            AiAttachmentsSection(
                attachments = attachments,
                editable = true,
                onRemove = onRemoveAttachment,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Card(
            modifier = modifier
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                .dropShadow(CircleShape) {
                    offset = Offset(0f, 30f)
                    alpha = 0.18f
                    radius = 36f
                    brush = gradientBrushColor()
                },
            elevation = CardDefaults.cardElevation(0.dp),
            shape = shape,
        ) {

            Column(
                modifier = Modifier
                    .clearGlass(
                        liquidState = liquidState,
                        shape = { shape },
                        edge = { if (isExpanded) 0.012f else 0.03f },
                    )
                    .drawAnimatedGradient(loading = loading)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(chatBarAnimationSpec()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = text,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        onValueChange = onTextChange,
                        shape = shape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(0.dp, if (isFocused) 250.dp else 150.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ask_assistant),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    )
                    if (loading) {
                        IconButton(onClick = { onCancel() }) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(id = R.drawable.ic_stop),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContent()
                                                drawRect(
                                                    gradientBrushColor(),
                                                    blendMode = BlendMode.SrcAtop
                                                )
                                            }
                                        }
                                )
                                CircularProgressIndicator(
                                    Modifier
                                        .size(30.dp)
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContent()
                                                drawRect(
                                                    gradientBrushColor(),
                                                    blendMode = BlendMode.SrcAtop
                                                )
                                            }
                                        },
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = isFocused,
                    enter = fadeIn(chatBarAnimationSpec()) + expandVertically(chatBarAnimationSpec()),
                    exit = fadeOut(chatBarAnimationSpec()) + shrinkVertically(chatBarAnimationSpec())
                ) {
                    LeftToRight {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { onAttachClick() }) {
                                    Icon(
                                        painterResource(id = R.drawable.ic_attach),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (!loading) {
                                IconButton(
                                    onClick = { onSend() },
                                    enabled = enabled,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_send_message),
                                        contentDescription = stringResource(R.string.send_message),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = if (enabled) 0.9f else 0.3f
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

private inline fun <T> chatBarAnimationSpec() = tween<T>(durationMillis = 300, easing = FastOutSlowInEasing)

fun Modifier.drawAnimatedGradient(
    loading: Boolean
) = composed {
    var lastX by remember { mutableFloatStateOf(0f) }
    var lastY by remember { mutableFloatStateOf(0f) }
    val animX = remember(loading) { Animatable(lastX) }
    val animY = remember(loading) { Animatable(lastY) }

    LaunchedEffect(loading) {
        if (loading) {
            launch {
                animX.animateTo(
                    1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(2200, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        )
                ) {
                    lastX = value
                }
            }
            launch {
                animY.animateTo(
                    1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(2800, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        )
                ) {
                    lastY = value
                }
            }
        }
    }
    drawBehind {
        val xMul = animX.value
        val yMul = animY.value
        drawGradientRadial(
            Blue.copy(0.15f),
            Offset(
                size.width * xMul,
                size.height - size.height * yMul
            ),
            radius = size.maxDimension * 0.45f
        )
        drawGradientRadial(
            DarkOrange.copy(0.15f),
            Offset(
                size.width - size.width * xMul,
                size.height - size.height * yMul
            ),
            radius = size.maxDimension * 0.45f
        )
        drawGradientRadial(
            LightPurple.copy(0.15f),
            Offset(
                size.width - size.width * xMul * 0.7f,
                size.height * yMul
            ),
            radius = size.maxDimension * 0.45f
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AssistantChatBarPreview() {
    MyBrainTheme {
        val liquidState = rememberLiquidState()
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.liquefiable(liquidState)) {
                LazyColumn {
                    item {
                        MessageCard(
                            message = AiMessage.UserMessage(
                                uuid = "uuid",
                                content = "This is a test example message",
                                time = 1
                            ),
                            onCopy = {}
                        )
                    }
                    item {
                        Spacer(Modifier.height(26.dp))
                    }
                }
            }
            AssistantChatBar(
                text = "",
                enabled = true,
                attachments = listOf(
                    AiMessageAttachment.Note(
                        Note(
                            id = "1",
                            title = "This is a Note Title",
                            content = "Note Content",
                        )
                    ),
                    AiMessageAttachment.Task(
                        Task(
                            id = "1",
                            title = "This is a Task Title",
                            description = "Task Description",
                            isCompleted = false,
                            dueDate = 12345,
                            subTasks = listOf(
                                SubTask()
                            )
                        )
                    )
                ),
                loading = true,
                onTextChange = {},
                onAttachClick = {},
                onRemoveAttachment = {},
                onSend = {},
                onCancel = {},
                liquidState = liquidState
            )
        }
    }
}