package com.tobiso.tobisoappnative.components.conceptmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.model.ConceptEdge
import com.tobiso.tobisoappnative.model.ConceptNode
import kotlin.math.abs

private const val MAX_LABEL = 24
private const val NODE_HEIGHT = 34f

private fun displayLabel(label: String) = if (label.length > MAX_LABEL) label.take(MAX_LABEL - 1) + "…" else label
private fun nodeWidth(label: String) = (displayLabel(label).length * 7.2f + 34f).coerceIn(64f, 180f)

/**
 * Pan/zoom mind map (after the web's mind-map.js): drag the background to pan, pinch to zoom, drag a node to move it,
 * tap a node to highlight its branch (tap again or tap the background to clear). Nothing is persisted.
 */
@Composable
fun ConceptMapView(nodes: List<ConceptNode>, edges: List<ConceptEdge>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val ids = remember(nodes) { nodes.map { it.id } }
    val validEdges = remember(nodes, edges) { edges.filter { it.source in ids && it.target in ids && it.source != it.target } }
    val positions = remember(nodes, edges) {
        mutableStateMapOf<String, Offset>().apply {
            putAll(ConceptMapLayout.compute(ids, validEdges.map { it.source to it.target }))
        }
    }
    val labels = remember(nodes) { nodes.associate { it.id to it.label } }
    var scale by remember(nodes) { mutableFloatStateOf(1f) }
    var offset by remember(nodes) { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var focused by remember(nodes) { mutableStateOf<String?>(null) }
    var fitRequest by remember(nodes) { mutableIntStateOf(0) }

    LaunchedEffect(viewSize, fitRequest, nodes) {
        if (viewSize.width == 0 || viewSize.height == 0) return@LaunchedEffect
        val worldPositions = positions.values.map { it * density }
        val (s, o) = ConceptMapLayout.fit(worldPositions, 90f * density, NODE_HEIGHT / 2 * density, viewSize.width.toFloat(), viewSize.height.toFloat())
        scale = s
        offset = o
    }

    val related = remember(focused, validEdges) {
        focused?.let { f -> setOf(f) + validEdges.filter { it.source == f || it.target == f }.flatMap { listOf(it.source, it.target) } }
    }

    val measurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val textStyle = TextStyle(fontSize = 12.sp, color = scheme.onSurface)
    val edgeTextStyle = TextStyle(fontSize = 10.sp, color = scheme.onSurfaceVariant)

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Tažením posouvej, štípnutím přibližuj, klepnutím na pojem zvýrazníš jeho větev.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { fitRequest++ }) { Text("Vycentrovat") }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainerLow)
                .pointerInput(nodes, edges) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun worldOf(p: Offset) = (p - offset) / (scale * density)
                        val hit = positions.entries.lastOrNull { (id, pos) ->
                            val w = nodeWidth(labels[id].orEmpty())
                            val w2 = worldOf(down.position)
                            abs(w2.x - pos.x) <= w / 2 && abs(w2.y - pos.y) <= NODE_HEIGHT / 2
                        }?.key
                        var moved = 0f
                        if (hit != null) {
                            drag(down.id) { change ->
                                val delta = (change.position - change.previousPosition) / (scale * density)
                                positions[hit] = (positions[hit] ?: Offset.Zero) + delta
                                moved += (change.position - change.previousPosition).getDistance()
                                change.consume()
                            }
                            if (moved < viewConfiguration.touchSlop) focused = if (focused == hit) null else hit
                        } else {
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.positionChanged() }) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    if (zoom != 1f && centroid != Offset.Unspecified) {
                                        val newScale = (scale * zoom).coerceIn(0.2f, 4f)
                                        offset = centroid - (centroid - offset) * (newScale / scale) + pan
                                        scale = newScale
                                    } else {
                                        offset += pan
                                    }
                                    moved += pan.getDistance()
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                            if (moved < viewConfiguration.touchSlop) focused = null
                        }
                    }
                }
        ) {
            if (size.width.toInt() != viewSize.width || size.height.toInt() != viewSize.height) {
                viewSize = IntSize(size.width.toInt(), size.height.toInt())
            }
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                fun alphaOf(id: String) = if (related == null || id in related) 1f else 0.25f
                validEdges.forEach { e ->
                    val a = positions[e.source] ?: return@forEach
                    val b = positions[e.target] ?: return@forEach
                    val alpha = if (related == null || (e.source in related && e.target in related)) 1f else 0.15f
                    drawLine(scheme.outline.copy(alpha = alpha), a * density, b * density, strokeWidth = 1.5f * density)
                    if (e.label.isNotBlank() && alpha == 1f) {
                        val layout = measurer.measure(AnnotatedString(displayLabel(e.label)), edgeTextStyle)
                        val mid = (a + b) * density / 2f
                        drawText(layout, topLeft = mid - Offset(layout.size.width / 2f, layout.size.height / 2f))
                    }
                }
                nodes.forEach { n ->
                    val p = positions[n.id] ?: return@forEach
                    val alpha = alphaOf(n.id)
                    val w = nodeWidth(n.label) * density
                    val h = NODE_HEIGHT * density
                    val topLeft = Offset(p.x * density - w / 2, p.y * density - h / 2)
                    val isFocus = n.id == focused
                    drawRoundRect(
                        (if (isFocus) scheme.primary else scheme.primaryContainer).copy(alpha = alpha),
                        topLeft, Size(w, h), CornerRadius(h / 2)
                    )
                    drawRoundRect(scheme.primary.copy(alpha = alpha), topLeft, Size(w, h), CornerRadius(h / 2), style = Stroke(1.5f * density))
                    val layout = measurer.measure(
                        AnnotatedString(displayLabel(n.label)),
                        textStyle.copy(color = (if (isFocus) scheme.onPrimary else scheme.onPrimaryContainer).copy(alpha = alpha))
                    )
                    drawText(layout, topLeft = Offset(p.x * density - layout.size.width / 2f, p.y * density - layout.size.height / 2f))
                }
            }
        }
    }
}
