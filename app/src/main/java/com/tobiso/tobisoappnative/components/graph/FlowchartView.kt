package com.tobiso.tobisoappnative.components.graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.withTransform
import com.tobiso.tobisoappnative.components.cozyEntrance
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.ui.theme.Figtree
import kotlin.math.min

/**
 * A `[vyvojN]` block, drawn like the web's `<figure class="post-flowchart">`: same SVG geometry (viewBox units),
 * stroke widths and colour tokens as `.fc-*` in organic.css. Like the SVG it shrinks to fit the width and never upscales.
 */
@Composable
fun FlowchartView(source: String, modifier: Modifier = Modifier) {
    val chart = remember(source) { FlowchartParser.parse(source) }
    if (chart.nodes.isEmpty()) {
        val reason = chart.warnings.joinToString(" ").ifEmpty { "Chybí uzly (start(id: s): Začátek)." }
        DiagramError("Vývojový diagram se nepodařilo vykreslit – $reason", modifier)
        return
    }
    val layout = remember(chart) { FlowchartLayouter.layout(chart) }
    val measurer = rememberTextMeasurer()
    val pal = rememberWebPalette()

    // Cozy reveal, once per post: nodes pop in layer by layer and each edge draws itself after its source node.
    val total = chart.nodes.maxOf { it.layer } * LAYER_STEP + EDGE_DELAY + EDGE_DRAW + 0.1f
    var played by rememberSaveable(source) { mutableStateOf(false) }
    val reveal = remember(source) { Animatable(if (played) total else 0f) }
    LaunchedEffect(source) {
        if (!played) {
            delay(200)
            reveal.animateTo(total, tween((total * 1000).toInt(), easing = LinearEasing))
            played = true
        }
    }

    DiagramFrame(chart.title, chart.warnings, chart.notes, pal, modifier.cozyEntrance(key = source)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val naturalW = layout.width.toFloat()
            val width = min(maxWidth.value, naturalW)
            val height = width * (layout.height / layout.width).toFloat()
            Canvas(Modifier.size(width.dp, height.dp)) {
                val k = size.width / naturalW
                fun px(x: Double) = ((x - layout.minX) * k).toFloat()
                fun py(y: Double) = ((y - layout.minY) * k).toFloat()
                val strokeW = 2f * k
                val t = reveal.value
                fun prog(start: Float, dur: Float) = ((t - start) / dur).coerceIn(0f, 1f)
                fun Color.a(x: Float) = copy(alpha = alpha * x)
                fun font(size: Double, color: Color, weight: FontWeight? = null) =
                    TextStyle(color = color, fontSize = (size * k).toFloat().toSp(), fontFamily = Figtree, fontWeight = weight)

                // Text positioned by SVG baseline (y), horizontally centred on x.
                fun centred(str: String, style: TextStyle, cx: Float, baseline: Float) {
                    val r = measurer.measure(str, style)
                    drawText(r, topLeft = Offset(cx - r.size.width / 2f, baseline - r.firstBaseline))
                }

                // Edges first, so nodes and label chips sit on top (same order as the SVG).
                chart.edges.forEachIndexed { i, e ->
                    val pts = layout.routes[i]
                    if (pts.size < 2) return@forEachIndexed
                    val color = parseDiagramColor(e.color, pal.neutral700)
                    val drawn = prog(chart.nodes[e.from].layer * LAYER_STEP + EDGE_DELAY, EDGE_DRAW)
                    if (drawn <= 0f) return@forEachIndexed
                    val full = roundedPolyline(pts, ::px, ::py, 8f * k)
                    if (drawn < 1f) {
                        val measure = PathMeasure().apply { setPath(full, false) }
                        val part = Path()
                        measure.getSegment(0f, measure.length * FastOutSlowInEasing.transform(drawn), part, true)
                        drawPath(part, color, style = Stroke(strokeW))
                    } else {
                        drawPath(full, color, style = Stroke(strokeW))
                    }
                    if (drawn > 0.85f) arrowHead(pts[pts.size - 2], pts.last(), color.a((drawn - 0.85f) / 0.15f), k, ::px, ::py)
                }

                chart.nodes.forEach { node ->
                    val appear = prog(node.layer * LAYER_STEP, 0.45f)
                    if (appear <= 0f) return@forEach
                    val alpha = FastOutSlowInEasing.transform(appear)
                    val pop = 0.86f + 0.14f * easeOutBack(appear)
                    withTransform({ scale(pop, pop, pivot = Offset(px(node.x), py(node.y))) }) {
                    val x = px(node.x - node.w / 2); val y = py(node.y - node.h / 2)
                    val w = (node.w * k).toFloat(); val h = (node.h * k).toFloat()
                    val custom = node.color?.let { parseDiagramColor(it, pal.accent600) }
                    val (baseFill, baseStroke) = when (node.kind) {
                        FcKind.Start, FcKind.End -> pal.accent2100 to pal.accent2600
                        FcKind.Decision -> pal.surface to pal.neutral700
                        FcKind.Io -> pal.bg to pal.neutral700
                        FcKind.Process -> pal.accent100 to pal.accent600
                    }
                    val fill = (if (custom != null) custom.copy(alpha = 0.2f) else baseFill).a(alpha)
                    val stroke = (custom ?: baseStroke).a(alpha)
                    when (node.kind) {
                        FcKind.Decision, FcKind.Io -> {
                            val shape = Path().apply {
                                if (node.kind == FcKind.Decision) {
                                    moveTo(x + w / 2, y); lineTo(x + w, y + h / 2); lineTo(x + w / 2, y + h); lineTo(x, y + h / 2)
                                } else {
                                    val skew = 14f * k
                                    moveTo(x + skew, y); lineTo(x + w, y); lineTo(x + w - skew, y + h); lineTo(x, y + h)
                                }
                                close()
                            }
                            drawPath(shape, fill)
                            drawPath(shape, stroke, style = Stroke(strokeW))
                        }
                        else -> {
                            val radius = CornerRadius(if (node.kind == FcKind.Process) 6f * k else h / 2)
                            drawRoundRect(fill, Offset(x, y), Size(w, h), radius)
                            drawRoundRect(stroke, Offset(x, y), Size(w, h), radius, style = Stroke(strokeW))
                        }
                    }

                    val style = font(FlowchartLayouter.FONT_SIZE, pal.text.a(alpha))
                    node.lines.forEachIndexed { li, line ->
                        val baseline = node.y + (li - (node.lines.size - 1) / 2.0) * FlowchartLayouter.LINE_HEIGHT + 5
                        centred(line, style, px(node.x), py(baseline))
                    }
                    }
                }

                chart.edges.forEachIndexed { i, e ->
                    val label = e.label ?: return@forEachIndexed
                    val pts = layout.routes[i]
                    if (pts.size < 2) return@forEachIndexed
                    // Sit on the first segment that is long enough to carry the label chip.
                    val seg = if (FlowchartLayouter.dist(pts[0], pts[1]) < 36 && pts.size > 2) 1 else 0
                    val lx = (pts[seg].x + pts[seg + 1].x) / 2
                    val ly = (pts[seg].y + pts[seg + 1].y) / 2
                    val labelW = label.length * 6.8 + 12
                    val shown = prog(chart.nodes[e.from].layer * LAYER_STEP + EDGE_DELAY + EDGE_DRAW * 0.6f, 0.3f)
                    if (shown <= 0f) return@forEachIndexed
                    drawRoundRect(pal.bg.a(shown), Offset(px(lx - labelW / 2), py(ly - 10)), Size((labelW * k).toFloat(), 20f * k), CornerRadius(5f * k))
                    centred(label, font(13.0, parseDiagramColor(e.color, pal.text).a(shown), FontWeight.SemiBold), px(lx), py(ly + 4.5))
                }
            }
        }
    }
}

