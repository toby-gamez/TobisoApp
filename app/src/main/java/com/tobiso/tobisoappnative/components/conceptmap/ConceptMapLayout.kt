package com.tobiso.tobisoappnative.components.conceptmap

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Starting layout for the "Myšlenková mapa" AI tool, after the web's mind-map.js: each node's children fan out on a ring
 * around THAT node (not around one global centre), so branches look organic. Positions are in an unbounded plane and
 * are only a starting point - the viewer lets the user drag nodes anywhere.
 */
object ConceptMapLayout {

    private const val MIN_RADIUS = 110f

    fun compute(nodeIds: List<String>, edges: List<Pair<String, String>>): Map<String, Offset> {
        if (nodeIds.isEmpty()) return emptyMap()
        val known = nodeIds.toSet()
        val outgoing = nodeIds.associateWith { mutableListOf<String>() }
        val incoming = nodeIds.associateWith { 0 }.toMutableMap()
        edges.filter { (a, b) -> a in known && b in known && a != b }.distinct().forEach { (a, b) ->
            outgoing.getValue(a).add(b)
            incoming[b] = incoming.getValue(b) + 1
        }

        // Breadth-first spanning tree from every root (no incoming edge) at once; a node reachable from two parents keeps
        // the first one. Anything left over (cycles, islands) becomes a root of its own.
        val parentOf = HashMap<String, String>()
        val visited = HashSet<String>()
        val roots = mutableListOf<String>()
        fun bfs(start: List<String>) {
            val queue = ArrayDeque(start)
            visited.addAll(start)
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                for (child in outgoing.getValue(id)) {
                    if (visited.add(child)) { parentOf[child] = id; queue.addLast(child) }
                }
            }
        }
        val initial = nodeIds.filter { incoming.getValue(it) == 0 }.ifEmpty { listOf(nodeIds.first()) }
        roots.addAll(initial)
        bfs(initial)
        for (id in nodeIds) if (id !in visited) { roots.add(id); bfs(listOf(id)) }

        val children = nodeIds.associateWith { mutableListOf<String>() }
        nodeIds.forEach { id -> parentOf[id]?.let { children.getValue(it).add(id) } }

        val leaves = HashMap<String, Int>()
        fun leafCount(id: String): Int = leaves.getOrPut(id) {
            val kids = children.getValue(id)
            if (kids.isEmpty()) 1 else kids.sumOf { leafCount(it) }
        }

        val positions = LinkedHashMap<String, Offset>()
        fun place(id: String, at: Offset, arrival: Double, depth: Int, fullCircle: Boolean) {
            positions[id] = at
            val kids = children.getValue(id)
            if (kids.isEmpty()) return
            val radius = MIN_RADIUS + 14f * sqrt(leafCount(id).toFloat()) + 18f * min(kids.size, 8) + 6f * depth
            val span = if (fullCircle) 2 * PI else min(2 * PI, PI * (0.55 + 0.14 * kids.size))
            kids.forEachIndexed { index, kid ->
                val angle = if (fullCircle) arrival + 2 * PI * index / kids.size
                else arrival + (index - (kids.size - 1) / 2.0) * (span / max(1, kids.size))
                val childAt = at + Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
                place(kid, childAt, angle, depth + 1, fullCircle = false)
            }
        }

        if (roots.size == 1) {
            place(roots.first(), Offset.Zero, -PI / 2, 0, fullCircle = true)
        } else {
            val ring = 260f + 40f * roots.size
            roots.forEachIndexed { index, root ->
                val angle = -PI / 2 + 2 * PI * index / roots.size
                place(root, Offset((ring * cos(angle)).toFloat(), (ring * sin(angle)).toFloat()), angle, 0, fullCircle = false)
            }
        }
        return positions
    }

    /** Scale and offset that fit [bounds] (centre-based boxes) into a viewport of the given size with a margin. */
    fun fit(positions: Collection<Offset>, halfWidth: Float, halfHeight: Float, viewWidth: Float, viewHeight: Float, margin: Float = 24f): Pair<Float, Offset> {
        if (positions.isEmpty() || viewWidth <= 0f || viewHeight <= 0f) return 1f to Offset.Zero
        val minX = positions.minOf { it.x } - halfWidth
        val maxX = positions.maxOf { it.x } + halfWidth
        val minY = positions.minOf { it.y } - halfHeight
        val maxY = positions.maxOf { it.y } + halfHeight
        val w = max(1f, maxX - minX)
        val h = max(1f, maxY - minY)
        val scale = min((viewWidth - 2 * margin) / w, (viewHeight - 2 * margin) / h).coerceIn(0.2f, 1.5f)
        val center = Offset((minX + maxX) / 2, (minY + maxY) / 2)
        return scale to Offset(viewWidth / 2 - center.x * scale, viewHeight / 2 - center.y * scale)
    }
}
