package com.example.tobisoappnative.viewmodel.matching

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.example.tobisoappnative.domain.usecase.ValidateExerciseUseCase
import com.example.tobisoappnative.model.MatchingConfig
import com.example.tobisoappnative.model.MatchingPair
import com.example.tobisoappnative.model.MatchingSolution
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.repository.ExerciseRepositoryImpl
import com.example.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MatchingExerciseViewModel(
    application: Application,
    private val getExercise: GetExerciseUseCase,
    private val validateExercise: ValidateExerciseUseCase
) : BaseAndroidViewModel<MatchingExerciseState, MatchingExerciseIntent, MatchingExerciseEffect>(
    application,
    MatchingExerciseState()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun onIntent(intent: MatchingExerciseIntent) {
        when (intent) {
            is MatchingExerciseIntent.Load -> load(intent.exerciseId)
            is MatchingExerciseIntent.SelectLeft -> selectLeft(intent.itemId)
            is MatchingExerciseIntent.SelectRight -> selectRight(intent.itemId)
            is MatchingExerciseIntent.RemovePair -> removePair(intent.pair)
            is MatchingExerciseIntent.Validate -> validate(intent.exerciseId)
            MatchingExerciseIntent.Reset -> reset()
        }
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
                        else json.decodeFromString<MatchingConfig>(raw)
                    }.getOrNull()?.let { c ->
                        c.copy(right = c.right.shuffled())
                    }
                    setState {
                        copy(
                            isLoading = false,
                            isOffline = isOffline,
                            exerciseTitle = exercise.title,
                            instructionsMarkdown = exercise.instructionsMarkdown,
                            config = config,
                            pairs = emptyList(),
                            selectedLeft = null,
                            selectedRight = null,
                            showResult = false,
                            validationResult = null,
                            error = if (config == null) "Nelze načíst konfiguraci cvičení" else null
                        )
                    }
                }
                .onFailure { e ->
                    setState {
                        copy(
                            isLoading = false,
                            isOffline = isOffline,
                            error = e.message ?: "Chyba při načítání cvičení"
                        )
                    }
                }
        }
    }

    private fun selectLeft(itemId: String) {
        setState { copy(selectedLeft = if (selectedLeft == itemId) null else itemId) }
        tryCreatePair()
    }

    private fun selectRight(itemId: String) {
        setState { copy(selectedRight = if (selectedRight == itemId) null else itemId) }
        tryCreatePair()
    }

    private fun tryCreatePair() {
        val left = currentState.selectedLeft ?: return
        val right = currentState.selectedRight ?: return
        setState {
            copy(
                pairs = pairs + MatchingPair(left, right),
                selectedLeft = null,
                selectedRight = null,
                showResult = false
            )
        }
    }

    private fun removePair(pair: MatchingPair) {
        if (currentState.showResult) return
        setState {
            copy(pairs = pairs.filter { it.leftId != pair.leftId || it.rightId != pair.rightId })
        }
    }

    private fun validate(exerciseId: Int) {
        if (currentState.pairs.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isValidating = true) }
            val solutionJson = json.encodeToString(MatchingSolution(currentState.pairs))
            validateExercise(exerciseId, solutionJson)
                .onSuccess { result ->
                    setState {
                        copy(isValidating = false, validationResult = result, showResult = true)
                    }
                }
                .onFailure { e ->
                    setState { copy(isValidating = false, showResult = true) }
                    emitEffect(MatchingExerciseEffect.ShowSnackbar(e.message ?: "Chyba při validaci"))
                }
        }
    }

    private fun reset() {
        setState {
            copy(
                pairs = emptyList(),
                selectedLeft = null,
                selectedRight = null,
                showResult = false,
                validationResult = null,
                error = null
            )
        }
    }

    // ──────────────────────────────────────
    // Factory
    // ──────────────────────────────────────
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val offlineDataManager = OfflineDataManager(application)
            val repo = ExerciseRepositoryImpl(application, offlineDataManager)
            return MatchingExerciseViewModel(
                application,
                GetExerciseUseCase(repo),
                ValidateExerciseUseCase(repo)
            ) as T
        }
    }
}
