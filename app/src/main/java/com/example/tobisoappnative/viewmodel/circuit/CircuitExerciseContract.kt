package com.example.tobisoappnative.viewmodel.circuit

import com.example.tobisoappnative.base.UiEffect
import com.example.tobisoappnative.base.UiIntent
import com.example.tobisoappnative.base.UiState
import com.example.tobisoappnative.model.CircuitConnection

// ──────────────────────────────────────────
// Domain models for the circuit sandbox
// ──────────────────────────────────────────

data class CircuitComponent(
    val id: String,
    val label: String,
    val type: String,
    val value: Double = 0.0,
    val x: Double = 0.0,
    val y: Double = 0.0
)

data class LocalCircuitConnection(
    val id: String,
    val from: String,
    val to: String
)

data class ComponentMetrics(
    val current: Double = 0.0,
    val voltage: Double = 0.0,
    val power: Double = 0.0
)

data class CircuitEvaluation(
    val hasPower: Boolean = false,
    val circuitClosed: Boolean = false,
    val totalVoltage: Double = 0.0,
    val totalResistance: Double = 0.0,
    val circuitCurrent: Double = 0.0,
    val circuitPower: Double = 0.0,
    val warning: String = "",
    val liveConnectionIds: Set<String> = emptySet(),
    val litComponentIds: Set<String> = emptySet(),
    val compMetrics: Map<String, ComponentMetrics> = emptyMap()
)

// ──────────────────────────────────────────
// MVI
// ──────────────────────────────────────────

data class CircuitExerciseState(
    val exerciseTitle: String = "",
    val instructionsMarkdown: String? = null,
    val components: List<CircuitComponent> = emptyList(),
    val connections: List<CircuitConnection> = emptyList(),
    val localConnections: List<LocalCircuitConnection> = emptyList(),
    val connectingFrom: String? = null,
    val switchStates: Map<String, Boolean> = emptyMap(),
    val compCounter: Int = 0,
    val eval: CircuitEvaluation = CircuitEvaluation(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false
) : UiState

sealed interface CircuitExerciseIntent : UiIntent {
    data class Load(val exerciseId: Int) : CircuitExerciseIntent
    data class AddComponent(val type: String) : CircuitExerciseIntent
    data class DragComponent(val id: String, val dx: Float, val dy: Float) : CircuitExerciseIntent
    data class TapComponent(val id: String) : CircuitExerciseIntent
    data class RemoveConnection(val from: String, val to: String) : CircuitExerciseIntent
    data class RemoveComponent(val id: String) : CircuitExerciseIntent
    data class UpdateComponentValue(val id: String, val value: Double) : CircuitExerciseIntent
    data class ToggleSwitch(val id: String) : CircuitExerciseIntent
    object Clear : CircuitExerciseIntent
}

sealed interface CircuitExerciseEffect : UiEffect {
    object NavigateBack : CircuitExerciseEffect
}
