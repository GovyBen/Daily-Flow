package com.mhss.app.mybrain.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.MyBrainAppBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    viewModel: PomodoroViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val state = viewModel.state
    val focusColor = Color(0xFFE53935)
    val shortBreakColor = Color(0xFF43A047)
    val longBreakColor = Color(0xFF1E88E5)

    val phaseColor = when (state.phase) {
        PomodoroPhase.FOCUS -> focusColor
        PomodoroPhase.SHORT_BREAK -> shortBreakColor
        PomodoroPhase.LONG_BREAK -> longBreakColor
    }

    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyBrainAppBar(
                title = "番茄钟",
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Text("⚙", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Phase label
            val phaseLabel = when (state.phase) {
                PomodoroPhase.FOCUS -> "专注中"
                PomodoroPhase.SHORT_BREAK -> "短休息"
                PomodoroPhase.LONG_BREAK -> "长休息"
            }
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = phaseColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            // Timer circle
            Box(contentAlignment = Alignment.Center) {
                val totalSeconds = state.phase.defaultMinutes * 60L
                val progress = if (totalSeconds > 0) {
                    (totalSeconds - state.remainingSeconds).toFloat() / totalSeconds.toFloat()
                } else 0f

                Canvas(modifier = Modifier.size(240.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2

                    // Background track
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = radius,
                        center = Offset(size.width / 2, size.height / 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    val sweepAngle = progress * 360f
                    drawArc(
                        color = phaseColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Timer text
                val minutes = state.remainingSeconds / 60
                val seconds = state.remainingSeconds % 60
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "完成 ${state.completedSessions} 个番茄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { viewModel.reset() }) {
                    Text("重置")
                }
                Button(
                    onClick = {
                        if (state.isRunning) viewModel.pause() else viewModel.start()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = phaseColor)
                ) {
                    Text(
                        if (state.isRunning) "暂停" else "开始",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedButton(onClick = { viewModel.skip() }) {
                    Text("跳过")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Daily stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.completedSessions}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("今日番茄", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.completedSessions * state.focusMinutes}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("今日分钟", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Settings dialog
    if (showSettings) {
        var focusMin by remember { mutableStateOf(state.focusMinutes.toString()) }
        var shortMin by remember { mutableStateOf(state.shortBreakMinutes.toString()) }
        var longMin by remember { mutableStateOf(state.longBreakMinutes.toString()) }

        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("番茄钟设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = focusMin,
                        onValueChange = { if (it.all(Char::isDigit) || it.isEmpty()) focusMin = it },
                        label = { Text("专注时长（分钟）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shortMin,
                        onValueChange = { if (it.all(Char::isDigit) || it.isEmpty()) shortMin = it },
                        label = { Text("短休息（分钟）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = longMin,
                        onValueChange = { if (it.all(Char::isDigit) || it.isEmpty()) longMin = it },
                        label = { Text("长休息（分钟）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    focusMin.toIntOrNull()?.let { viewModel.setFocusMinutes(it) }
                    shortMin.toIntOrNull()?.let { viewModel.setShortBreakMinutes(it) }
                    longMin.toIntOrNull()?.let { viewModel.setLongBreakMinutes(it) }
                    showSettings = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSettings = false }) {
                    Text("取消")
                }
            }
        )
    }
}
