package com.mhss.app.mybrain.pomodoro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class PomodoroPhase(val label: String, val defaultMinutes: Int) {
    FOCUS("专注", 25),
    SHORT_BREAK("短休息", 5),
    LONG_BREAK("长休息", 15)
}

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val completedSessions: Int = 0,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4
)

@KoinViewModel
class PomodoroViewModel : ViewModel() {

    var state by mutableStateOf(PomodoroState())
        private set

    private var timerJob: Job? = null
    private val _todaySessions = MutableStateFlow(0)
    val todaySessions = _todaySessions.asStateFlow()
    private val _todayMinutes = MutableStateFlow(0L)
    val todayMinutes = _todayMinutes.asStateFlow()

    fun start() {
        if (state.isRunning) return
        state = state.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && state.remainingSeconds > 0) {
                delay(1000L)
                state = state.copy(remainingSeconds = state.remainingSeconds - 1)
            }
            if (state.remainingSeconds <= 0) {
                onTimerComplete()
            }
        }
    }

    fun pause() {
        state = state.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun reset() {
        timerJob?.cancel()
        val defaultSeconds = state.phase.defaultMinutes * 60L
        state = state.copy(
            isRunning = false,
            remainingSeconds = defaultSeconds
        )
    }

    fun skip() {
        timerJob?.cancel()
        onTimerComplete()
    }

    fun setFocusMinutes(minutes: Int) {
        state = state.copy(focusMinutes = minutes.coerceIn(5, 60))
        if (state.phase == PomodoroPhase.FOCUS && !state.isRunning) {
            state = state.copy(remainingSeconds = state.focusMinutes * 60L)
        }
    }

    fun setShortBreakMinutes(minutes: Int) {
        state = state.copy(shortBreakMinutes = minutes.coerceIn(1, 15))
        if (state.phase == PomodoroPhase.SHORT_BREAK && !state.isRunning) {
            state = state.copy(remainingSeconds = state.shortBreakMinutes * 60L)
        }
    }

    fun setLongBreakMinutes(minutes: Int) {
        state = state.copy(longBreakMinutes = minutes.coerceIn(5, 30))
        if (state.phase == PomodoroPhase.LONG_BREAK && !state.isRunning) {
            state = state.copy(remainingSeconds = state.longBreakMinutes * 60L)
        }
    }

    private fun onTimerComplete() {
        val isFocus = state.phase == PomodoroPhase.FOCUS
        val newCompleted = if (isFocus) state.completedSessions + 1 else state.completedSessions

        val nextPhase = when {
            isFocus && newCompleted % state.longBreakInterval == 0 -> PomodoroPhase.LONG_BREAK
            isFocus -> PomodoroPhase.SHORT_BREAK
            else -> PomodoroPhase.FOCUS
        }

        val nextSeconds = nextPhase.defaultMinutes * 60L

        state = state.copy(
            phase = nextPhase,
            remainingSeconds = nextSeconds,
            isRunning = false,
            completedSessions = newCompleted
        )

        if (isFocus) {
            _todaySessions.value += 1
            _todayMinutes.value += state.focusMinutes.toLong()
        }
    }
}
