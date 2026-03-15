package com.example.tobisoappnative.viewmodel.dragdrop

import com.example.tobisoappnative.base.UiEffect
import com.example.tobisoappnative.base.UiIntent
import com.example.tobisoappnative.base.UiState
import com.example.tobisoappnative.model.DragDropConfig
import com.example.tobisoappnative.model.ExerciseValidationResult

data class DragDropExerciseState(
    val exerciseTitle: String = "",
    val instructionsMarkdown: String? = null,
    val config: DragDropConfig? = null,
    /** itemId → categoryId */
    val placements: Map<String, String> = emptyMap(),
    val selectedItem: String? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isValidating: Boolean = false,
    val validationResult: ExerciseValidationResult? = null,
    val showResult: Boolean = false,
    val error: String? = null
) : UiState

sealed interface DragDropExerciseIntent : UiIntent {
    data class Load(val exerciseId: Int) : DragDropExerciseIntent
    data class SelectItem(val itemId: String) : DragDropExerciseIntent
    /** Place the currently selected item into a category. */
    data class PlaceInCategory(val categoryId: String) : DragDropExerciseIntent
    /** Remove item from its assigned category (tap on placed item). */
    data class RemoveFromCategory(val itemId: String) : DragDropExerciseIntent
    data class Validate(val exerciseId: Int) : DragDropExerciseIntent
    object Reset : DragDropExerciseIntent
}

sealed interface DragDropExerciseEffect : UiEffect {
    object NavigateBack : DragDropExerciseEffect
    data class ShowSnackbar(val message: String) : DragDropExerciseEffect
}
