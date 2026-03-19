package com.tobiso.tobisoappnative.viewmodel.matching

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.ExerciseValidationResult
import com.tobiso.tobisoappnative.model.MatchingConfig
import com.tobiso.tobisoappnative.model.MatchingPair

// ──────────────────────────────────────────
// State
// ──────────────────────────────────────────
data class MatchingExerciseState(
    val exerciseTitle: String = "",
    val instructionsMarkdown: String? = null,
    val config: MatchingConfig? = null,
    val pairs: List<MatchingPair> = emptyList(),
    val selectedLeft: String? = null,
    val selectedRight: String? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isValidating: Boolean = false,
    val validationResult: ExerciseValidationResult? = null,
    val showResult: Boolean = false,
    val error: String? = null
) : UiState

// ──────────────────────────────────────────
// Intent
// ──────────────────────────────────────────
sealed interface MatchingExerciseIntent : UiIntent {
    data class Load(val exerciseId: Int) : MatchingExerciseIntent
    data class SelectLeft(val itemId: String) : MatchingExerciseIntent
    data class SelectRight(val itemId: String) : MatchingExerciseIntent
    data class RemovePair(val pair: MatchingPair) : MatchingExerciseIntent
    data class Validate(val exerciseId: Int) : MatchingExerciseIntent
    object Reset : MatchingExerciseIntent
}

// ──────────────────────────────────────────
// Effect  (one-shot events for the screen)
// ──────────────────────────────────────────
sealed interface MatchingExerciseEffect : UiEffect {
    object NavigateBack : MatchingExerciseEffect
    data class ShowSnackbar(val message: String) : MatchingExerciseEffect
}
