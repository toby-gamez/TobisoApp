package com.tobiso.tobisoappnative.viewmodel.simulation

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseAndroidViewModel
import com.tobiso.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class SimulationExerciseViewModel @Inject constructor(
    application: Application,
    private val getExercise: GetExerciseUseCase
) : BaseAndroidViewModel<SimulationExerciseState, SimulationExerciseIntent, SimulationExerciseEffect>(
    application, SimulationExerciseState()
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun onIntent(intent: SimulationExerciseIntent) = when (intent) {
        is SimulationExerciseIntent.Load -> load(intent.exerciseId)
    }

    private fun load(exerciseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true) }
            val isOffline = !NetworkUtils.isOnline(getApplication())
            getExercise(exerciseId)
                .onSuccess { exercise ->
                    // Invalid or missing config falls back to the default Kepler simulation, as on the web.
                    val config = runCatching {
                        val raw = exercise.configJson
                        if (raw.isNullOrBlank() || raw == "null") JsonObject(emptyMap())
                        else json.parseToJsonElement(raw).jsonObject
                    }.getOrDefault(JsonObject(emptyMap()))
                    val kind = config["kind"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() } ?: "kepler"
                    setState {
                        copy(
                            isLoading = false,
                            isOffline = isOffline,
                            exerciseTitle = exercise.title ?: "",
                            instructionsMarkdown = exercise.instructionsMarkdown,
                            kind = kind,
                            config = config
                        )
                    }
                }
                .onFailure { e ->
                    setState { copy(isLoading = false, isOffline = isOffline, error = e.message) }
                }
        }
    }
}
