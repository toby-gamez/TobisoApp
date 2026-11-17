package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.components.Card
import com.tobiso.tobisoappnative.components.ContentRenderer
import com.tobiso.tobisoappnative.components.ExerciseLoadingContent
import com.tobiso.tobisoappnative.components.IconButton
import com.tobiso.tobisoappnative.components.parseContentToElements
import com.tobiso.tobisoappnative.navigation.ExerciseCircuitRoute
import com.tobiso.tobisoappnative.navigation.ExerciseSimulationRoute
import com.tobiso.tobisoappnative.utils.SimulationPhysics
import com.tobiso.tobisoappnative.utils.SimulationPhysics.DEG
import com.tobiso.tobisoappnative.utils.SimulationPhysics.End
import com.tobiso.tobisoappnative.utils.SimulationPhysics.OrbitKind
import com.tobiso.tobisoappnative.utils.SimulationPhysics.TAU
import com.tobiso.tobisoappnative.viewmodel.simulation.SimulationExerciseEffect
import com.tobiso.tobisoappnative.viewmodel.simulation.SimulationExerciseIntent
import com.tobiso.tobisoappnative.viewmodel.simulation.SimulationExerciseViewModel
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationExerciseScreen(
    exerciseId: Int,
    navController: NavController,
    ttsViewModel: TtsViewModel
) {
    val vm: SimulationExerciseViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(exerciseId) { vm.onIntent(SimulationExerciseIntent.Load(exerciseId)) }
    LaunchedEffect(Unit) {
        vm.effect.collectLatest { effect ->
            when (effect) {
                SimulationExerciseEffect.NavigateBack -> navController.popBackStack()
            }
        }
    }
    // The web renders the circuit simulator for kind "circuit"; the app already has that as its own screen.
    LaunchedEffect(state.kind) {
        if (state.kind == "circuit") {
            navController.navigate(ExerciseCircuitRoute(exerciseId)) {
                popUpTo<ExerciseSimulationRoute> { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.exerciseTitle.ifEmpty { "Cvičení: simulace" },
                        style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    if (!state.instructionsMarkdown.isNullOrEmpty()) {
                        IconButton(onClick = { ttsViewModel.speak(state.instructionsMarkdown ?: "") }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Číst nahlas")
                        }
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
            ExerciseLoadingContent(
                isLoading = state.isLoading,
                isOffline = state.isOffline,
                offlineText = "Offline režim: simulace je plně funkční i offline.",
                error = state.error
            )

            state.instructionsMarkdown?.let { instructions ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ContentRenderer(
                            contentElements = parseContentToElements(instructions, isOffline = state.isOffline, posts = emptyList()),
                            isOffline = state.isOffline,
                            posts = emptyList(),
                            addendums = emptyList(),
                            navController = navController,
                            onAddendumSelected = {}
                        )
                    }
                }
            }

            if (!state.isLoading && state.error == null) {
                // key on the exercise so switching exercises restarts the simulation from its config
                androidx.compose.runtime.key(exerciseId, state.kind) {
                    when (state.kind) {
                        "kepler" -> KeplerSimulation(state.config)
                        "projectile" -> ProjectileSimulation(state.config)
                        "gravity-field" -> GravityFieldSimulation(state.config)
                        "circuit", "" -> Unit
                        else -> Text("Neznámý typ simulace: ${state.kind}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────

private class SimColors(
    val text: Color, val accent: Color, val accentSoft: Color,
    val accent2: Color, val accent2Soft: Color, val muted: Color, val faint: Color, val bg: Color
)

@Composable
private fun rememberSimColors(): SimColors {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        SimColors(
            text = cs.onSurface, accent = cs.primary, accentSoft = cs.primary.copy(alpha = 0.6f),
            accent2 = cs.tertiary, accent2Soft = cs.tertiary.copy(alpha = 0.6f),
            muted = cs.outline, faint = cs.outlineVariant, bg = cs.surface
        )
    }
}

private val csLocale = Locale("cs", "CZ")
private fun fmt(v: Double, d: Int = 2): String = String.format(csLocale, "%.${d}f", v)

private fun JsonObject.num(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.flag(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

/** Frame loop: calls [onFrame] with dt in seconds (capped at 50 ms) and bumps the returned tick to redraw. */
@Composable
private fun rememberFrameTicker(onFrame: (Double) -> Unit): State<Long> {
    val tick = remember { mutableLongStateOf(0L) }
    val callback = rememberUpdatedState(onFrame)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else min(0.05, (now - last) / 1e9)
                last = now
                callback.value(dt)
                tick.longValue++
            }
        }
    }
    return tick
}

@Composable
private fun SimSlider(
    label: String, value: Double, min: Double, max: Double,
    unit: String = "", digits: Int = 2, onChange: (Double) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(
                fmt(value, digits) + if (unit.isNotEmpty()) " $unit" else "",
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value.coerceIn(min, max).toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = min.toFloat()..max.toFloat()
        )
    }
}

@Composable
private fun SimToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SimButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) { Text(label, maxLines = 1) }
}

@Composable
private fun SimNote(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
    )
}

/** Value cells; [tick] is read here so only this block recomposes every frame. */
@Composable
private fun SimReadouts(tick: State<Long>, values: () -> List<Pair<String, String>>) {
    tick.value
    val items = values()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEach { (name, v) ->
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun DrawScope.simLabel(
    tm: TextMeasurer, text: String, x: Float, y: Float, color: Color,
    align: Int = 0, sizeSp: Float = 12f, bold: Boolean = false
) {
    val layout = tm.measure(text, TextStyle(color = color, fontSize = sizeSp.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium))
    val tx = when (align) { -1 -> x; 1 -> x - layout.size.width; else -> x - layout.size.width / 2f }
    drawText(layout, topLeft = Offset(tx, y - layout.size.height / 2f))
}

private fun DrawScope.simDot(x: Float, y: Float, r: Float, fill: Color?, stroke: Color?) {
    if (fill != null) drawCircle(fill, r, Offset(x, y), style = Fill)
    if (stroke != null) drawCircle(stroke, r, Offset(x, y), style = Stroke(2.dp.toPx()))
}

private fun DrawScope.simArrow(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, widthDp: Float = 2.5f) {
    val len = hypot(x2 - x1, y2 - y1)
    if (len < 2f) return
    val ang = atan2(y2 - y1, x2 - x1)
    val head = min(9.dp.toPx(), len * 0.45f)
    val w = widthDp.dp.toPx()
    drawLine(color, Offset(x1, y1), Offset(x2 - cos(ang) * head * 0.6f, y2 - sin(ang) * head * 0.6f), w, StrokeCap.Round)
    val p = Path().apply {
        moveTo(x2, y2)
        lineTo(x2 - cos(ang - 0.45f) * head, y2 - sin(ang - 0.45f) * head)
        lineTo(x2 - cos(ang + 0.45f) * head, y2 - sin(ang + 0.45f) * head)
        close()
    }
    drawPath(p, color, style = Fill)
}

private fun dashed(onDp: Float, offDp: Float, density: Float) = PathEffect.dashPathEffect(floatArrayOf(onDp * density, offDp * density))

/**
 * Drag that starts only when [hit] accepts the first touch; any other touch is left alone so the
 * surrounding column can still scroll.
 */
private fun Modifier.grabDrag(
    hit: (Offset) -> Boolean,
    onStart: () -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!hit(down.position)) return@awaitEachGesture
        down.consume()
        onStart()
        drag(down.id) { change ->
            change.consume()
            onMove(change.position)
        }
        onEnd()
    }
}

// ── Kepler ─────────────────────────────────────────────────────────────────

private val PLANETS = listOf("Merkur" to 0.387, "Venuše" to 0.723, "Země" to 1.0, "Mars" to 1.524, "Jupiter" to 5.203)
private const val MU_SUN = 4 * PI * PI      // AU³ / rok²
private const val KMS_PER_AU_YR = 4.74047
private const val YEAR_SECONDS = 10.0       // 1 simulated year = 10 s at speed ×1

private class KeplerRuntime { var meanAnomaly = 0.0 }

@Composable
private fun KeplerSimulation(cfg: JsonObject) {
    var e by remember { mutableDoubleStateOf((cfg.num("eccentricity") ?: 0.6).coerceIn(0.0, 0.95)) }
    var a by remember { mutableDoubleStateOf((cfg.num("semiMajorAxis") ?: 1.0).coerceIn(0.3, 6.0)) }
    var n by remember { mutableDoubleStateOf((cfg.num("sectors") ?: 8.0).coerceIn(3.0, 16.0).roundToInt().toDouble()) }
    var speed by remember { mutableDoubleStateOf(1.0) }
    var playing by remember { mutableStateOf(true) }
    var axes by remember { mutableStateOf(cfg.flag("showAxes") ?: true) }
    var foci by remember { mutableStateOf(cfg.flag("showFoci") ?: false) }
    var sectorsOn by remember { mutableStateOf(cfg.flag("showSectors") ?: true) }
    var velocity by remember { mutableStateOf(cfg.flag("showVelocity") ?: true) }
    val rt = remember { KeplerRuntime() }
    val colors = rememberSimColors()
    val tm = rememberTextMeasurer()

    val stats = remember(a, e, n) { SimulationPhysics.keplerSectors(a, e, n.roundToInt()) }
    val tick = rememberFrameTicker { dt ->
        if (playing) rt.meanAnomaly = (rt.meanAnomaly + TAU * dt * speed / (YEAR_SECONDS * a.pow(1.5))) % TAU
    }

    fun sectorIndex() = min(n.roundToInt() - 1, floor(rt.meanAnomaly / (TAU / n.roundToInt())).toInt())

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(320.dp)) {
            tick.value
            val c = colors
            val w = size.width; val h = size.height
            val nn = n.roundToInt()
            val b = a * sqrt(1 - e * e)
            val pad = 40.dp.toPx()
            val s = min((w - 2 * pad) / (2 * a), (h - 2 * pad) / (2 * b)).toFloat()
            val cx = w / 2; val cy = h / 2
            val fx = cx + (a * e * s).toFloat()
            val f2x = cx - (a * e * s).toFloat()
            fun px(big: Double) = fx + (a * (cos(big) - e) * s).toFloat()
            fun py(big: Double) = cy - (b * sin(big) * s).toFloat()

            val big = SimulationPhysics.keplerE(rt.meanAnomaly, e)
            val pX = px(big); val pY = py(big)
            val sector = sectorIndex()

            if (sectorsOn) {
                stats.forEachIndexed { k, sc ->
                    val path = Path().apply {
                        moveTo(fx, cy)
                        for (i in 0..28) {
                            val en = sc.e0 + (sc.e1 - sc.e0) * i / 28
                            lineTo(px(en), py(en))
                        }
                        close()
                    }
                    drawPath(path, (if (k % 2 == 0) c.accentSoft else c.accent2Soft).copy(alpha = if (k == sector) 0.6f else 0.2f), style = Fill)
                    drawPath(path, c.faint.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
                }
            }

            val orbit = Path().apply {
                for (i in 0..200) {
                    val en = i / 200.0 * TAU
                    if (i == 0) moveTo(px(en), py(en)) else lineTo(px(en), py(en))
                }
            }
            drawPath(orbit, c.muted, style = Stroke(2.dp.toPx()))
            if (sectorsOn) stats.forEach { simDot(px(it.e0), py(it.e0), 3.5.dp.toPx(), c.muted, null) }

            if (axes) {
                val dash = dashed(6f, 5f, density)
                drawLine(c.accent, Offset(cx - (a * s).toFloat(), cy), Offset(cx + (a * s).toFloat(), cy), 1.5.dp.toPx(), pathEffect = dash)
                drawLine(c.accent2, Offset(cx, cy + (b * s).toFloat()), Offset(cx, cy - (b * s).toFloat()), 1.5.dp.toPx(), pathEffect = dash)
                simDot(cx, cy, 3.dp.toPx(), c.muted, null)
                simLabel(tm, "a = ${fmt(a)} AU", cx - (a * s).toFloat() / 2, cy + 16.dp.toPx(), c.accent)
                simLabel(tm, "b = ${fmt(b)} AU", cx + 6.dp.toPx(), cy - (b * s).toFloat() / 2, c.accent2, align = -1)
                simLabel(tm, "perihel", cx + (a * s).toFloat() - 4.dp.toPx(), cy - 16.dp.toPx(), c.muted, align = 1, sizeSp = 11f)
                simLabel(tm, "afel", cx - (a * s).toFloat() + 4.dp.toPx(), cy - 16.dp.toPx(), c.muted, align = -1, sizeSp = 11f)
            }

            if (foci) {
                simDot(f2x, cy, 5.dp.toPx(), null, c.muted)
                drawLine(c.muted, Offset(pX, pY), Offset(f2x, cy), 1.5.dp.toPx(), pathEffect = dashed(3f, 4f, density))
            }
            drawLine(c.accent, Offset(fx, cy), Offset(pX, pY), 1.8.dp.toPx())

            // 1 AU scale bar
            drawLine(c.muted, Offset(16.dp.toPx(), h - 18.dp.toPx()), Offset(16.dp.toPx() + s, h - 18.dp.toPx()), 2.dp.toPx())
            simLabel(tm, "1 AU", 16.dp.toPx() + s / 2, h - 30.dp.toPx(), c.muted, sizeSp = 11f)

            simDot(fx, cy, 15.dp.toPx(), c.accentSoft, c.accent)

            val r1 = hypot(pX - fx, pY - cy) / s.toDouble()
            if (velocity) {
                val v = sqrt(MU_SUN * (2 / r1 - 1 / a))
                val tx = -a * sin(big); val ty = -b * cos(big)
                val tl = hypot(tx, ty)
                val len = min(90.dp.toPx(), 22.dp.toPx() + 14.dp.toPx() * v.toFloat())
                simArrow(pX, pY, pX + (tx / tl).toFloat() * len, pY + (ty / tl).toFloat() * len, c.accent2)
            }
            simDot(pX, pY, 8.dp.toPx(), c.accent2Soft, c.accent2)
        }

        SimReadouts(tick) {
            val nn = n.roundToInt()
            val big = SimulationPhysics.keplerE(rt.meanAnomaly, e)
            val r1 = a * (1 - e * cos(big))
            val v = sqrt(MU_SUN * (2 / r1 - 1 / a))
            val period = a.pow(1.5)
            val sc = stats[sectorIndex()]
            listOf(
                "Vzdálenost od Slunce r" to "${fmt(r1, 3)} AU",
                "Rychlost v" to "${fmt(v * KMS_PER_AU_YR, 1)} km/s",
                "Oběžná doba T = a³ᐟ²" to "${fmt(period, 2)} roku",
                "T² / a³" to fmt(period * period / (a * a * a), 3),
                "Plocha jedné výseče" to "${fmt(sc.area, 3)} AU²",
                "Dráha v aktuální výseči" to "${fmt(sc.arc, 3)} AU",
                "Doba jedné výseče" to "${fmt(period * 365.25 / nn, 0)} dní",
                "r₁ + r₂" to if (foci) "${fmt(2 * a, 3)} AU (= 2a)" else "zapni ohniska"
            )
        }

        SimSlider("Excentricita e", e, 0.0, 0.9) { e = it }
        SimSlider("Hlavní poloosa a", a, 0.3, 6.0, "AU") { a = it }
        SimSlider("Počet stejných časových dílů", n, 3.0, 16.0, digits = 0) { n = it.roundToInt().toDouble() }
        SimSlider("Rychlost animace", speed, 0.0, 4.0, "×") { speed = it }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimToggle("Poloosy", axes) { axes = it }
            SimToggle("Ohniska (r₁ + r₂)", foci) { foci = it }
            SimToggle("Výseče za stejný čas", sectorsOn) { sectorsOn = it }
            SimToggle("Vektor rychlosti", velocity) { velocity = it }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimButton(if (playing) "Pozastavit" else "Spustit") { playing = !playing }
            SimButton("Od začátku") { rt.meanAnomaly = 0.0 }
            PLANETS.forEach { (name, axis) -> SimButton(name) { a = axis } }
        }
        SimNote("Excentricita je záměrně přehnaná – skutečná dráha Země má e ≈ 0,017 a je téměř kružnice. Všechny výseče mají stejnou plochu, protože je planeta oběhne za stejný čas.")
    }
}

