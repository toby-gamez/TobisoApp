package com.tobiso.tobisoappnative.viewmodel.simulation

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import kotlinx.serialization.json.JsonObject

data class SimulationExerciseState(
    val exerciseTitle: String = "",
    val instructionsMarkdown: String? = null,
    /** "kepler" | "projectile" | "gravity-field" | "circuit"; defaults to kepler like the web. */
    val kind: String = "",
    val config: JsonObject = JsonObject(emptyMap()),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
) : UiState

sealed interface SimulationExerciseIntent : UiIntent {
    data class Load(val exerciseId: Int) : SimulationExerciseIntent
}

sealed interface SimulationExerciseEffect : UiEffect {
    object NavigateBack : SimulationExerciseEffect
}
