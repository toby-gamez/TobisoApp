package com.tobiso.tobisoappnative.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pomodoro: 25 min of focus, then a 5 min break. Lives in the process, so it keeps running while the user reads on. */
object PomodoroTimer {
    const val FOCUS_SECONDS = 25 * 60
    const val BREAK_SECONDS = 5 * 60

    data class State(
        val breakPhase: Boolean = false,
        val remainingSeconds: Int = FOCUS_SECONDS,
        val running: Boolean = false,
        /** Completed focus sessions since the app started. */
        val completed: Int = 0
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun start() {
        if (job?.isActive == true) return
        _state.update { it.copy(running = true) }
        job = scope.launch {
            while (_state.value.running) {
                delay(1000)
                _state.update { s ->
                    if (s.remainingSeconds > 1) s.copy(remainingSeconds = s.remainingSeconds - 1)
                    else if (s.breakPhase) State(breakPhase = false, remainingSeconds = FOCUS_SECONDS, running = false, completed = s.completed)
                    else State(breakPhase = true, remainingSeconds = BREAK_SECONDS, running = true, completed = s.completed + 1)
                }
                if (!_state.value.running) break
            }
        }
    }

    fun pause() {
        job?.cancel()
        _state.update { it.copy(running = false) }
    }

    fun reset() {
        job?.cancel()
        _state.update { State(completed = it.completed) }
    }
}