// ── Projectile in a gravitational field ────────────────────────────────────

private val BODIES = listOf("Země" to 9.81, "Měsíc" to 1.62, "Mars" to 3.71, "Jupiter" to 24.79)
private const val R_EARTH = 6371.0      // km
private const val MU_EARTH = 398600.0   // km³/s²

private class CentralRun(
    val sim: SimulationPhysics.Trajectory,
    val info: SimulationPhysics.OrbitInfo,
    val start: DoubleArray,
    val past: List<Pair<Double, Double>>
) {
    val duration get() = sim.pts.last().t
    val r0 get() = hypot(start[0], start[1])
}

private class FlatView(val s: Float, val padL: Float, val padB: Float, val h: Float)

private class ProjectileRuntime {
    var t = 0.0
    var flat: SimulationPhysics.FlatThrow? = null
    var central: CentralRun? = null
    var view: FlatView? = null
    var dragKind: String? = null
    var dragScale = 0f
    var touched = false
    var w = 0f
    var h = 0f
}

@Composable
private fun ProjectileSimulation(cfg: JsonObject) {
    var mode by remember { mutableStateOf(if (cfg["mode"]?.jsonPrimitive?.contentOrNull == "central") "central" else "flat") }
    var g by remember { mutableDoubleStateOf(cfg.num("gravity") ?: 9.81) }
    var v0 by remember { mutableDoubleStateOf(cfg.num("speed") ?: 20.0) }
    var angle by remember { mutableDoubleStateOf(cfg.num("angle") ?: 45.0) }
    var height by remember { mutableDoubleStateOf(cfg.num("height") ?: 0.0) }
    var vectors by remember { mutableStateOf(true) }
    var strobe by remember { mutableStateOf(true) }
    var cv by remember { mutableDoubleStateOf(cfg.num("centralSpeed") ?: 7.0) }
    var cangle by remember { mutableDoubleStateOf(cfg.num("centralAngle") ?: 0.0) }
    var calt by remember { mutableDoubleStateOf(cfg.num("altitude") ?: 300.0) }
    var compare by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(true) }
    val rt = remember { ProjectileRuntime() }
    val colors = rememberSimColors()
    val tm = rememberTextMeasurer()
    val density = LocalDensity.current.density

    fun computeCentral(start: DoubleArray, past: List<Pair<Double, Double>>) {
        val r = hypot(start[0], start[1])
        val v = hypot(start[2], start[3])
        val radial = (start[0] * start[2] + start[1] * start[3]) / (r * max(v, 1e-9))
        val info = SimulationPhysics.classifyOrbit(r, v, kotlin.math.asin(radial.coerceIn(-1.0, 1.0)), MU_EARTH, R_EARTH)
        val sim = SimulationPhysics.integrateCentral(start, MU_EARTH, 2.0, R_EARTH, 14 * R_EARTH, max(info.period ?: 20000.0, 600.0))
        rt.central = CentralRun(sim, info, start, past)
    }

    fun compute() {
        if (mode == "flat") rt.flat = SimulationPhysics.flatThrow(v0, angle, height, g)
        else {
            val a = cangle * DEG
            computeCentral(doubleArrayOf(0.0, R_EARTH + calt, cv * cos(a), cv * sin(a)), emptyList())
        }
    }

    fun restart() { rt.t = 0.0; compute(); playing = true }

    if (rt.flat == null && rt.central == null) compute()

    fun centralEnded(): Boolean { val c = rt.central!!; return c.sim.end != End.TIME && rt.t >= c.duration - 1e-9 }
    fun centralFlying() = rt.central != null && rt.t > 1e-6 && !centralEnded()

    fun centralNow(): SimulationPhysics.TrajPoint {
        val pts = rt.central!!.sim.pts
        var i = 0
        while (i < pts.size - 1 && pts[i + 1].t <= rt.t) i++
        val p = pts[i]; val q = pts[min(i + 1, pts.size - 1)]
        val k = if (q.t > p.t) ((rt.t - p.t) / (q.t - p.t)).coerceIn(0.0, 1.0) else 0.0
        fun l(u: Double, w: Double) = u + (w - u) * k
        return SimulationPhysics.TrajPoint(rt.t, l(p.x, q.x), l(p.y, q.y), l(p.vx, q.vx), l(p.vy, q.vy))
    }

    // In flight a change is an impulse at the current point; before launch or after landing it is a fresh shot.
    fun relaunchCentral(adjust: (SimulationPhysics.TrajPoint) -> DoubleArray) {
        if (!centralFlying()) { restart(); return }
        val n = centralNow()
        val c = rt.central!!
        val traveled = (c.past + c.sim.pts.filter { it.t <= rt.t }.map { it.x to it.y } + (n.x to n.y)).takeLast(4000)
        val (vx, vy) = adjust(n).let { it[0] to it[1] }
        computeCentral(doubleArrayOf(n.x, n.y, vx, vy), traveled)
        rt.t = 0.0
        playing = true
    }

    fun withSpeed(v: Double): (SimulationPhysics.TrajPoint) -> DoubleArray = { n ->
        val sp = hypot(n.vx, n.vy); val r = hypot(n.x, n.y)
        if (sp > 1e-9) doubleArrayOf(n.vx / sp * v, n.vy / sp * v) else doubleArrayOf(n.y / r * v, -n.x / r * v)
    }
    fun withAngle(a: Double): (SimulationPhysics.TrajPoint) -> DoubleArray = { n ->
        val sp = hypot(n.vx, n.vy); val r = hypot(n.x, n.y)
        doubleArrayOf(sp * (cos(a) * (n.y / r) + sin(a) * (n.x / r)), sp * (cos(a) * (-n.x / r) + sin(a) * (n.y / r)))
    }

    val tick = rememberFrameTicker { dt ->
        if (mode == "flat") {
            val f = rt.flat
            if (f != null && playing) {
                rt.t += dt * (if (f.tFlight > 4) f.tFlight / 4 else 1.0)
                if (rt.t >= f.tFlight) { rt.t = f.tFlight; playing = false }
            }
        } else {
            val c = rt.central
            if (c != null && playing) {
                val d = c.duration
                rt.t += dt * d / 9.0
                if (rt.t >= d) {
                    if (c.sim.end == End.TIME && c.info.period != null) rt.t %= d else { rt.t = d; playing = false }
                }
            }
        }
    }

    // arrow/handle length in px per 1 m/s
    fun vecPx(): Float = (min(3.4f, max(1.8f, min(rt.w, rt.h) / density * 0.0065f))) * density

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "flat", onClick = { mode = "flat"; rt.central = null; rt.flat = null; restart() }, label = { Text("Homogenní pole") })
            FilterChip(selected = mode == "central", onClick = { mode = "central"; rt.central = null; rt.flat = null; restart() }, label = { Text("Centrální pole (Země)") })
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .grabDrag(
                    hit = { p ->
                        val v = rt.view; val f = rt.flat
                        if (mode != "flat" || v == null || f == null) false
                        else {
                            val lx = v.padL; val ly = v.h - v.padB - (height * v.s).toFloat()
                            val tipX = lx + (f.vx * vecPx()).toFloat(); val tipY = ly - (f.vy * vecPx()).toFloat()
                            val kind = when {
                                hypot(p.x - tipX, p.y - tipY) < 26 * density -> "v"
                                hypot(p.x - lx, p.y - ly) < 22 * density -> "h"
                                else -> null
                            }
                            rt.dragKind = kind
                            kind != null
                        }
                    },
                    onStart = { rt.dragScale = rt.view!!.s; rt.touched = true; playing = false; rt.t = 0.0 },
                    onMove = { p ->
                        val v = rt.view!!
                        if (rt.dragKind == "v") {
                            val dx = p.x - v.padL; val dy = v.h - v.padB - (height * rt.dragScale).toFloat() - p.y
                            v0 = ((hypot(dx, dy) / vecPx() * 2).roundToInt() / 2.0).coerceIn(1.0, 80.0)
                            angle = Math.round(atan2(dy.toDouble(), dx.toDouble()) / DEG).toDouble().coerceIn(-45.0, 90.0)
                        } else {
                            height = Math.round((v.h - v.padB - p.y) / rt.dragScale).toDouble().coerceIn(0.0, 60.0)
                        }
                        rt.flat = SimulationPhysics.flatThrow(v0, angle, height, g)
                    },
                    onEnd = { rt.dragKind = null; restart() }
                )
        ) {
            tick.value
            rt.w = size.width; rt.h = size.height
            if (mode == "flat") drawFlat(rt, colors, tm, g, height, vectors, strobe, vecPx())
            else drawCentral(rt, colors, tm, compare, cv, cangle, density)
        }

        SimReadouts(tick) {
            if (mode == "flat") {
                val f = rt.flat ?: return@SimReadouts emptyList()
                listOf(
                    "Dolet" to "${fmt(f.range, 1)} m", "Maximální výška" to "${fmt(f.hMax, 1)} m",
                    "Doba letu" to "${fmt(f.tFlight, 2)} s", "Rychlost dopadu" to "${fmt(f.vImpact, 1)} m/s"
                )
            } else {
                val c = rt.central ?: return@SimReadouts emptyList()
                val cur = centralNowSafe(c, rt.t)
                val r = hypot(cur.x, cur.y)
                val names = mapOf(
                    OrbitKind.CIRCLE to "Kružnice", OrbitKind.ORBIT to "Elipsa – oběžná dráha",
                    OrbitKind.SUBORBITAL to "Elipsa – těleso dopadne", OrbitKind.PARABOLA to "Parabola – těleso unikne",
                    OrbitKind.HYPERBOLA to "Hyperbola – těleso unikne"
                )
                val done = rt.t >= c.duration
                listOf(
                    "Typ dráhy" to names.getValue(c.info.kind),
                    "Excentricita e" to fmt(c.info.e, 3),
                    "1. kosmická v₁" to "${fmt(sqrt(MU_EARTH / r), 2)} km/s",
                    "2. kosmická v₂" to "${fmt(sqrt(2 * MU_EARTH / r), 2)} km/s",
                    "Oběžná doba" to if (c.info.period != null && c.info.kind != OrbitKind.SUBORBITAL) "${fmt(c.info.period / 60, 0)} min" else "–",
                    "Stav" to when (c.sim.end) {
                        End.CRASH -> if (done) "Dopadlo" else "Letí"
                        End.ESCAPE -> if (done) "Uniklo" else "Letí"
                        End.TIME -> "Obíhá"
                    }
                )
            }
        }

        if (mode == "flat") {
            SimSlider("Počáteční rychlost v₀", v0, 1.0, 80.0, "m/s", 1) { v0 = it; restart() }
            SimSlider("Úhel výstřelu α", angle, -45.0, 90.0, "°", 0) { angle = it.roundToInt().toDouble(); restart() }
            SimSlider("Výška výstřelu h", height, 0.0, 60.0, "m", 0) { height = it.roundToInt().toDouble(); restart() }
            SimSlider("Gravitační zrychlení g", g, 1.0, 26.0, "m/s²") { g = it; restart() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimToggle("Rozklad rychlosti", vectors) { vectors = it }
                SimToggle("Poloha každou 0,5 s", strobe) { strobe = it }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimButton("Vystřelit znovu") { restart() }
                BODIES.forEach { (name, gv) -> SimButton(name) { g = gv; restart() } }
            }
            SimNote("Vodorovná složka rychlosti se nemění, svislá se mění rovnoměrně – proto jsou značky po 0,5 s vodorovně stejně daleko od sebe. Vodorovný hod zkus pomocí α = 0° a nenulové výšky. Trajektorii můžeš měnit i přímo v obrázku: tažením za kroužek na konci šipky nastavíš rychlost a úhel, tažením za bod výstřelu výšku.")
        } else {
            SimSlider("Výška výstřelu nad povrchem", calt, 0.0, 1000.0, "km", 0) { calt = (it / 10).roundToInt() * 10.0; restart() }
            SimSlider("Rychlost v (jde měnit i za letu)", cv, 0.0, 14.0, "km/s") { cv = it; relaunchCentral(withSpeed(it)) }
            SimSlider("Úhel nad vodorovnou rovinou", cangle, -30.0, 90.0, "°", 0) { cangle = it.roundToInt().toDouble(); relaunchCentral(withAngle(cangle * DEG)) }
            SimToggle("Porovnat s homogenním polem", compare) { compare = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimButton("Vystřelit znovu") { restart() }
                val cosmic = { mult: Double ->
                    val n = if (centralFlying()) centralNow() else null
                    val r = if (n != null) hypot(n.x, n.y) else R_EARTH + calt
                    val v = sqrt(mult * MU_EARTH / r)
                    cv = v; cangle = 0.0
                    relaunchCentral { m -> val rr = hypot(m.x, m.y); doubleArrayOf(m.y / rr * v, -m.x / rr * v) }
                }
                SimButton("1. kosmická") { cosmic(1.0) }
                SimButton("2. kosmická") { cosmic(2.0) }
            }
            SimNote("Při malých rychlostech je dráha téměř parabola (homogenní pole), jak ale rychlost roste, je vidět, že skutečná dráha je část elipsy se Zemí v ohnisku. Nad v₁ těleso Zemi neprobíjí, nad v₂ unikne. Rychlost a úhel můžeš měnit i za letu – těleso dostane impuls v místě, kde zrovna je, a dráha se přepočítá.")
        }
    }
}

