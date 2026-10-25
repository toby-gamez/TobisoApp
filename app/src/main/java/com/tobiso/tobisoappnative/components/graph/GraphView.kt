package com.tobiso.tobisoappnative.components.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.components.buildAnnotatedStringFromParts
import com.tobiso.tobisoappnative.components.parseInlineParts
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private const val W = 640f
private const val H = 400f
private const val MARGIN_LEFT = 90f
private const val MARGIN_RIGHT = 28f
private const val MARGIN_TOP = 28f
private const val MARGIN_BOTTOM = 64f
private const val PLOT_W = W - MARGIN_LEFT - MARGIN_RIGHT
private const val PLOT_H = H - MARGIN_TOP - MARGIN_BOTTOM

private fun parseColor(hex: String?, fallback: Color): Color {
    if (hex == null) return fallback
    var h = hex.removePrefix("#")
    if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
    return h.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: fallback
}

/** A `[grafN]` block: title, plot, legend, errors and notes — same pieces as the web's `<figure class="post-graph">`. */
@Composable
fun GraphView(source: String, modifier: Modifier = Modifier) {
    val graph = remember(source) { GraphParser.parse(source) }
    if (graph.isEmpty) {
        val reason = graph.errors.joinToString(" ").ifEmpty { "Chybí osy (x: 0-10) i data (curve: [0;0] [1;1])." }
        Text(
            text = "Graf se nepodařilo vykreslit – $reason",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                .padding(12.dp)
        )
        return
    }
    val layout = remember(graph) { GraphMath.layout(graph) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        graph.title?.let {
            val annotated = remember(it, linkColor) { buildAnnotatedStringFromParts(parseInlineParts(it), linkColor) }
            Text(annotated, style = MaterialTheme.typography.titleSmall, color = textColor, modifier = Modifier.padding(bottom = 6.dp))
        }
        GraphCanvas(graph, layout, Modifier.fillMaxWidth().aspectRatio(W / H))

        val legend = graph.series.filter { !it.name.isNullOrEmpty() }.map { Triple(it.name!!, it.color, false) } +
            graph.functions.map { Triple(it.name ?: "", it.color, false) } +
            graph.markers.filter { !it.name.isNullOrEmpty() }.map { Triple(it.name!!, it.color, true) }
        if (legend.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                legend.forEach { (name, color, isPoint) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(if (isPoint) 10.dp else 14.dp, 10.dp)
                                .background(parseColor(color, textColor), if (isPoint) CircleShape else RoundedCornerShape(3.dp))
                        )
                        Text(name, style = MaterialTheme.typography.bodySmall, color = textColor, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        graph.errors.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }
        graph.notes.forEach { note ->
            val annotated = remember(note, linkColor) { buildAnnotatedStringFromParts(parseInlineParts(note), linkColor) }
            Text(annotated, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun GraphCanvas(g: Graph, l: GraphLayout, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = textColor.copy(alpha = 0.12f)
    val axisColor = textColor.copy(alpha = 0.75f)
    val tickStyle = TextStyle(color = textColor.copy(alpha = 0.85f), fontSize = 11.sp)
    val labelStyle = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val pointLabelStyle = TextStyle(color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val dotRing = MaterialTheme.colorScheme.surface

    Canvas(modifier) {
        val s = size.width / W
        fun sx(x: Double) = ((MARGIN_LEFT + (x - l.xMin) / (l.xMax - l.xMin) * PLOT_W) * s).toFloat()
        fun sy(y: Double) = ((MARGIN_TOP + PLOT_H - (y - l.yMin) / (l.yMax - l.yMin) * PLOT_H) * s).toFloat()
        val left = MARGIN_LEFT * s
        val top = MARGIN_TOP * s
        val right = (MARGIN_LEFT + PLOT_W) * s
        val bottom = (MARGIN_TOP + PLOT_H) * s

        val axisY = sy(0.0.coerceIn(l.yMin, l.yMax))
        val axisX = sx(0.0.coerceIn(l.xMin, l.xMax))
        val xAxisAtEdge = axisY >= bottom - 0.5f * s
        val xAxisAtTop = !xAxisAtEdge && axisY <= top + 0.5f * s
        val yAxisAtEdge = axisX <= left + 0.5f * s
        val interiorOrigin = l.xMin < 0 && l.xMax > 0 && l.yMin < 0 && l.yMax > 0

        val xTicks = GraphMath.ticks(l.xMin, l.xMax, l.xStep)
        val yTicks = GraphMath.ticks(l.yMin, l.yMax, l.yStep)
        val widest = xTicks.maxOfOrNull { GraphMath.formatTick(it, l.xStep).length } ?: 1
        val xEvery = GraphMath.labelEvery((widest * 7.dp.toPx() + 8.dp.toPx()).toDouble(), (right - left) * l.xStep / (l.xMax - l.xMin))
        val yEvery = GraphMath.labelEvery(16.dp.toPx().toDouble(), (bottom - top) * l.yStep / (l.yMax - l.yMin))
        val tick = 5f * s

        fun text(str: String, style: TextStyle, x: Float, y: Float, anchor: Anchor) {
            val r = measurer.measure(str, style)
            val ox = when (anchor) { Anchor.Start -> x; Anchor.Middle -> x - r.size.width / 2f; Anchor.End -> x - r.size.width }
            drawText(r, topLeft = Offset(ox, y - r.size.height / 2f))
        }

        // Grid + ticks
        xTicks.forEach { x ->
            val px = sx(x)
            if (abs(px - axisX) > 0.5f) drawLine(gridColor, Offset(px, top), Offset(px, bottom), 1f)
            drawLine(axisColor, Offset(px, axisY), Offset(px, axisY + tick), 1f)
            if ((interiorOrigin && x == 0.0) || Math.round(x / l.xStep) % xEvery != 0L) return@forEach
            val label = GraphMath.formatTick(x, l.xStep)
            if (xAxisAtTop) {
                val onY = abs(px - axisX) <= 0.5f
                text(label, tickStyle, if (onY) px + 8 * s else px, axisY - 14 * s, if (onY) Anchor.Start else Anchor.Middle)
            } else text(label, tickStyle, px, axisY + 18 * s, Anchor.Middle)
        }
        yTicks.forEach { y ->
            val py = sy(y)
            if (abs(py - axisY) > 0.5f) drawLine(gridColor, Offset(left, py), Offset(right, py), 1f)
            drawLine(axisColor, Offset(axisX - tick, py), Offset(axisX, py), 1f)
            if ((interiorOrigin && y == 0.0) || Math.round(y / l.yStep) % yEvery != 0L) return@forEach
            if (interiorOrigin && py > axisY && py - axisY < 30 * s) return@forEach
            text(GraphMath.formatTick(y, l.yStep), tickStyle, axisX - 8 * s, py, Anchor.End)
        }
        if (interiorOrigin) text("0", tickStyle, axisX - 8 * s, axisY + 18 * s, Anchor.End)

        // Axes with arrow heads
        val axisStroke = 1.5f * s
        drawLine(axisColor, Offset(left, axisY), Offset(right + 14 * s, axisY), axisStroke)
        drawPath(Path().apply {
            moveTo(right + 20 * s, axisY); lineTo(right + 11 * s, axisY - 4 * s); lineTo(right + 11 * s, axisY + 4 * s); close()
        }, axisColor)
        drawLine(axisColor, Offset(axisX, bottom), Offset(axisX, top - 14 * s), axisStroke)
        drawPath(Path().apply {
            moveTo(axisX, top - 20 * s); lineTo(axisX - 4 * s, top - 11 * s); lineTo(axisX + 4 * s, top - 11 * s); close()
        }, axisColor)

        // Axis labels
        if (g.x.label.isNotEmpty()) {
            if (xAxisAtEdge || xAxisAtTop) text(g.x.label, labelStyle, (left + right) / 2, (H - 12) * s, Anchor.Middle)
            else text(g.x.label, labelStyle, right + 20 * s, axisY - 14 * s, Anchor.End)
        }
        if (g.y.label.isNotEmpty()) {
            when {
                yAxisAtEdge -> withTransform({ rotate(-90f, Offset(16 * s, (top + bottom) / 2)) }) {
                    text(g.y.label, labelStyle, 16 * s, (top + bottom) / 2, Anchor.Middle)
                }
                axisX >= right - 0.5f * s -> text(g.y.label, labelStyle, axisX - 12 * s, top - 8 * s, Anchor.End)
                else -> text(g.y.label, labelStyle, axisX + 12 * s, top - 8 * s, Anchor.Start)
            }
        }

        // Data, clipped to the plot area
        val lineWidth = 2.2.dp.toPx()
        val dotR = 3.5.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
        clipRect(left - 4 * s, top - 4 * s, right + 4 * s, bottom + 4 * s) {
            g.series.forEach { ser ->
                val c = parseColor(ser.color, textColor)
                if (ser.points.size == 1) {
                    dot(ser.points[0], ::sx, ::sy, c, dotRing, dotR)
                } else {
                    drawPath(seriesPath(ser, ::sx, ::sy), c, style = Stroke(lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (ser.dashed) dash else null))
                }
            }
            g.functions.forEachIndexed { i, f ->
                val path = functionPath(l.samples[i], ::sx, ::sy, top, bottom)
                drawPath(path, parseColor(f.color, textColor), style = Stroke(lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (f.dashed) dash else null))
            }
            g.series.filter { it.showDots && it.points.size > 1 }.forEach { ser ->
                val c = parseColor(ser.color, textColor)
                ser.points.forEach { dot(it, ::sx, ::sy, c, dotRing, dotR) }
            }
            g.markers.forEach { m ->
                dot(GraphPoint(m.x, m.y), ::sx, ::sy, parseColor(m.color, textColor), dotRing, dotR * 1.2f)
                if (!m.name.isNullOrEmpty()) text(m.name, pointLabelStyle, sx(m.x) + 8 * s, sy(m.y) - 10 * s, Anchor.Start)
            }
        }
    }
}

private enum class Anchor { Start, Middle, End }

private fun DrawScope.dot(p: GraphPoint, sx: (Double) -> Float, sy: (Double) -> Float, color: Color, ring: Color, r: Float) {
    val c = Offset(sx(p.x), sy(p.y))
    drawCircle(ring, r + 1.5f, c)
    drawCircle(color, r, c)
}

private fun seriesPath(s: GraphSeries, sx: (Double) -> Float, sy: (Double) -> Float): Path {
    val pts = s.points
    val path = Path()
    path.moveTo(sx(pts[0].x), sy(pts[0].y))
    if (!s.smooth || pts.size == 2) {
        for (i in 1 until pts.size) path.lineTo(sx(pts[i].x), sy(pts[i].y))
        return path
    }
    var increasing = true
    for (i in 1 until pts.size) if (pts[i].x <= pts[i - 1].x) { increasing = false; break }

    if (increasing) {
        // Monotone cubic (Fritsch–Carlson): smooth but never overshoots, so flat parts stay flat.
        val n = pts.size
        val d = DoubleArray(n - 1) { (pts[it + 1].y - pts[it].y) / (pts[it + 1].x - pts[it].x) }
        val m = DoubleArray(n)
        m[0] = d[0]; m[n - 1] = d[n - 2]
        for (k in 1 until n - 1) m[k] = if (d[k - 1] * d[k] <= 0) 0.0 else (d[k - 1] + d[k]) / 2
        for (k in 0 until n - 1) {
            if (d[k] == 0.0) { m[k] = 0.0; m[k + 1] = 0.0; continue }
            val a = m[k] / d[k]; val b = m[k + 1] / d[k]
            val h = a * a + b * b
            if (h > 9) { val t = 3 / sqrt(h); m[k] = t * a * d[k]; m[k + 1] = t * b * d[k] }
        }
        for (k in 0 until n - 1) {
            val dx = (pts[k + 1].x - pts[k].x) / 3
            path.cubicTo(
                sx(pts[k].x + dx), sy(pts[k].y + m[k] * dx),
                sx(pts[k + 1].x - dx), sy(pts[k + 1].y - m[k + 1] * dx),
                sx(pts[k + 1].x), sy(pts[k + 1].y)
            )
        }
        return path
    }

    // x not strictly increasing: Catmull-Rom through all points.
    val sc = pts.map { Offset(sx(it.x), sy(it.y)) }
    for (i in 0 until sc.size - 1) {
        val p0 = sc[max(i - 1, 0)]; val p1 = sc[i]; val p2 = sc[i + 1]; val p3 = sc[minOf(i + 2, sc.size - 1)]
        path.cubicTo(
            p1.x + (p2.x - p0.x) / 6, p1.y + (p2.y - p0.y) / 6,
            p2.x - (p3.x - p1.x) / 6, p2.y - (p3.y - p1.y) / 6,
            p2.x, p2.y
        )
    }
    return path
}

private fun functionPath(samples: List<GraphPoint>, sx: (Double) -> Float, sy: (Double) -> Float, top: Float, bottom: Float): Path {
    val path = Path()
    // Keep off-screen points finite; the clip hides them.
    val limitTop = top - 2 * (bottom - top)
    val limitBottom = bottom + 2 * (bottom - top)
    var prevY: Float? = null
    for (p in samples) {
        if (p.y.isNaN()) { prevY = null; continue }
        val py = sy(p.y).coerceIn(limitTop, limitBottom)
        val prev = prevY
        // A jump from far above to far below (or back) between neighbouring samples is an asymptote: lift the pen.
        val jump = prev != null && ((prev < top && py > bottom) || (prev > bottom && py < top))
        if (prev == null || jump) path.moveTo(sx(p.x), py) else path.lineTo(sx(p.x), py)
        prevY = py
    }
    return path
}
