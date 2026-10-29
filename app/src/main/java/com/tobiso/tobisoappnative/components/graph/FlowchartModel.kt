package com.tobiso.tobisoappnative.components.graph

import com.tobiso.tobisoappnative.components.graph.DiagramSupport.lineRegex
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Port of the web's `[vyvojN]…[/vyvojN]` flowchart blocks (Tobiso.Web.Shared/Graphs/FlowchartRenderer).
 * Pure Kotlin so it is unit-testable; drawing lives in `FlowchartView.kt`.
 */

enum class FcKind { Start, End, Process, Decision, Io }

class FcNode(val id: String, val kind: FcKind, val label: String, val color: String?) {
    var lines: List<String> = emptyList()
    var w = 0.0; var h = 0.0      // screen size
    var m = 0.0; var c = 0.0      // size along the main (flow) and cross axis
    var cm = 0.0; var cc = 0.0    // centre along main / cross axis
    var layer = 0
    /** Screen centre, filled in by [FlowchartLayouter.layout]. */
    var x = 0.0; var y = 0.0
}

class FcEdge(val from: Int, val to: Int, val label: String?, val color: String?) {
    var back = false
}

class Flowchart {
    var title: String? = null
    var right = false
    val nodes = mutableListOf<FcNode>()
    val edges = mutableListOf<FcEdge>()
    val notes = mutableListOf<String>()
    val warnings = mutableListOf<String>()
}

data class FcPoint(val x: Double, val y: Double)

class FlowchartLayout(
    val minX: Double, val minY: Double, val width: Double, val height: Double,
    /** One orthogonal polyline per [Flowchart.edges] entry, in screen space. */
    val routes: List<List<FcPoint>>,
)

object FlowchartParser {
    /** `[vyvojX] … [/vyvojX]` on their own lines; the closing tag must repeat the name. */
    val blockRegex = Regex(
        """^[ \t]*\[(vyvoj[\w-]*)][ \t]*\r?\n(.*?)^[ \t]*\[/\1][ \t]*\r?$""",
        setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val edgeRegex = Regex("""^([\p{L}\p{N}_]+)\s*(?:->|→)\s*([\p{L}\p{N}_]+)(?:\s*\(([^)]*)\))?(?:\s+(\S+))?\s*$""")

    private val kindAliases = mapOf(
        "start" to FcKind.Start, "zacatek" to FcKind.Start,
        "end" to FcKind.End, "konec" to FcKind.End, "stop" to FcKind.End,
        "process" to FcKind.Process, "proces" to FcKind.Process, "krok" to FcKind.Process, "akce" to FcKind.Process,
        "decision" to FcKind.Decision, "podminka" to FcKind.Decision, "rozhodnuti" to FcKind.Decision, "vetveni" to FcKind.Decision,
        "io" to FcKind.Io, "vstup" to FcKind.Io, "vystup" to FcKind.Io, "data" to FcKind.Io,
    )

    private class Pending(val from: String, val to: String, val label: String?, val color: String?, val raw: String)

    fun parse(body: String): Flowchart {
        val d = Flowchart()
        val ids = HashMap<String, Int>()
        val pending = ArrayList<Pending>()

        for (rawLine in body.split('\n')) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("//")) continue

            val em = edgeRegex.matchEntire(line)
            if (em != null) {
                var color: String? = null
                val colorToken = em.groups[4]?.value
                if (colorToken != null) {
                    val c = DiagramSupport.color(colorToken)
                    if (!c.known) d.warnings += "Neznámá barva „$colorToken“." else color = c.hex
                }
                val label = em.groups[3]?.value?.trim()
                pending += Pending(em.groupValues[1], em.groupValues[2], label?.ifEmpty { null }, color, line)
                continue
            }

            val lm = lineRegex.matchEntire(line) ?: continue
            val rawKey = lm.groupValues[1]
            val key = DiagramSupport.normalize(rawKey)
            val value = lm.groupValues[3].trim()
            val args = DiagramSupport.parseArgs(lm.groups[2]?.value)

            when {
                key == "title" || key == "nazev" || key == "nadpis" -> if (value.isNotEmpty()) d.title = value
                key == "note" || key == "poznamka" -> if (value.isNotEmpty()) d.notes += value
                key == "direction" || key == "smer" ->
                    d.right = DiagramSupport.normalize(value) in setOf("right", "doprava", "vodorovne", "horizontal", "lr")
                kindAliases.containsKey(key) -> {
                    var id = args["id"] ?: args[""] ?: ""
                    if (id.isEmpty()) {
                        id = "n${d.nodes.size + 1}"
                        d.warnings += "Uzel „$value“ nemá id (např. $rawKey(id: a)), nelze na něj navázat šipku."
                    }
                    if (ids.containsKey(id.lowercase())) {
                        d.warnings += "Id „$id“ je použité vícekrát, druhý uzel je ignorován."
                        continue
                    }
                    var color: String? = null
                    val colorToken = args["color"] ?: args["barva"]
                    if (colorToken != null) {
                        val c = DiagramSupport.color(colorToken)
                        if (!c.known) d.warnings += "Neznámá barva „$colorToken“." else color = c.hex
                    }
                    ids[id.lowercase()] = d.nodes.size
                    d.nodes += FcNode(id, kindAliases.getValue(key), value, color)
                }
            }
        }

