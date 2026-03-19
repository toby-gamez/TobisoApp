package com.tobiso.tobisoappnative.viewmodel.dragdrop

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseAndroidViewModel
import com.tobiso.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.tobiso.tobisoappnative.domain.usecase.ValidateExerciseUseCase
import com.tobiso.tobisoappnative.model.DragDropConfig
import com.tobiso.tobisoappnative.model.DragDropSolution
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class DragDropExerciseViewModel @Inject constructor(
    application: Application,
    private val getExercise: GetExerciseUseCase,
    private val validateExercise: ValidateExerciseUseCase
) : BaseAndroidViewModel<DragDropExerciseState, DragDropExerciseIntent, DragDropExerciseEffect>(
    application, DragDropExerciseState()
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    override fun onIntent(intent: DragDropExerciseIntent) = when (intent) {
        is DragDropExerciseIntent.Load -> load(intent.exerciseId)
        is DragDropExerciseIntent.SelectItem -> selectItem(intent.itemId)
        is DragDropExerciseIntent.PlaceInCategory -> placeInCategory(intent.categoryId)
        is DragDropExerciseIntent.RemoveFromCategory -> removeFromCategory(intent.itemId)
        is DragDropExerciseIntent.Validate -> validate(intent.exerciseId)
        DragDropExerciseIntent.Reset -> reset()
    }

    private fun load(exerciseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true, error = null) }
            val isOffline = !NetworkUtils.isOnline(getApplication())
            getExercise(exerciseId)
                .onSuccess { exercise ->
                    val config = runCatching {
                        val raw = exercise.configJson
                        if (raw.isBlank() || raw == "null") null
                        else json.decodeFromString<DragDropConfig>(raw)
                    }.getOrNull()?.let { c ->
                        c.copy(items = c.items.shuffled())
                    }
                    setState {
                        copy(
                            isLoading = false,
                            isOffline = isOffline,
                            exerciseTitle = exercise.title,
                            instructionsMarkdown = exercise.instructionsMarkdown,
                            config = config,
                            placements = emptyMap(),
                            selectedItem = null,
                            showResult = false,
                            validationResult = null,
                            error = if (config == null) "Nelze načíst konfiguraci cvičení" else null
                        )
                    }
                }
                .onFailure { e ->
                    setState {
                        copy(isLoading = false, isOffline = isOffline, error = e.message ?: "Chyba při načítání cvičení")
                    }
                }
        }
    }

    private fun selectItem(itemId: String) {
        setState { copy(selectedItem = if (selectedItem == itemId) null else itemId) }
    }

    private fun placeInCategory(categoryId: String) {
        val itemId = currentState.selectedItem ?: return
        setState {
            copy(
                placements = placements + (itemId to categoryId),
                selectedItem = null,
                showResult = false
            )
        }
    }

    private fun removeFromCategory(itemId: String) {
        if (currentState.showResult) return
        setState { copy(placements = placements - itemId) }
    }

    private fun validate(exerciseId: Int) {
        if (currentState.placements.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isValidating = true) }
            val solutionJson = json.encodeToString(DragDropSolution(currentState.placements))
            validateExercise(exerciseId, solutionJson)
                .onSuccess { result ->
                    setState { copy(isValidating = false, validationResult = result, showResult = true) }
                }
                .onFailure { e ->
                    setState { copy(isValidating = false, showResult = true) }
                    emitEffect(DragDropExerciseEffect.ShowSnackbar(e.message ?: "Chyba při validaci"))
                }
        }
    }

    private fun reset() {
        setState {
            copy(placements = emptyMap(), selectedItem = null, showResult = false, validationResult = null, error = null)
        }
    }
}
