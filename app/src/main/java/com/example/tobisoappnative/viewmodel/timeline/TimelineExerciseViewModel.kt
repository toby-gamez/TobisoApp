package com.example.tobisoappnative.viewmodel.timeline

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.example.tobisoappnative.domain.usecase.ValidateExerciseUseCase
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.TimelineConfig
import com.example.tobisoappnative.model.TimelineSolution
import com.example.tobisoappnative.repository.ExerciseRepositoryImpl
import com.example.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TimelineExerciseViewModel(
    application: Application,
    private val getExercise: GetExerciseUseCase,
    private val validateExercise: ValidateExerciseUseCase
) : BaseAndroidViewModel<TimelineExerciseState, TimelineExerciseIntent, TimelineExerciseEffect>(
    application, TimelineExerciseState()
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    override fun onIntent(intent: TimelineExerciseIntent) = when (intent) {
        is TimelineExerciseIntent.Load -> load(intent.exerciseId)
        is TimelineExerciseIntent.AddEvent -> addEvent(intent.eventId)
        is TimelineExerciseIntent.RemoveEvent -> removeEvent(intent.eventId)
        is TimelineExerciseIntent.Validate -> validate(intent.exerciseId)
        TimelineExerciseIntent.Reset -> reset()
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
                        else json.decodeFromString<TimelineConfig>(raw)
                    }.getOrNull()
                    val slotYears = config?.events?.mapNotNull { it.year }?.sorted() ?: emptyList()
                    setState {
                        copy(
                            isLoading = false,
                            isOffline = isOffline,
                            exerciseTitle = exercise.title,
                            instructionsMarkdown = exercise.instructionsMarkdown,
                            config = config,
                            orderedEvents = emptyList(),
                            availableEvents = config?.events ?: emptyList(),
                            slotYears = slotYears,
                            showResult = false,
                            validationResult = null,
                            error = if (config == null) "Nelze načíst konfiguraci cvičení" else null
                        )
                    }
                }
                .onFailure { e ->
                    setState { copy(isLoading = false, isOffline = isOffline, error = e.message ?: "Chyba při načítání cvičení") }
                }
        }
    }

    private fun addEvent(eventId: String) {
        val s = currentState
        if (s.orderedEvents.size >= s.slotYears.size) return
        val event = s.availableEvents.find { it.id == eventId } ?: return
        setState {
            copy(
                orderedEvents = orderedEvents + eventId,
                availableEvents = availableEvents.filter { it.id != eventId },
                showResult = false
            )
        }
    }

    private fun removeEvent(eventId: String) {
        val event = currentState.config?.events?.find { it.id == eventId } ?: return
        setState {
            copy(
                orderedEvents = orderedEvents.filter { it != eventId },
                availableEvents = (availableEvents + event).sortedBy { currentState.config?.events?.indexOfFirst { e -> e.id == it.id } ?: 0 },
                showResult = false
            )
        }
    }

    private fun validate(exerciseId: Int) {
        if (currentState.orderedEvents.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isValidating = true) }
            val solutionJson = json.encodeToString(TimelineSolution(currentState.orderedEvents))
            validateExercise(exerciseId, solutionJson)
                .onSuccess { result ->
                    setState { copy(isValidating = false, validationResult = result, showResult = true) }
                }
                .onFailure { e ->
                    setState { copy(isValidating = false, showResult = true) }
                    emitEffect(TimelineExerciseEffect.ShowSnackbar(e.message ?: "Chyba při validaci"))
                }
        }
    }

    private fun reset() {
        val config = currentState.config
        setState {
            copy(
                orderedEvents = emptyList(),
                availableEvents = config?.events ?: emptyList(),
                showResult = false,
                validationResult = null,
                error = null
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val repo = ExerciseRepositoryImpl(application, OfflineDataManager(application))
            return TimelineExerciseViewModel(application, GetExerciseUseCase(repo), ValidateExerciseUseCase(repo)) as T
        }
    }
}