        for (p in pending) {
            val fi = ids[p.from.lowercase()]
            val ti = ids[p.to.lowercase()]
            if (fi == null || ti == null) {
                d.warnings += "Šipka „${p.raw}“ odkazuje na neznámý uzel."
                continue
            }
            d.edges += FcEdge(fi, ti, p.label, p.color)
        }

        if (d.nodes.size > 1) {
            val connected = d.edges.flatMap { listOf(it.from, it.to) }.toSet()
            d.nodes.forEachIndexed { i, n -> if (i !in connected) d.warnings += "Uzel „${n.id}“ nemá žádnou šipku." }
        }
        return d
    }
}

object FlowchartLayouter {
    private const val CHAR_W = 7.4
    private const val LINE_H = 18.0
    private const val INK_PAD = 16.0
    const val FONT_SIZE = 14.0
    const val LINE_HEIGHT = LINE_H

    private fun measure(n: FcNode, right: Boolean) {
        n.lines = DiagramSupport.wrap(n.label, if (n.kind == FcKind.Decision) 16 else 24)
        val textW = n.lines.maxOf { it.length } * CHAR_W
        val textH = n.lines.size * LINE_H
        when (n.kind) {
            FcKind.Decision -> { n.w = max(textW * 1.9 + 36, 130.0); n.h = max(textH * 1.9 + 24, 72.0) }
            FcKind.Io -> { n.w = max(textW + 60, 96.0); n.h = max(textH + 20, 42.0) }
            FcKind.Start, FcKind.End -> { n.w = max(textW + 44, 92.0); n.h = max(textH + 18, 38.0) }
            FcKind.Process -> { n.w = max(textW + 32, 92.0); n.h = max(textH + 20, 42.0) }
        }
        n.m = if (right) n.w else n.h
        n.c = if (right) n.h else n.w
    }