private fun centralNowSafe(c: CentralRun, t: Double): SimulationPhysics.TrajPoint {
    val pts = c.sim.pts
    var i = 0
    while (i < pts.size - 1 && pts[i + 1].t <= t) i++
    val p = pts[i]; val q = pts[min(i + 1, pts.size - 1)]
    val k = if (q.t > p.t) ((t - p.t) / (q.t - p.t)).coerceIn(0.0, 1.0) else 0.0
    return SimulationPhysics.TrajPoint(t, p.x + (q.x - p.x) * k, p.y + (q.y - p.y) * k, 0.0, 0.0)
}

private fun DrawScope.drawFlat(
    rt: ProjectileRuntime, c: SimColors, tm: TextMeasurer,
    g: Double, h0: Double, vectors: Boolean, strobe: Boolean, vecPx: Float
) {
    val f = rt.flat ?: return
    val w = size.width; val h = size.height
    val padL = 44.dp.toPx(); val padR = 24.dp.toPx(); val padT = 30.dp.toPx(); val padB = 44.dp.toPx()
    val xSpan = max(f.range, 10.0) * 1.08; val ySpan = max(f.hMax, 5.0) * 1.2
    // frozen while dragging so the handle under the finger doesn't slide
    val s = if (rt.dragKind != null) rt.dragScale else min((w - padL - padR) / xSpan, (h - padT - padB) / ySpan).toFloat()
    rt.view = FlatView(s, padL, padB, h)
    fun X(x: Double) = padL + (x * s).toFloat()
    fun Y(y: Double) = h - padB - (y * s).toFloat()
    fun pos(t: Double) = (f.vx * t) to (h0 + f.vy * t - 0.5 * g * t * t)

    drawRect(c.accent2Soft.copy(alpha = 0.35f), Offset(0f, Y(0.0)), androidx.compose.ui.geometry.Size(w, h - Y(0.0)))
    drawLine(c.accent2, Offset(0f, Y(0.0)), Offset(w, Y(0.0)), 2.dp.toPx())
    val step = SimulationPhysics.niceStep(xSpan)
    var x = 0.0
    while (x <= xSpan) {
        drawLine(c.muted, Offset(X(x), Y(0.0)), Offset(X(x), Y(0.0) + 6.dp.toPx()), 1.5.dp.toPx())
        simLabel(tm, "${fmt(x, if (x < 10 && step < 1) 1 else 0)} m", X(x), Y(0.0) + 18.dp.toPx(), c.muted, sizeSp = 11f)
        x += step
    }
    if (h0 > 0) drawLine(c.muted, Offset(X(0.0) - 2.dp.toPx(), Y(0.0)), Offset(X(0.0) - 2.dp.toPx(), Y(h0)), 4.dp.toPx(), StrokeCap.Round)

    val predicted = Path().apply {
        for (i in 0..80) { val (px, py) = pos(i / 80.0 * f.tFlight); if (i == 0) moveTo(X(px), Y(py)) else lineTo(X(px), Y(py)) }
    }
    drawPath(predicted, c.faint, style = Stroke(2.dp.toPx(), pathEffect = dashed(6f, 6f, density)))
    val done = Path().apply {
        for (i in 0..80) { val (px, py) = pos(i / 80.0 * rt.t); if (i == 0) moveTo(X(px), Y(py)) else lineTo(X(px), Y(py)) }
    }
    drawPath(done, c.accent, style = Stroke(3.dp.toPx()))

    if (strobe) {
        var t = 0.0
        while (t <= rt.t + 1e-9) { val (px, py) = pos(t); simDot(X(px), Y(py), 3.5.dp.toPx(), c.muted, null); t += 0.5 }
    }

    val (bx, by) = pos(rt.t)
    val vx = f.vx; val vy = f.vy - g * rt.t
    if (vectors) {
        simArrow(X(bx), Y(by), X(bx) + (vx * vecPx).toFloat(), Y(by), c.accent2)
        simArrow(X(bx), Y(by), X(bx), Y(by) - (vy * vecPx).toFloat(), c.accent)
        simArrow(X(bx), Y(by), X(bx) + (vx * vecPx).toFloat(), Y(by) - (vy * vecPx).toFloat(), c.text, 2f)
    }
    simDot(X(bx), Y(by), 8.dp.toPx(), c.accentSoft, c.accent)

    // drag handles: tip of the initial velocity (v₀, α) and the launch point (h)
    val lx = X(0.0); val ly = Y(h0)
    val tipX = lx + (f.vx * vecPx).toFloat(); val tipY = ly - (f.vy * vecPx).toFloat()
    drawLine(c.accent, Offset(lx, ly), Offset(tipX, tipY), 1.5.dp.toPx(), pathEffect = dashed(4f, 4f, density))
    drawCircle(c.bg.copy(alpha = 0.85f), 10.dp.toPx(), Offset(tipX, tipY))
    drawCircle(c.accent, 10.dp.toPx(), Offset(tipX, tipY), style = Stroke(2.5.dp.toPx()))
    drawCircle(c.muted, 7.dp.toPx(), Offset(lx, ly), style = Stroke(2.dp.toPx()))
    if (!rt.touched) simLabel(tm, "táhni za kroužek", tipX + 16.dp.toPx(), tipY - 14.dp.toPx(), c.muted, align = -1, sizeSp = 11f)
}

