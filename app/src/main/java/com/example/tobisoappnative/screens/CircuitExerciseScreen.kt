package com.example.tobisoappnative.screens

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.circuit.CircuitComponent
import com.example.tobisoappnative.viewmodel.circuit.CircuitExerciseIntent
import com.example.tobisoappnative.viewmodel.circuit.CircuitExerciseState
import com.example.tobisoappnative.viewmodel.circuit.CircuitExerciseViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitExerciseScreen(
    exerciseId: Int,
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val vm: CircuitExerciseViewModel = viewModel(factory = CircuitExerciseViewModel.Factory(application))
    val state by vm.uiState.collectAsState()

    LaunchedEffect(exerciseId) {
        vm.onIntent(CircuitExerciseIntent.Load(exerciseId))
    }

    fun brightnessColor(comp: CircuitComponent): Color {
        val b = vm.getBrightness(comp.id)
        if (b <= 0.0) return Color.Unspecified
        return when (comp.type) {
            "bulb" -> when {
                b < 0.3 -> Color(0xFFFF8C00)
                b < 0.7 -> Color(0xFFFFC107)
                b <= 1.2 -> Color(0xFFFFF59D)
                else -> Color(0xFFFFEB3B)
            }
            "led" -> if (b < 0.5) Color(0xFF90CAF9) else Color(0xFF42A5F5)
            "motor" -> if (b < 0.5) Color(0xFFB39DDB) else Color(0xFF7E57C2)
            "buzzer" -> if (b < 0.5) Color(0xFFFFCC80) else Color(0xFFFF8A65)
            else -> Color.Unspecified
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.exerciseTitle.ifEmpty { "Cvičení: obvod" }) },
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
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (state.isOffline) {
                Text(
                    text = "Offline režim: kontrola vyžaduje internet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Instructions
            state.instructionsMarkdown?.let { instructions ->
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
                        onClick = { vm.onIntent(CircuitExerciseIntent.AddComponent(type)) },
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

                var previewPointer by remember { mutableStateOf<Offset?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // draw connections and preview
                    Canvas(modifier = Modifier.matchParentSize()) {
                        state.connections.forEach { conn ->
                            val a = state.components.find { it.id == conn.from }
                            val b = state.components.find { it.id == conn.to }
                            if (a != null && b != null) {
                                val lc = state.localConnections.find { it.from == conn.from && it.to == conn.to }
                                drawLine(
                                    color = if (lc != null && state.eval.liveConnectionIds.contains(lc.id)) Color(0xFFFFC107) else Color.Gray,
                                    start = Offset(a.x.toFloat() + compSizePx / 2f, a.y.toFloat() + compSizePx / 2f),
                                    end = Offset(b.x.toFloat() + compSizePx / 2f, b.y.toFloat() + compSizePx / 2f),
                                    strokeWidth = 6f
                                )
                            }
                        }

                        // preview line while connecting
                        if (state.connectingFrom != null && previewPointer != null) {
                            val fromComp = state.components.find { it.id == state.connectingFrom }
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
                    state.components.forEach { comp ->
                        val bColor = brightnessColor(comp)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(comp.x.roundToInt(), comp.y.roundToInt()) }
                                .size(compSizeDp)
                                .pointerInput(comp.id) {
                                    detectDragGestures { _, dragAmount ->
                                        vm.onIntent(CircuitExerciseIntent.DragComponent(comp.id, dragAmount.x, dragAmount.y))
                                    }
                                }
                                .pointerInput(comp.id) {
                                    detectTapGestures(onTap = {
                                        vm.onIntent(CircuitExerciseIntent.TapComponent(comp.id))
                                    })
                                }
                                .background(
                                    when {
                                        state.connectingFrom == comp.id -> MaterialTheme.colorScheme.secondaryContainer
                                        comp.type in listOf("bulb", "led", "motor", "buzzer") && bColor != Color.Unspecified -> bColor
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = comp.label, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                if (comp.type in listOf("bulb", "led", "motor", "buzzer")) {
                                    val b = vm.getBrightness(comp.id)
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
                        state.connections.forEach { conn ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                val fromLabel = state.components.find { it.id == conn.from }?.label ?: conn.from
                                val toLabel = state.components.find { it.id == conn.to }?.label ?: conn.to
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$fromLabel → $toLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row {
                                        TextButton(onClick = {
                                            vm.onIntent(CircuitExerciseIntent.RemoveConnection(conn.from, conn.to))
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
            val settingsComponents = state.components.filter {
                it.type == "battery" || it.type == "resistor" || it.type == "bulb" || it.type == "capacitor" || it.type == "switch"
            }
            if (settingsComponents.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Nastavení komponent", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        settingsComponents.forEach { comp ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(comp.label, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (comp.type == "switch") {
                                    val switchOn = state.switchStates[comp.id] ?: false
                                    Button(onClick = {
                                        vm.onIntent(CircuitExerciseIntent.ToggleSwitch(comp.id))
                                    }) { Text(if (switchOn) "Zapnuto" else "Vypnuto") }
                                } else {
                                    var textVal by remember(comp.id) { mutableStateOf(comp.value.toString()) }
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
                                            onValueChange = { newText ->
                                                textVal = newText
                                                val v = newText.toDoubleOrNull()
                                                if (v != null) {
                                                    vm.onIntent(CircuitExerciseIntent.UpdateComponentValue(comp.id, v))
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
                                    vm.onIntent(CircuitExerciseIntent.RemoveComponent(comp.id))
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Smazat")
                                }
                            }
                        }
                    }
                }
            }

            // Circuit calculations
            if (state.eval.circuitClosed && state.eval.hasPower) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Výpočty obvodu", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Napětí")
                                Text("${String.format("%.1f", state.eval.totalVoltage)} V", style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Celkový odpor")
                                Text("${String.format("%.1f", state.eval.totalResistance)} Ω", style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Proud")
                                Text("${String.format("%.2f", state.eval.circuitCurrent)} A", style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Výkon")
                                Text("${String.format("%.1f", state.eval.circuitPower)} W", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (state.eval.warning.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.eval.warning, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.onIntent(CircuitExerciseIntent.Clear) }) { Text("Vymazat") }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
