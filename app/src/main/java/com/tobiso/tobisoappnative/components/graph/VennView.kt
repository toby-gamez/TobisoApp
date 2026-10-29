package com.tobiso.tobisoappnative.components.graph

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import com.tobiso.tobisoappnative.components.cozyEntrance
import com.tobiso.tobisoappnative.components.cozyPress
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.ui.theme.Figtree
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** A `[vennN]` block: 2–3 sets, optional tappable terms (průnik, sjednocení, rozdíl, doplněk) — like the web's `post-venn`. */
@Composable
fun VennView(source: String, modifier: Modifier = Modifier) {
    val venn = remember(source) { VennParser.parse(source) }
    if (venn.sets.size < 2) {
        val reason = venn.warnings.joinToString(" ").ifEmpty { "Chybí množiny." }
        DiagramError("Vennův diagram se nepodařilo vykreslit – $reason (set(name: A): 1; 2; 3)", modifier)
        return
    }
    val geo = remember(venn) { VennGeometries.of(venn.sets.size) }
    val ops = remember(venn) { VennParser.visibleOps(venn) }
    var activeKey by remember(venn) { mutableStateOf(venn.showKeys.firstOrNull { k -> ops.any { it.key == k } }) }
    val active = ops.firstOrNull { it.key == activeKey }
    // Cozy transitions: circles settle to outlines, the result region fades in, other text dims — all springy.
    val soft = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    val activeP = animateFloatAsState(if (active != null) 1f else 0f, soft, label = "vennActive")
    val lit = List(1 shl venn.sets.size) { mask ->
        animateFloatAsState(if (active?.masks?.contains(mask) == true) 1f else 0f, soft, label = "vennRegion$mask")
    }
    val universe = venn.universe || venn.outside.isNotEmpty() || ops.any { it.category == "doplnek" }
    val view = if (universe) geo.viewUniverse else geo.viewTight

    // Overflow warnings are static (they depend on the text only), so compute them once.
    val warnings = remember(venn) {
        venn.warnings + venn.regions.mapNotNull { (mask, items) ->
            val slot = geo.slots[mask] ?: return@mapNotNull null
            if (fitVennText(items, slot).fits) null
            else "V oblasti „${VennParser.regionName(venn, mask)}“ je příliš mnoho prvků, nevejdou se do diagramu."
        } + (if (venn.outside.isNotEmpty() && wrapVennElements(venn.outside, 18).size > 5) listOf("V oblasti „mimo množiny“ je příliš mnoho prvků, nevejdou se do diagramu.") else emptyList())
    }

    val measurer = rememberTextMeasurer()
    val pal = rememberWebPalette()
    val setColors = venn.sets.map { parseDiagramColor(it.color, pal.text) }

    // One path per region: inside every listed circle, outside all the others, within the universe box.
    val regionPaths = remember(venn, geo) {
        val u = geo.universeRect
        (0 until (1 shl venn.sets.size)).associateWith { mask ->
            var path = Path().apply { addRect(Rect(u.x.toFloat(), u.y.toFloat(), (u.x + u.w).toFloat(), (u.y + u.h).toFloat())) }
            geo.circles.forEachIndexed { i, c ->
                val circle = Path().apply { addOval(Rect(Offset(c.x.toFloat(), c.y.toFloat()), c.r.toFloat())) }
                val op = if (mask and (1 shl i) != 0) PathOperation.Intersect else PathOperation.Difference
                path = Path.combine(op, path, circle)
            }
            path
        }
    }

    DiagramFrame(venn.title, warnings, venn.notes, pal, modifier.cozyEntrance(key = source)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = min(maxWidth.value, view.w.toFloat())
            val height = width * (view.h / view.w).toFloat()
            Canvas(Modifier.size(width.dp, height.dp)) {
                val k = size.width / view.w.toFloat()
                fun px(x: Double) = ((x - view.x) * k).toFloat()
                fun py(y: Double) = ((y - view.y) * k).toFloat()
                val p = activeP.value

                // Text positioned by SVG baseline (y), like <text x y>.
                fun text(str: String, sizeUnits: Double, x: Double, baseline: Double, anchor: VennAnchor, color: Color, weight: FontWeight? = null, italic: Boolean = false) {
                    val style = TextStyle(
                        color = color, fontSize = (sizeUnits * k).toFloat().toSp(), fontFamily = Figtree,
                        fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else null
                    )
                    val r = measurer.measure(str, style)
                    val left = when (anchor) {
                        VennAnchor.Start -> px(x)
                        VennAnchor.Middle -> px(x) - r.size.width / 2f
                        VennAnchor.End -> px(x) - r.size.width
                    }
                    drawText(r, topLeft = Offset(left, py(baseline) - r.firstBaseline))
                }

                if (universe) {
                    val u = geo.universeRect
                    drawRoundRect(pal.neutral600, Offset(px(u.x), py(u.y)), Size((u.w * k).toFloat(), (u.h * k).toFloat()),
                        CornerRadius(10f * k), style = Stroke(1.5f * k))
                    text("U", 15.0, u.x + 12, u.y + 22, VennAnchor.Start, pal.text, italic = true)
                }

                // .venn-circle: fill .28 / stroke .9 at 2.5; while a term is shown (.venn-active) every circle
                // goes outline-only (fill .05, stroke .6) so the filled region IS the result.
                geo.circles.forEachIndexed { i, c ->
                    val center = Offset(px(c.x), py(c.y))
                    drawCircle(setColors[i].copy(alpha = 0.28f + (0.05f - 0.28f) * p), (c.r * k).toFloat(), center)
                    drawCircle(setColors[i].copy(alpha = 0.9f + (0.6f - 0.9f) * p), (c.r * k).toFloat(), center, style = Stroke(2.5f * k))
                }

                // .venn-region.venn-hl: the result, accent colour at .62 over the circles.
                withTransform({
                    translate(-view.x.toFloat() * k, -view.y.toFloat() * k)
                    scale(k, k, pivot = Offset.Zero)
                }) {
                    lit.forEachIndexed { mask, a ->
                        if (a.value > 0.01f) regionPaths[mask]?.let { drawPath(it, pal.vennHighlight.copy(alpha = 0.62f * a.value)) }
                    }
                }

                // .venn-text: elements outside the result fade to .4, those inside turn bold.
                fun elementStyle(mask: Int): Pair<Color, FontWeight?> {
                    val l = lit.getOrNull(mask)?.value ?: 0f
                    return pal.text.copy(alpha = 1f - p * (1f - l) * 0.6f) to (if (l > 0.5f) FontWeight.Bold else null)
                }

                venn.regions.forEach { (mask, items) ->
                    val slot = geo.slots[mask] ?: return@forEach
                    val fitted = fitVennText(items, slot)
                    val lineH = fitted.size + 4
                    val (color, weight) = elementStyle(mask)
                    fitted.lines.forEachIndexed { li, line ->
                        text(line, fitted.size, slot.x, slot.y + (li - (fitted.lines.size - 1) / 2.0) * lineH + fitted.size * 0.35, VennAnchor.Middle, color, weight)
                    }
                }

                venn.sets.forEachIndexed { i, set ->
                    val count = venn.regions.filterKeys { it and (1 shl i) != 0 }.values.sumOf { it.size }
                    val a = geo.nameAnchors[i]
                    text(if (count > 0) "${set.name} ($count)" else set.name, 16.0, a.x, a.y, a.anchor, setColors[i], FontWeight.Bold)
                }

                if (venn.outside.isNotEmpty()) {
                    val (ox, oy) = geo.outsideAnchor
                    val (color, weight) = elementStyle(0)
                    wrapVennElements(venn.outside, 18).take(5).forEachIndexed { li, line ->
                        text(line, 13.0, ox + 22, oy + 22 + li * 17, VennAnchor.Start, color, weight)
                    }
                }
            }
        }

        if (ops.isNotEmpty()) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ops.groupBy { it.category }.forEach { (category, group) ->
                    Text(VennParser.categoryTitle(category), style = webText(12, pal.neutral700, FontWeight.Bold))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.forEach { op ->
                            VennChip(op.label, op.key == activeKey, pal) { activeKey = if (op.key == activeKey) null else op.key }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(14.dp).background(pal.vennHighlight.copy(alpha = 0.62f), RoundedCornerShape(3.dp)))
                    Text("vybarvená část = výsledek pojmu", style = webText(12, pal.neutral700))
                }
                Crossfade(active?.description ?: VennParser.IDLE_TEXT, animationSpec = tween(220), label = "vennDesc") { text ->
                    Text(
                        text,
                        style = webText(13, pal.neutral700).copy(lineHeight = 18.2.sp),
                        modifier = Modifier.heightIn(min = 33.8.dp) // min-height: 2.6em at 13px
                    )
                }
            }
        }
    }
}

/** Term pill: colours glide between states and it squishes a little while pressed. */
@Composable
private fun VennChip(label: String, pressed: Boolean, pal: WebDiagramPalette, onClick: () -> Unit) {
    val spec = tween<Color>(180)
    val bg by animateColorAsState(if (pressed) pal.accent600 else pal.bg, spec, label = "chipBg")
    val border by animateColorAsState(if (pressed) pal.accent600 else pal.neutral600, spec, label = "chipBorder")
    val fg by animateColorAsState(if (pressed) Color.White else pal.text, spec, label = "chipFg")
    Text(
        label,
        style = webText(13, fg),
        modifier = Modifier
            .cozyPress()
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun webText(size: Int, color: Color, weight: FontWeight? = null) =
    TextStyle(fontFamily = Figtree, fontSize = size.sp, color = color, fontWeight = weight)
