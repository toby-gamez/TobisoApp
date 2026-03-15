package com.example.tobisoappnative.viewmodel.circuit

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.example.tobisoappnative.model.CircuitConnection
import com.example.tobisoappnative.model.CircuitConfig
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.repository.ExerciseRepositoryImpl
import com.example.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class CircuitExerciseViewModel(
    application: Application,
    private val getExercise: GetExerciseUseCase
) : BaseAndroidViewModel<CircuitExerciseState, CircuitExerciseIntent, CircuitExerciseEffect>(
    application, CircuitExerciseState()
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    override fun onIntent(intent: CircuitExerciseIntent) = when (intent) {
        is CircuitExerciseIntent.Load -> load(intent.exerciseId)
        is CircuitExerciseIntent.AddComponent -> addComponent(intent.type)
        is CircuitExerciseIntent.DragComponent -> dragComponent(intent.id, intent.dx, intent.dy)
        is CircuitExerciseIntent.TapComponent -> tapComponent(intent.id)
        is CircuitExerciseIntent.RemoveConnection -> removeConnection(intent.from, intent.to)
        is CircuitExerciseIntent.RemoveComponent -> removeComponent(intent.id)
        is CircuitExerciseIntent.UpdateComponentValue -> updateComponentValue(intent.id, intent.value)
        is CircuitExerciseIntent.ToggleSwitch -> toggleSwitch(intent.id)
        CircuitExerciseIntent.Clear -> clear()
    }

    private fun load(exerciseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true) }
            val isOffline = !NetworkUtils.isOnline(getApplication())
            getExercise(exerciseId)
                .onSuccess { exercise ->
                    val config = runCatching {
                        val raw = exercise.configJson
                        if (raw.isBlank() || raw == "null") null
                        else json.decodeFromString<CircuitConfig>(raw)
                    }.getOrNull()

                    val components = config?.components?.map {
                        CircuitComponent(it.id, it.label, it.type, it.value, it.x, it.y)
                    } ?: emptyList()
                    val localConnections = config?.connections?.map {
                        LocalCircuitConnection(it.id, it.from, it.to)
                    } ?: emptyList()
                    val connections = localConnections.map { CircuitConnection(it.from, it.to) }

                    val base = copy(
                        isLoading = false,
                        isOffline = isOffline,
                        exerciseTitle = exercise.title,
                        instructionsMarkdown = exercise.instructionsMarkdown,
                        components = components,
                        connections = connections,
                        localConnections = localConnections,
                        compCounter = components.size
                    )
                    setState { base }
                    reEvaluate()
                }
                .onFailure { e ->
                    setState { copy(isLoading = false, isOffline = isOffline) }
                }
        }
    }

    private fun addComponent(type: String) {
        val counter = currentState.compCounter + 1
        val label = componentLabel(type)
        val defaultValue = when (type) {
            "battery" -> 9.0; "resistor" -> 100.0; "bulb" -> 10.0; "capacitor" -> 100.0; else -> 0.0
        }
        val x = 24.0 + (counter * 28 % 220)
        val y = 24.0 + (counter * 18 % 140)
        val comp = CircuitComponent("comp_$counter", "$label $counter", type, defaultValue, x, y)
        setState { copy(components = components + comp, compCounter = counter) }
        reEvaluate()
    }

    private fun dragComponent(id: String, dx: Float, dy: Float) {
        setState {
            copy(components = components.map { c ->
                if (c.id == id) c.copy(x = c.x + dx, y = c.y + dy) else c
            })
        }
        reEvaluate()
    }

    private fun tapComponent(id: String) {
        val connectingFrom = currentState.connectingFrom
        if (connectingFrom == null) {
            setState { copy(connectingFrom = id) }
        } else if (connectingFrom == id) {
            setState { copy(connectingFrom = null) }
        } else {
            val connId = "conn_${currentState.localConnections.size + 1}"
            val newLocal = LocalCircuitConnection(connId, connectingFrom, id)
            val newConn = CircuitConnection(connectingFrom, id)
            setState {
                copy(
                    localConnections = localConnections + newLocal,
                    connections = connections + newConn,
                    connectingFrom = null
                )
            }
            reEvaluate()
        }
    }

    private fun removeConnection(from: String, to: String) {
        setState {
            copy(
                connections = connections.filterNot { it.from == from && it.to == to },
                localConnections = localConnections.filterNot { it.from == from && it.to == to }
            )
        }
        reEvaluate()
    }

    private fun removeComponent(id: String) {
        setState {
            copy(
                components = components.filterNot { it.id == id },
                connections = connections.filterNot { it.from == id || it.to == id },
                localConnections = localConnections.filterNot { it.from == id || it.to == id },
                connectingFrom = if (connectingFrom == id) null else connectingFrom,
                switchStates = switchStates - id
            )
        }
        reEvaluate()
    }

    private fun updateComponentValue(id: String, value: Double) {
        setState {
            copy(components = components.map { c -> if (c.id == id) c.copy(value = value) else c })
        }
        reEvaluate()
    }

    private fun toggleSwitch(id: String) {
        setState { copy(switchStates = switchStates + (id to !(switchStates[id] ?: false))) }
        reEvaluate()
    }

    private fun clear() {
        setState {
            copy(components = emptyList(), connections = emptyList(), localConnections = emptyList(),
                connectingFrom = null, switchStates = emptyMap(), compCounter = 0, eval = CircuitEvaluation())
        }
    }

    // ──────────────────────────────────────
    // Circuit physics evaluation
    // ──────────────────────────────────────

    private fun reEvaluate() {
        val s = currentState
        setState { copy(eval = evaluateCircuit(s.components, s.connections, s.localConnections, s.switchStates)) }
    }

    private fun evaluateCircuit(
        components: List<CircuitComponent>,
        connections: List<CircuitConnection>,
        localConnections: List<LocalCircuitConnection>,
        switchStates: Map<String, Boolean>
    ): CircuitEvaluation {
        val compsById = components.associateBy { it.id }
        val batteries = components.filter { it.type == "battery" }
        if (batteries.isEmpty()) return CircuitEvaluation()

        val totalVoltage = batteries.sumOf { it.value }

        val graph = mutableMapOf<String, MutableList<String>>()
        components.forEach { graph[it.id] = mutableListOf() }
        connections.forEach { conn ->
            graph[conn.from]?.add(conn.to)
            graph[conn.to]?.add(conn.from)
        }

        fun findClosed(start: String, current: String, visited: MutableSet<String>, path: MutableList<String>): Pair<Boolean, List<String>> {
            path.add(current)
            visited.add(current)
            val comp = compsById[current]
            if (comp?.type == "switch" && switchStates[comp.id] != true && path.size > 1) {
                path.removeAt(path.size - 1); visited.remove(current)
                return Pair(false, path)
            }
            for (neighbor in graph.getOrDefault(current, mutableListOf())) {
                if (neighbor == start && path.size >= 3) { path.add(neighbor); return Pair(true, path.toList()) }
                if (!visited.contains(neighbor)) {
                    val res = findClosed(start, neighbor, visited, path)
                    if (res.first) return res
                }
            }
            path.removeAt(path.size - 1); visited.remove(current)
            return Pair(false, path)
        }

        var foundPath: List<String>? = null
        for (bat in batteries) {
            val res = findClosed(bat.id, bat.id, mutableSetOf(), mutableListOf())
            if (res.first) { foundPath = res.second; break }
        }
        if (foundPath == null) return CircuitEvaluation(hasPower = true, totalVoltage = totalVoltage)

        val visitedSet = foundPath.toSet()
        var totR = 0.0
        val bulbsInPath = mutableListOf<CircuitComponent>()
        for (compId in visitedSet) {
            val comp = compsById[compId] ?: continue
            when (comp.type) {
                "resistor" -> totR += comp.value
                "bulb" -> { val r = if (comp.value > 0) (totalVoltage * totalVoltage) / comp.value else 0.0; totR += r; bulbsInPath.add(comp) }
                "motor" -> totR += 5.0
                "led" -> totR += 100.0
                "buzzer" -> totR += 50.0
            }
        }
        if (totR < 0.1) totR = 0.1
        val current = totalVoltage / totR
        val power = totalVoltage * current

        val metrics = mutableMapOf<String, ComponentMetrics>()
        var warning = ""
        for (bulb in bulbsInPath) {
            val actualPower = current * current * ((totalVoltage * totalVoltage) / bulb.value)
            metrics[bulb.id] = ComponentMetrics(current, totalVoltage, actualPower)
            when {
                actualPower > bulb.value * 1.5 -> warning = "⚠️ ${bulb.label} je přetížená! (${String.format("%.1f", actualPower)}W > ${bulb.value}W)"
                actualPower < bulb.value * 0.5 -> warning = "💡 ${bulb.label} svítí slabě (${String.format("%.1f", actualPower)}W < ${bulb.value}W)"
            }
        }
        for (comp in components) {
            if (!metrics.containsKey(comp.id)) {
                val p = if (comp.type == "resistor") current * current * comp.value else 0.0
                metrics[comp.id] = ComponentMetrics(current, totalVoltage, p)
            }
        }

        val pathPairs = mutableSetOf<Pair<String, String>>()
        for (i in 0 until foundPath.size - 1) { pathPairs.add(Pair(foundPath[i], foundPath[i + 1])); pathPairs.add(Pair(foundPath[i + 1], foundPath[i])) }
        val liveConnIds = localConnections.mapNotNull { lc ->
            if (pathPairs.contains(Pair(lc.from, lc.to))) lc.id else null
        }.toSet()
        val litComps = visitedSet.filter { id -> compsById[id]?.type in listOf("bulb", "led", "motor", "buzzer") }.toSet()

        return CircuitEvaluation(true, true, totalVoltage, totR, current, power, warning, liveConnIds, litComps, metrics)
    }

    // ──────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────

    private fun componentLabel(type: String) = when (type) {
        "battery" -> "Baterie"; "bulb" -> "Žárovka"; "switch" -> "Přepínač"
        "resistor" -> "Odpor"; "capacitor" -> "Kondenzátor"; "led" -> "LED"
        "motor" -> "Motor"; "buzzer" -> "Bzučák"; else -> type
    }

    /** Helper used from Scaffold to avoid deep copying entire state before setState. */
    private fun copy(
        isLoading: Boolean = currentState.isLoading,
        isOffline: Boolean = currentState.isOffline,
        exerciseTitle: String = currentState.exerciseTitle,
        instructionsMarkdown: String? = currentState.instructionsMarkdown,
        components: List<CircuitComponent> = currentState.components,
        connections: List<CircuitConnection> = currentState.connections,
        localConnections: List<LocalCircuitConnection> = currentState.localConnections,
        connectingFrom: String? = currentState.connectingFrom,
        switchStates: Map<String, Boolean> = currentState.switchStates,
        compCounter: Int = currentState.compCounter,
        eval: CircuitEvaluation = currentState.eval
    ) = CircuitExerciseState(exerciseTitle, instructionsMarkdown, components, connections, localConnections, connectingFrom, switchStates, compCounter, eval, isLoading, isOffline)

    fun getBrightness(compId: String): Double {
        val s = currentState
        val comp = s.components.find { it.id == compId } ?: return 0.0
        if (!s.eval.litComponentIds.contains(compId)) return 0.0
        val metrics = s.eval.compMetrics[compId] ?: return 0.0
        return when (comp.type) {
            "bulb" -> if (comp.value <= 0.0) 0.0 else (metrics.power / comp.value).coerceIn(0.0, 2.0)
            "led", "motor", "buzzer" -> (metrics.current / 0.2).coerceIn(0.0, 1.5)
            else -> 0.0
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val repo = ExerciseRepositoryImpl(application, OfflineDataManager(application))
            return CircuitExerciseViewModel(application, GetExerciseUseCase(repo)) as T
        }
    }
}