private fun DrawScope.drawCentral(
    rt: ProjectileRuntime, c: SimColors, tm: TextMeasurer, compare: Boolean, cv: Double, cangle: Double, density: Float
) {
    val run = rt.central ?: return
    val w = size.width; val h = size.height
    val sim = run.sim
    val d = run.duration

    var minX = run.start[0]; var maxX = minX; var minY = run.start[1]; var maxY = minY
    for ((px, py) in run.past) { minX = min(minX, px); maxX = max(maxX, px); minY = min(minY, py); maxY = max(maxY, py) }
    minX = min(minX, 0.0); maxX = max(maxX, 0.0)
    for (p in sim.pts) { minX = min(minX, p.x); maxX = max(maxX, p.x); minY = min(minY, p.y); maxY = max(maxY, p.y) }
    if (sim.end == End.ESCAPE) {
        val k = 0.35
        minX *= k; maxX *= k; minY = if (minY > 0) minY * (1 - k) else minY * k; maxY *= k
    }
    val minSpan = 0.5 * R_EARTH
    val spanX = max(maxX - minX, minSpan); val spanY = max(maxY - minY, minSpan)
    val pad = 46.dp.toPx()
    val s = min((w - 2 * pad) / spanX, (h - 2 * pad) / spanY).toFloat()
    val cx = (minX + maxX) / 2; val cy = (minY + maxY) / 2
    fun X(x: Double) = w / 2 + ((x - cx) * s).toFloat()
    fun Y(y: Double) = h / 2 - ((y - cy) * s).toFloat()

    drawCircle(c.accent2Soft.copy(alpha = 0.35f), (R_EARTH * s).toFloat(), Offset(X(0.0), Y(0.0)))
    drawCircle(c.accent2, (R_EARTH * s).toFloat(), Offset(X(0.0), Y(0.0)), style = Stroke(2.5.dp.toPx()))
    simDot(X(0.0), Y(0.0), 3.dp.toPx(), c.accent2, null)

    if (compare && run.past.isEmpty()) {
        val g0 = MU_EARTH / (run.r0 * run.r0)
        val a = cangle * DEG
        val vx = cv * cos(a); val vy = cv * sin(a)
        val path = Path()
        var started = false
        var t = 0.0
        while (t <= d) {
            val x = vx * t; val y = run.r0 + vy * t - 0.5 * g0 * t * t
            if (hypot(x, y) < R_EARTH) break
            if (started) path.lineTo(X(x), Y(y)) else path.moveTo(X(x), Y(y))
            started = true
            t += d / 200
        }
        drawPath(path, c.faint, style = Stroke(2.dp.toPx(), pathEffect = dashed(6f, 6f, density)))
    }

    if (run.past.size > 1) {
        val path = Path()
        run.past.forEachIndexed { i, (px, py) -> if (i == 0) path.moveTo(X(px), Y(py)) else path.lineTo(X(px), Y(py)) }
        drawPath(path, c.accent, style = Stroke(3.dp.toPx()))
    }

    val full = Path()
    sim.pts.forEachIndexed { i, p -> if (i == 0) full.moveTo(X(p.x), Y(p.y)) else full.lineTo(X(p.x), Y(p.y)) }
    drawPath(full, c.faint, style = Stroke(2.dp.toPx()))

    val trav = Path()
    var curX = sim.pts[0].x; var curY = sim.pts[0].y; var curT = sim.pts[0].t
    trav.moveTo(X(curX), Y(curY))
    for (i in 1 until sim.pts.size) {
        val p = sim.pts[i]
        if (p.t <= rt.t) { trav.lineTo(X(p.x), Y(p.y)); curX = p.x; curY = p.y; curT = p.t; continue }
        val k = (rt.t - curT) / max(p.t - curT, 1e-9)
        curX += (p.x - curX) * k; curY += (p.y - curY) * k
        trav.lineTo(X(curX), Y(curY))
        break
    }
    drawPath(trav, c.accent, style = Stroke(3.dp.toPx()))
    simDot(X(curX), Y(curY), 8.dp.toPx(), c.accentSoft, c.accent)
    simDot(X(run.start[0]), Y(run.start[1]), 3.5.dp.toPx(), c.muted, null)

    val bar = SimulationPhysics.niceStep(w / s / 5.0, 1.0)
    drawLine(c.muted, Offset(16.dp.toPx(), h - 18.dp.toPx()), Offset(16.dp.toPx() + (bar * s).toFloat(), h - 18.dp.toPx()), 2.dp.toPx())
    simLabel(tm, "${String.format(csLocale, "%,.0f", bar)} km", 16.dp.toPx() + (bar * s).toFloat() / 2, h - 30.dp.toPx(), c.muted, sizeSp = 11f)
}