private const val LAYER_STEP = 0.12f   // seconds between one layer appearing and the next
private const val EDGE_DELAY = 0.2f    // an edge starts drawing this long after its source node starts to appear
private const val EDGE_DRAW = 0.5f

/** Ease with a little overshoot, so nodes settle into place. */
private fun easeOutBack(x: Float): Float {
    val c1 = 1.4f
    val c3 = c1 + 1f
    val u = x - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

/** Polyline with small rounded corners (`PathD` in FlowchartRenderer.cs). */
private fun roundedPolyline(p: List<FcPoint>, px: (Double) -> Float, py: (Double) -> Float, radius: Float): Path {
    val path = Path()
    path.moveTo(px(p[0].x), py(p[0].y))
    for (i in 1 until p.size - 1) {
        val a = Offset(px(p[i - 1].x), py(p[i - 1].y))
        val b = Offset(px(p[i].x), py(p[i].y))
        val c = Offset(px(p[i + 1].x), py(p[i + 1].y))
        val l1 = (a - b).getDistance()
        val l2 = (c - b).getDistance()
        val r = min(radius, min(l1, l2) / 2)
        if (r < 0.5f) { path.lineTo(b.x, b.y); continue }
        val p1 = b + (a - b) / l1 * r
        val p2 = b + (c - b) / l2 * r
        path.lineTo(p1.x, p1.y)
        path.quadraticTo(b.x, b.y, p2.x, p2.y)
    }
    path.lineTo(px(p.last().x), py(p.last().y))
    return path
}

/** The SVG marker: 10x10 triangle scaled to 11 units, ref point at x = 9 (so the tip overshoots the path end by 1.1). */
private fun DrawScope.arrowHead(from: FcPoint, end: FcPoint, color: Color, k: Float, px: (Double) -> Float, py: (Double) -> Float) {
    val e = Offset(px(end.x), py(end.y))
    val dir = e - Offset(px(from.x), py(from.y))
    val len = dir.getDistance()
    if (len < 0.01f) return
    val d = dir / len
    val n = Offset(-d.y, d.x)
    val tip = e + d * (1.1f * k)
    val back = e - d * (9.9f * k)
    drawPath(Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x + n.x * 5.5f * k, back.y + n.y * 5.5f * k)
        lineTo(back.x - n.x * 5.5f * k, back.y - n.y * 5.5f * k)
        close()
    }, color)
}
