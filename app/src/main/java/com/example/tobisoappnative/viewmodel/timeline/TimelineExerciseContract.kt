package com.example.tobisoappnative.viewmodel.timeline

import com.example.tobisoappnative.base.UiEffect
import com.example.tobisoappnative.base.UiIntent
import com.example.tobisoappnative.base.UiState
import com.example.tobisoappnative.model.ExerciseValidationResult
import com.example.tobisoappnative.model.TimelineConfig
import com.example.tobisoappnative.model.TimelineEvent

data class TimelineExerciseState(
    val exerciseTitle: String = "",
    val instructionsMarkdown: String? = null,
    val config: TimelineConfig? = null,
    /** IDs of events in the order user placed them (index n → slotYears[n]). */
    val orderedEvents: List<String> = emptyList(),
    /** All events from config minus already ordered ones. */
    val availableEvents: List<TimelineEvent> = emptyList(),
    /** Years extracted from config in sorted order – each slot maps to one event. */
    val slotYears: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isValidating: Boolean = false,
    val validationResult: ExerciseValidationResult? = null,
    val showResult: Boolean = false,
    val error: String? = null
) : UiState

sealed interface TimelineExerciseIntent : UiIntent {
    data class Load(val exerciseId: Int) : TimelineExerciseIntent
    /** Append eventId to orderedEvents (fills next free slot). */
    data class AddEvent(val eventId: String) : TimelineExerciseIntent
    /** Remove event from orderedEvents (free up its slot). */
    data class RemoveEvent(val eventId: String) : TimelineExerciseIntent
    data class Validate(val exerciseId: Int) : TimelineExerciseIntent
    object Reset : TimelineExerciseIntent
}

sealed interface TimelineExerciseEffect : UiEffect {
    object NavigateBack : TimelineExerciseEffect
    data class ShowSnackbar(val message: String) : TimelineExerciseEffect
}