// ── Gravity field ──────────────────────────────────────────────────────────

private const val R_BODY = 0.18
private const val VIEW = 3.2   // half of the displayed range (world units)

private class GravityRuntime {
    var x = 1.5
    var y = 0.0
    var vel: DoubleArray? = null
    val trail = ArrayList<Offset>()
    var status = ""
    var home: Offset? = null
    var w = 0f
    var h = 0f
    var dragging = false
}

@Composable
private fun GravityFieldSimulation(cfg: JsonObject) {
    var n by remember { mutableDoubleStateOf(cfg.num("exponent") ?: 2.0) }
    var mu by remember { mutableDoubleStateOf(cfg.num("mass") ?: 1.0) }
    var m by remember { mutableDoubleStateOf(1.0) }
    var vFactor by remember { mutableDoubleStateOf(cfg.num("speedFactor") ?: 1.0) }
    var arrows by remember { mutableStateOf(true) }
    var rings by remember { mutableStateOf(true) }
    var rDisplay by remember { mutableDoubleStateOf(1.5) }
    val rt = remember { GravityRuntime() }
    val colors = rememberSimColors()
    val tm = rememberTextMeasurer()
    val density = LocalDensity.current.density

    fun field(r: Double) = mu / r.pow(n)
    fun vCirc(r: Double) = sqrt(mu * r.pow(1 - n))
    fun vEsc(r: Double) = if (n > 1) sqrt(2 * mu / ((n - 1) * r.pow(n - 1))) else Double.POSITIVE_INFINITY
    fun scale() = min(rt.w, rt.h) / (2 * VIEW.toFloat())
    fun bodyRadius() = max(R_BODY * (0.6 + 0.4 * mu), 10 / (scale().toDouble().takeIf { it > 0 } ?: 1.0))
    fun radius() = hypot(rt.x, rt.y)
    fun clampPos() {
        val r = radius()
        val k = min(VIEW * 0.95, max(bodyRadius() + 0.1, r)) / max(r, 1e-9)
        rt.x *= k; rt.y *= k
        rDisplay = radius()
    }
    fun halt() { rt.vel = null; rt.trail.clear(); rt.status = "" }
    fun running() = rt.vel != null && rt.status.isEmpty()
    fun applySpeed() {
        val vel = rt.vel
        if (vel == null || rt.status.isNotEmpty()) return
        val r = radius(); val v = hypot(vel[0], vel[1]); val target = vFactor * vCirc(r)
        rt.vel = if (v > 1e-9) doubleArrayOf(vel[0] / v * target, vel[1] / v * target)
        else doubleArrayOf(-rt.y / r * target, rt.x / r * target)
    }

    val tick = rememberFrameTicker { dt ->
        val vel = rt.vel
        if (vel != null && rt.status.isEmpty()) {
            // 2× faster than the unit-radius orbit time keeps the animation lively
            var rem = dt * 2
            val f = SimulationPhysics.centralDerivs(mu, n)
            var s = doubleArrayOf(rt.x, rt.y, vel[0], vel[1])
            while (rem > 1e-9) {
                val r = hypot(s[0], s[1]); val v = hypot(s[2], s[3])
                val step = min(rem, min(0.004 * r / max(v, 1e-6), 0.02))
                s = SimulationPhysics.rk4(s, step, f)
                rem -= step
                val r2 = hypot(s[0], s[1])
                if (r2 < bodyRadius()) { rt.status = "Dopad na těleso"; break }
                if (r2 > VIEW * 1.6) { rt.status = "Uniklo z dosahu"; break }
            }
            rt.x = s[0]; rt.y = s[1]; rt.vel = doubleArrayOf(s[2], s[3])
            val last = rt.trail.lastOrNull()
            if (last == null || hypot(last.x - s[0], last.y - s[1]) > 0.02) rt.trail.add(Offset(s[0].toFloat(), s[1].toFloat()))
            if (rt.trail.size > 1600) rt.trail.removeAt(0)
            rDisplay = radius()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .grabDrag(
                    hit = { p ->
                        val s = scale()
                        val bx = rt.w / 2 + (rt.x * s).toFloat(); val by = rt.h / 2 - (rt.y * s).toFloat()
                        hypot(p.x - bx, p.y - by) < 26 * density
                    },
                    onStart = { rt.dragging = true; halt() },
                    onMove = { p ->
                        val s = scale()
                        rt.x = ((p.x - rt.w / 2) / s).toDouble(); rt.y = (-(p.y - rt.h / 2) / s).toDouble()
                        clampPos()
                    },
                    onEnd = { rt.dragging = false }
                )
        ) {
            tick.value
            rt.w = size.width; rt.h = size.height
            val c = colors
            val s = scale()
            fun X(x: Double) = size.width / 2 + (x * s).toFloat()
            fun Y(y: Double) = size.height / 2 - (y * s).toFloat()

            if (rings) {
                for (r in listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0)) {
                    drawCircle(c.faint, (r * s).toFloat(), Offset(X(0.0), Y(0.0)), style = Stroke(1.2.dp.toPx(), pathEffect = dashed(4f, 6f, density)))
                    simLabel(tm, fmt(r, 1), X(r * sqrt(0.5)) + 4.dp.toPx(), Y(r * sqrt(0.5)) - 4.dp.toPx(), c.muted, align = -1, sizeSp = 10f)
                }
            }

            if (arrows) {
                val f1 = field(1.0)
                var gx = -3.0
                while (gx <= 3.0) {
                    var gy = -3.0
                    while (gy <= 3.0) {
                        val r = hypot(gx, gy)
                        if (r >= 0.45 && r <= VIEW * 0.98) {
                            val mag = field(r) / f1
                            val len = min(0.42 * s, 0.17 * s * mag.pow(0.5) + 4.dp.toPx()).toFloat()
                            val ux = (-gx / r).toFloat(); val uy = (-gy / r).toFloat()
                            val a = min(1.0, 0.25 + 0.4 * mag.pow(0.5)).toFloat()
                            simArrow(
                                X(gx) - ux * len * 0.5f, Y(gy) + uy * len * 0.5f,
                                X(gx) + ux * len * 0.5f, Y(gy) - uy * len * 0.5f, c.accent.copy(alpha = a), 1.8f
                            )
                        }
                        gy += 0.5
                    }
                    gx += 0.5
                }
            }

            if (rt.trail.size > 1) {
                val path = Path()
                rt.trail.forEachIndexed { i, p -> if (i == 0) path.moveTo(X(p.x.toDouble()), Y(p.y.toDouble())) else path.lineTo(X(p.x.toDouble()), Y(p.y.toDouble())) }
                drawPath(path, c.accent2, style = Stroke(2.5.dp.toPx()))
            }

            simDot(X(0.0), Y(0.0), (bodyRadius() * s).toFloat(), c.accentSoft, c.accent)

            val r = radius()
            val px = X(rt.x); val py = Y(rt.y)
            val fMag = field(r) / field(1.0)
            val fl = min(70.dp.toPx(), 16.dp.toPx() + 26.dp.toPx() * sqrt(fMag * m).toFloat())
            simArrow(px, py, px - (rt.x / r).toFloat() * fl, py + (rt.y / r).toFloat() * fl, c.accent, 3f)
            val vel = rt.vel
            if (vel != null) {
                val v = hypot(vel[0], vel[1])
                val vl = min(70.dp.toPx(), 14.dp.toPx() + 34.dp.toPx() * v.toFloat())
                simArrow(px, py, px + (vel[0] / max(v, 1e-9)).toFloat() * vl, py - (vel[1] / max(v, 1e-9)).toFloat() * vl, c.accent2, 3f)
            }
            simDot(px, py, 9.dp.toPx(), c.accent2Soft, c.accent2)
            if (vel == null) simLabel(tm, "táhni mě", px, py - 20.dp.toPx(), c.muted, sizeSp = 11f)
        }

        SimReadouts(tick) {
            val r = radius()
            val ve = vEsc(r)
            listOf(
                "Vzdálenost r" to fmt(r, 2),
                "Intenzita pole g = F/m" to fmt(field(r), 3),
                "Síla F = m·g" to fmt(field(r) * m, 3),
                "Kruhová rychlost" to fmt(vCirc(r), 3),
                "Úniková rychlost" to if (ve.isFinite()) fmt(ve, 3) else "neexistuje",
                "Stav" to rt.status.ifEmpty { if (rt.vel != null) "Letí" else "Čeká na puštění" }
            )
        }

        SimSlider("Exponent síly n (F ∝ 1/rⁿ)", n, 1.5, 3.0) { n = it; if (!running()) halt() }
        SimSlider("Hmotnost centrálního tělesa M", mu, 0.3, 3.0, digits = 1) { mu = it; if (!running()) halt(); clampPos() }
        SimSlider("Hmotnost zkušebního tělesa m", m, 1.0, 10.0, digits = 1) { m = it }
        SimSlider("Vzdálenost r", rDisplay, 0.45, 3.0) {
            val r = radius().takeIf { it > 0 } ?: 1.0
            rt.x *= it / r; rt.y *= it / r
            if (!running()) halt()
            clampPos()
        }
        SimSlider("Rychlost v / v_kruhová", vFactor, 0.0, 1.6) { vFactor = it; applySpeed() }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimToggle("Šipky pole", arrows) { arrows = it }
            SimToggle("Kružnice stejné velikosti pole", rings) { rings = it }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimButton("Pustit") {
                val r = radius()
                val v = vFactor * vCirc(r)
                rt.home = Offset(rt.x.toFloat(), rt.y.toFloat())
                // perpendicular to the radius, counter-clockwise
                rt.vel = doubleArrayOf(-rt.y / r * v, rt.x / r * v)
                rt.trail.clear(); rt.trail.add(Offset(rt.x.toFloat(), rt.y.toFloat()))
                rt.status = ""
            }
            SimButton("Zastavit a vrátit") {
                halt()
                rt.home?.let { rt.x = it.x.toDouble(); rt.y = it.y.toDouble(); rDisplay = radius() }
            }
            SimButton("v = v_kruhová") { vFactor = 1.0; applySpeed() }
            SimButton("v = v_úniková") {
                val r = radius()
                val k = vEsc(r) / vCirc(r)
                if (k.isFinite()) { vFactor = min(1.6, k); applySpeed() }
            }
        }
        SimNote("Táhni zkušebním tělesem prstem. Pro n = 2 (Newton) je dráha uzavřená elipsa; při jiném exponentu se dráha stáčí do růžice a nikdy se neuzavře. Hmotnost m mění sílu, ale ne dráhu.")
    }
}
