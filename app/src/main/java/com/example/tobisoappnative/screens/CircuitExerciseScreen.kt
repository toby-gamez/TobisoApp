package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.model.CircuitConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitExerciseScreen(
    exerciseId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val currentExercise by viewModel.currentExercise.collectAsState()
    val exerciseLoading by viewModel.exercisesLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    val json = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    data class LocalComponent(
        val id: String,
        val label: String,
        val type: String,
        var value: Double = 0.0,
        var x: Double = 0.0,
        var y: Double = 0.0
    )

    data class LocalConnection(val id: String? = null, val from: String, val to: String)

    data class ComponentMetrics(val current: Double = 0.0, val voltage: Double = 0.0, val power: Double = 0.0)

    var components by remember { mutableStateOf<List<LocalComponent>>(emptyList()) }
    var compCounter by remember { mutableStateOf(0) }
    var selectedA by remember { mutableStateOf<String?>(null) }
    var selectedB by remember { mutableStateOf<String?>(null) }
    var connectingFrom by remember { mutableStateOf<String?>(null) }
    var connections by remember { mutableStateOf<List<CircuitConnection>>(emptyList()) }
    var localConnections by remember { mutableStateOf<List<LocalConnection>>(emptyList()) }

    // Parse configJson if present
    LaunchedEffect(currentExercise) {
        currentExercise?.let { ex ->
            val raw = ex.configJson
            if (!raw.isNullOrBlank() && raw != "null") {
                try {
                    val cfg = json.decodeFromString<com.example.tobisoappnative.model.CircuitConfig>(raw)
                    // Map to local components/connections
                    components = cfg.components.map {
                        LocalComponent(it.id, it.label, it.type, it.value, it.x, it.y)
                    }
                    compCounter = components.size
                    localConnections = cfg.connections.map { LocalConnection(it.id, it.from, it.to) }
                    connections = cfg.connections.map { CircuitConnection(it.from, it.to) }
                } catch (e: Exception) {
                    // ignore parse errors and keep empty
                }
            }
        }
    }

    // Circuit evaluation state
    var hasPower by remember { mutableStateOf(false) }
    var circuitClosed by remember { mutableStateOf(false) }
    var totalVoltage by remember { mutableStateOf(0.0) }
    var totalResistance by remember { mutableStateOf(0.0) }
    var circuitCurrent by remember { mutableStateOf(0.0) }
    var circuitPower by remember { mutableStateOf(0.0) }
    var warning by remember { mutableStateOf("") }
    var liveConnections by remember { mutableStateOf<Set<String>>(emptySet()) }
    var litComponents by remember { mutableStateOf<Set<String>>(emptySet()) }
    var compMetrics by remember { mutableStateOf<Map<String, ComponentMetrics>>(emptyMap()) }
    var switchStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    fun getBrightness(comp: LocalComponent): Double {
        if (!litComponents.contains(comp.id)) return 0.0
        val metrics = compMetrics[comp.id] ?: return 0.0

        return when (comp.type) {
            "bulb" -> {
                if (comp.value <= 0.0) 0.0 else (metrics.power / comp.value).coerceIn(0.0, 2.0)
            }
            "led", "motor", "buzzer" -> {
                (metrics.current / 0.2).coerceIn(0.0, 1.5)
            }
            else -> 0.0
        }
    }

    @Composable
    fun brightnessColor(comp: LocalComponent): Color {
        val b = getBrightness(comp)
        if (b <= 0.0) return MaterialTheme.colorScheme.primaryContainer
        return when (comp.type) {
            "bulb" -> when {
                b < 0.3 -> Color(0xFFFF8C00) // dim orange
                b < 0.7 -> Color(0xFFFFC107) // warm yellow
                b <= 1.2 -> Color(0xFFFFF59D) // bright yellow
                else -> Color(0xFFFFEB3B) // overbright
            }
            "led" -> if (b < 0.5) Color(0xFF90CAF9) else Color(0xFF42A5F5)
            "motor" -> if (b < 0.5) Color(0xFFB39DDB) else Color(0xFF7E57C2)
            "buzzer" -> if (b < 0.5) Color(0xFFFFCC80) else Color(0xFFFF8A65)
            else -> MaterialTheme.colorScheme.primaryContainer
        }
    }

    fun evaluateCircuit() {
        warning = ""
        liveConnections = emptySet()
        litComponents = emptySet()
        compMetrics = emptyMap()

        val compsById = components.associateBy { it.id }

        // Batteries
        val batteries = components.filter { it.type == "battery" }
        if (batteries.isEmpty()) {
            hasPower = false
            circuitClosed = false
            totalVoltage = 0.0
            return
        }
        hasPower = true
        totalVoltage = batteries.sumOf { it.value }

        // Build graph
        val graph = mutableMapOf<String, MutableList<String>>()
        components.forEach { graph[it.id] = mutableListOf() }
        connections.forEach { conn ->
            graph[conn.from]?.add(conn.to)
            graph[conn.to]?.add(conn.from)
        }

        // DFS to find closed loop starting from each battery
        fun findClosed(start: String, current: String, visited: MutableSet<String>, path: MutableList<String>): Pair<Boolean, List<String>> {
            path.add(current)
            visited.add(current)

            val currComp = compsById[current]
            if (currComp?.type == "switch" && (switchStates[currComp.id] != true) && path.size > 1) {
                path.removeAt(path.size - 1)
                visited.remove(current)
                return Pair(false, path)
            }

            for (neighbor in graph.getOrDefault(current, mutableListOf())) {
                if (neighbor == start && path.size >= 3) {
                    path.add(neighbor)
                    return Pair(true, path.toList())
                }
                if (!visited.contains(neighbor)) {
                    val res = findClosed(start, neighbor, visited, path)
                    if (res.first) return res
                }
            }

            path.removeAt(path.size - 1)
            visited.remove(current)
            return Pair(false, path)
        }

        var foundPath: List<String>? = null
        for (bat in batteries) {
            val res = findClosed(bat.id, bat.id, mutableSetOf(), mutableListOf())
            if (res.first) {
                foundPath = res.second
                break
            }
        }

        if (foundPath == null) {
            circuitClosed = false
            return
        }
        circuitClosed = true

        // Compute resistance along path
        val visitedSet = foundPath.toSet()
        var totR = 0.0
        val bulbsInPath = mutableListOf<LocalComponent>()
        for (compId in visitedSet) {
            val comp = compsById[compId]
            if (comp != null) {
                when (comp.type) {
                    "resistor" -> totR += comp.value
                    "bulb" -> {
                        val r = if (comp.value > 0.0) (totalVoltage * totalVoltage) / comp.value else 0.0
                        totR += r
                        bulbsInPath.add(comp)
                    }
                    "motor" -> totR += 5.0
                    "led" -> totR += 100.0
                    "buzzer" -> totR += 50.0
                }
            }
        }
        if (totR < 0.1) totR = 0.1
        totalResistance = totR
        circuitCurrent = if (totalResistance > 0.0) totalVoltage / totalResistance else 0.0
        circuitPower = totalVoltage * circuitCurrent

        val metrics = mutableMapOf<String, ComponentMetrics>()
        for (bulb in bulbsInPath) {
            val actualPower = circuitCurrent * circuitCurrent * ((totalVoltage * totalVoltage) / bulb.value)
            metrics[bulb.id] = ComponentMetrics(circuitCurrent, totalVoltage, actualPower)
            if (actualPower > bulb.value * 1.5) {
                warning = "⚠️ ${bulb.label} je přetížená! (${String.format("%.1f", actualPower)}W > ${bulb.value}W)"
            } else if (actualPower < bulb.value * 0.5) {
                warning = "💡 ${bulb.label} svítí slabě (${String.format("%.1f", actualPower)}W < ${bulb.value}W)"
            }
        }

        // Fill metrics for other comps
        for (comp in components) {
            if (!metrics.containsKey(comp.id)) {
                val p = if (comp.type == "resistor") circuitCurrent * circuitCurrent * comp.value else 0.0
                metrics[comp.id] = ComponentMetrics(circuitCurrent, totalVoltage, p)
            }
        }
        compMetrics = metrics

        // Mark live connections along path
        val pathPairs = mutableSetOf<Pair<String, String>>()
        for (i in 0 until foundPath.size - 1) {
            val a = foundPath[i]
            val b = foundPath[i + 1]
            pathPairs.add(Pair(a, b))
            pathPairs.add(Pair(b, a))
        }

        val liveConnIds = localConnections.mapNotNull { lc ->
            if (pathPairs.contains(Pair(lc.from, lc.to))) lc.id else null
        }.toSet()
        liveConnections = liveConnIds

        // Mark lit components
        litComponents = visitedSet.filter { id ->
            val t = compsById[id]?.type
            t == "bulb" || t == "led" || t == "motor" || t == "buzzer"
        }.toSet()
    }

    // Trigger initial evaluation when components/connections change
    LaunchedEffect(components, connections, localConnections, switchStates) {
        evaluateCircuit()
    }

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentExercise?.title ?: "Cvičení: obvod") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
            ) {
            if (exerciseLoading && currentExercise == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (isOffline) {
                Text(
                    text = "Offline režim: kontrola vyžaduje internet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Instructions
            currentExercise?.instructionsMarkdown?.let { instructions ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        androidx.compose.material3.Text(text = instructions)
                    }
                }
            }

            // Palette buttons (playground)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("battery", "bulb", "switch", "resistor", "capacitor", "led", "motor", "buzzer")
                types.forEach { type ->
                    Button(
                        onClick = {
                        compCounter += 1
                        val id = "comp_" + compCounter
                        val label = when (type) {
                            "battery" -> "Baterie"
                            "bulb" -> "Žárovka"
                            "switch" -> "Přepínač"
                            "resistor" -> "Odpor"
                            "capacitor" -> "Kondenzátor"
                            "led" -> "LED"
                            "motor" -> "Motor"
                            "buzzer" -> "Bzučák"
                            else -> type
                        }
                        val defaultValue = when (type) {
                            "battery" -> 9.0
                            "resistor" -> 100.0
                            "bulb" -> 10.0
                            "capacitor" -> 100.0
                            else -> 0.0
                        }
                        // assign a default position so components appear in the playground
                        val defaultX = 24.0 + (compCounter * 28 % 220)
                        val defaultY = 24.0 + (compCounter * 18 % 140)
                        components = components + LocalComponent(id, "$label $compCounter", type, value = defaultValue, x = defaultX, y = defaultY)
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 120.dp)
                    ) {
                        Text(
                            when (type) {
                                "battery" -> "Baterie"
                                "bulb" -> "Žárovka"
                                "switch" -> "Přepínač"
                                "resistor" -> "Odpor"
                                "capacitor" -> "Kondenzátor"
                                "led" -> "LED"
                                "motor" -> "Motor"
                                "buzzer" -> "Bzučák"
                                else -> type
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playground canvas with draggable components
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(bottom = 12.dp)
            ) {
                val density = LocalDensity.current
                val compSizeDp = 80.dp
                val compSizePx = with(density) { compSizeDp.toPx() }

                // pointer state for preview line
                var previewPointer by remember { mutableStateOf<Offset?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // draw connections and preview
                    Canvas(modifier = Modifier.matchParentSize()) {
                        connections.forEach { conn ->
                            val a = components.find { it.id == conn.from }
                            val b = components.find { it.id == conn.to }
                            if (a != null && b != null) {
                                drawLine(
                                    color = if (liveConnections.contains(localConnections.find { it.from == conn.from && it.to == conn.to }?.id)) Color(0xFFFFC107) else Color.Gray,
                                    start = Offset(a.x.toFloat() + compSizePx / 2f, a.y.toFloat() + compSizePx / 2f),
                                    end = Offset(b.x.toFloat() + compSizePx / 2f, b.y.toFloat() + compSizePx / 2f),
                                    strokeWidth = 6f
                                )
                            }
                        }

                        // preview line while connecting
                        if (connectingFrom != null && previewPointer != null) {
                            val fromComp = components.find { it.id == connectingFrom }
                            if (fromComp != null) {
                                drawLine(
                                    color = Color(0xFF0D6EFD),
                                    start = Offset(fromComp.x.toFloat() + compSizePx / 2f, fromComp.y.toFloat() + compSizePx / 2f),
                                    end = previewPointer!!,
                                    strokeWidth = 4f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }
                        }
                    }

                    // draw components
                    components.forEach { comp ->
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(comp.x.roundToInt(), comp.y.roundToInt()) }
                                .size(compSizeDp)
                                .pointerInput(comp.id) {
                                    detectDragGestures { change, dragAmount ->
                                        components = components.map { c -> if (c.id == comp.id) c.copy(x = c.x + dragAmount.x, y = c.y + dragAmount.y) else c }
                                        evaluateCircuit()
                                    }
                                }
                                .pointerInput(comp.id) {
                                    detectTapGestures(onTap = {
                                        if (connectingFrom == null) connectingFrom = comp.id
                                        else if (connectingFrom != comp.id) {
                                            val connId = "conn_${localConnections.size + 1}"
                                            localConnections = localConnections + LocalConnection(connId, connectingFrom!!, comp.id)
                                            connections = connections + CircuitConnection(from = connectingFrom!!, to = comp.id)
                                            connectingFrom = null
                                            selectedA = null
                                            selectedB = null
                                            evaluateCircuit()
                                        } else {
                                            connectingFrom = null
                                        }
                                    })
                                }
                                .background(
                                    when {
                                        connectingFrom == comp.id -> MaterialTheme.colorScheme.secondaryContainer
                                        comp.type in listOf("bulb", "led", "motor", "buzzer") -> brightnessColor(comp)
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = comp.label, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                if (comp.type in listOf("bulb", "led", "motor", "buzzer")) {
                                    val b = getBrightness(comp)
                                    val hint = when {
                                        b <= 0.05 -> "vypnuto"
                                        b < 0.3 -> "slabě"
                                        b < 0.7 -> "středně"
                                        b <= 1.2 -> "silně"
                                        else -> "přetíženo"
                                    }
                                    Text(hint, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Connections list
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Spojení", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        connections.forEach { conn ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                val fromLabel = components.find { it.id == conn.from }?.label ?: conn.from
                                val toLabel = components.find { it.id == conn.to }?.label ?: conn.to
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$fromLabel → $toLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row {
                                        TextButton(onClick = {
                                            // remove matching local connection(s) and connection
                                            connections = connections.filterNot { it.from == conn.from && it.to == conn.to }
                                            localConnections = localConnections.filterNot { it.from == conn.from && it.to == conn.to }
                                            evaluateCircuit()
                                        }) { Text("Smazat") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Component settings (batteries, resistors, bulbs, capacitors, switches)
            if (components.any { it.type == "battery" || it.type == "resistor" || it.type == "bulb" || it.type == "capacitor" || it.type == "switch" }) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Nastavení komponent", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        components.filter { it.type == "battery" || it.type == "resistor" || it.type == "bulb" || it.type == "capacitor" || it.type == "switch" }
                            .forEach { comp ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(comp.label, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (comp.type == "switch") {
                                        val state = switchStates[comp.id] ?: false
                                        Button(onClick = {
                                            switchStates = switchStates + (comp.id to !state)
                                            evaluateCircuit()
                                        }) { Text(if (state) "Zapnuto" else "Vypnuto") }
                                    } else {
                                        var textVal by remember { mutableStateOf(comp.value.toString()) }
                                        val unit = when (comp.type) {
                                            "battery" -> "V"
                                            "resistor" -> "Ω"
                                            "bulb" -> "W"
                                            "capacitor" -> "μF"
                                            else -> ""
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = textVal,
                                                onValueChange = {
                                                    textVal = it
                                                    val v = it.toDoubleOrNull()
                                                    if (v != null) {
                                                        components = components.map { c -> if (c.id == comp.id) c.copy(value = v) else c }
                                                        evaluateCircuit()
                                                    }
                                                },
                                                singleLine = true,
                                                modifier = Modifier.width(120.dp)
                                            )
                                            if (unit.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(unit, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    IconButton(onClick = {
                                        components = components.filterNot { it.id == comp.id }
                                        connections = connections.filterNot { it.from == comp.id || it.to == comp.id }
                                        localConnections = localConnections.filterNot { it.from == comp.id || it.to == comp.id }
                                        if (selectedA == comp.id) selectedA = null
                                        if (selectedB == comp.id) selectedB = null
                                        if (connectingFrom == comp.id) connectingFrom = null
                                        evaluateCircuit()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Smazat")
                                    }
                                }
                            }
                    }
                }
            }

            // Circuit calculations
            if (circuitClosed && hasPower) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Výpočty obvodu", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Napětí")
                                Text("${String.format("%.1f", totalVoltage)} V", style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Celkový odpor")
                                Text("${String.format("%.1f", totalResistance)} Ω", style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Proud")
                                Text("${String.format("%.2f", circuitCurrent)} A", style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Výkon")
                                Text("${String.format("%.1f", circuitPower)} W", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (warning.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(warning, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // clear everything
                    components = emptyList()
                    compCounter = 0
                    connections = emptyList()
                    selectedA = null
                    selectedB = null
                }) { Text("Vymazat") }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