    /** Positions every node (sets [FcNode.x]/[FcNode.y]) and routes the edges. [d] must have at least one node. */
    fun layout(d: Flowchart): FlowchartLayout {
        val n = d.nodes.size
        d.nodes.forEach { measure(it, d.right) }

        val outs = Array(n) { mutableListOf<Int>() }
        val ins = Array(n) { mutableListOf<Int>() }
        d.edges.forEachIndexed { ei, e -> outs[e.from] += ei; ins[e.to] += ei }

        // Back edges (loops) are found by DFS from the roots and left out of the layering.
        val state = IntArray(n)
        fun dfs(u: Int) {
            state[u] = 1
            for (ei in outs[u]) {
                val e = d.edges[ei]
                if (state[e.to] == 1) e.back = true
                else if (state[e.to] == 0) dfs(e.to)
            }
            state[u] = 2
        }
        for (i in 0 until n) if (ins[i].isEmpty() && state[i] == 0) dfs(i)
        for (i in 0 until n) if (state[i] == 0) dfs(i)

        // Layer = longest path from a root over the remaining (acyclic) edges.
        for (pass in 0..n) {
            var changed = false
            for (e in d.edges) {
                if (e.back) continue
                if (d.nodes[e.to].layer < d.nodes[e.from].layer + 1) {
                    d.nodes[e.to].layer = d.nodes[e.from].layer + 1
                    changed = true
                }
            }
            if (!changed) break
        }

        val layerCount = d.nodes.maxOf { it.layer } + 1
        val layers = MutableList(layerCount) { mutableListOf<Int>() }
        for (i in 0 until n) layers[d.nodes[i].layer] += i

        fun neighbours(node: Int, parents: Boolean): List<Int> =
            (if (parents) ins[node] else outs[node]).map { d.edges[it] }.filter { !it.back }.map { if (parents) it.from else it.to }

        // Order inside layers by the barycenter of the neighbours (ties keep declaration order).
        val pos = DoubleArray(n)
        fun reindex() { for (layer in layers) layer.forEachIndexed { k, i -> pos[i] = k.toDouble() } }
        reindex()
        for (sweep in 0 until 4) {
            val down = sweep % 2 == 0
            for (step in 1 until layerCount) {
                val l = if (down) step else layerCount - 1 - step
                val key = layers[l].associateWith { i ->
                    val nb = neighbours(i, down)
                    if (nb.isEmpty()) pos[i] else nb.map { pos[it] }.average()
                }
                layers[l] = layers[l].sortedBy { key.getValue(it) }.toMutableList()
                reindex()
            }
        }

        // Cross-axis coordinates: centred rows first, then pulled towards the neighbours.
        val crossGap = if (d.right) 28.0 else 40.0
        for (layer in layers) {
            val total = layer.sumOf { d.nodes[it].c } + crossGap * (layer.size - 1)
            var cursor = -total / 2
            for (i in layer) {
                d.nodes[i].cc = cursor + d.nodes[i].c / 2
                cursor += d.nodes[i].c + crossGap
            }
        }

        fun relax(l: Int, useParents: Boolean) {
            val list = layers[l]
            val desired = DoubleArray(list.size) { k ->
                val nb = neighbours(list[k], useParents)
                if (nb.isNotEmpty()) nb.map { d.nodes[it].cc }.average() else d.nodes[list[k]].cc
            }
            val actual = DoubleArray(list.size)
            var prevRight = Double.NEGATIVE_INFINITY
            for (k in list.indices) {
                val node = d.nodes[list[k]]
                actual[k] = max(desired[k], prevRight + crossGap + node.c / 2)
                prevRight = actual[k] + node.c / 2
            }
            val shift = list.indices.map { desired[it] - actual[it] }.average()
            for (k in list.indices) d.nodes[list[k]].cc = actual[k] + shift
        }
        repeat(3) {
            for (l in 1 until layerCount) relax(l, true)
            for (l in layerCount - 2 downTo 0) relax(l, false)
        }
        for (l in 1 until layerCount) relax(l, true)

        // Main-axis coordinates.
        val mainGap = if (d.right) 76.0 else 58.0
        val layerStart = DoubleArray(layerCount)
        val layerEnd = DoubleArray(layerCount)
        var m = 0.0
        for (l in 0 until layerCount) {
            val size = layers[l].maxOf { d.nodes[it].m }
            layerStart[l] = m
            layerEnd[l] = m + size
            for (i in layers[l]) d.nodes[i].cm = m + size / 2
            m += size + mainGap
        }

        fun p(main: Double, cross: Double) = if (d.right) FcPoint(main, cross) else FcPoint(cross, main)

        val crossMax = d.nodes.maxOf { it.cc + it.c / 2 }
        val crossMin = d.nodes.minOf { it.cc - it.c / 2 }
        var channelPos = 0
        var channelNeg = 0

        // Route every edge as an orthogonal polyline in screen space.
        val routes = ArrayList<List<FcPoint>>()
        for (e in d.edges) {
            val u = d.nodes[e.from]
            val v = d.nodes[e.to]
            val span = v.layer - u.layer
            val pts = ArrayList<FcPoint>()

            if (!e.back && span == 1) {
                val dc = v.cc - u.cc
                val siblings = d.edges.count { it.from == e.from && !it.back }
                if (abs(dc) < 2) {
                    pts += p(u.cm + u.m / 2, u.cc)
                    pts += p(v.cm - v.m / 2, u.cc)
                } else if (u.kind == FcKind.Decision && siblings > 1 && abs(dc) > u.c / 2 + 8) {
                    val sgn = sign(dc)
                    pts += p(u.cm, u.cc + sgn * u.c / 2)
                    pts += p(u.cm, v.cc)
                    pts += p(v.cm - v.m / 2, v.cc)
                } else {
                    val mid = (layerEnd[u.layer] + layerStart[v.layer]) / 2
                    pts += p(u.cm + u.m / 2, u.cc)
                    pts += p(mid, u.cc)
                    pts += p(mid, v.cc)
                    pts += p(v.cm - v.m / 2, v.cc)
                }
            } else {
                // Loops and edges skipping layers run along the outside of the diagram.
                val side = if (e.back) 1 else if (v.cc >= u.cc) 1 else -1
                val ch = if (side > 0) crossMax + 26 + 14 * channelPos++ else crossMin - 26 - 14 * channelNeg++
                var startM = u.cm
                var endM = v.cm
                if (e.from == e.to) { startM -= min(10.0, u.m / 4); endM += min(10.0, u.m / 4) }
                pts += p(startM, u.cc + side * u.c / 2)
                pts += p(startM, ch)
                pts += p(endM, ch)
                pts += p(endM, v.cc + side * v.c / 2)
            }
            routes += dedupe(pts)
        }

        for (node in d.nodes) {
            val c = p(node.cm, node.cc)
            node.x = c.x; node.y = c.y
        }

        var minX = d.nodes.minOf { it.x - it.w / 2 }
        var maxX = d.nodes.maxOf { it.x + it.w / 2 }
        var minY = d.nodes.minOf { it.y - it.h / 2 }
        var maxY = d.nodes.maxOf { it.y + it.h / 2 }
        for (pt in routes.flatten()) {
            minX = min(minX, pt.x); maxX = max(maxX, pt.x)
            minY = min(minY, pt.y); maxY = max(maxY, pt.y)
        }
        minX -= INK_PAD + 8; maxX += INK_PAD + 8; minY -= INK_PAD; maxY += INK_PAD
        return FlowchartLayout(minX, minY, maxX - minX, maxY - minY, routes)
    }

    fun dist(a: FcPoint, b: FcPoint) = sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

    private fun dedupe(pts: List<FcPoint>): List<FcPoint> {
        val result = ArrayList<FcPoint>()
        for (pt in pts) if (result.isEmpty() || dist(result.last(), pt) > 0.01) result += pt
        return result
    }
}
