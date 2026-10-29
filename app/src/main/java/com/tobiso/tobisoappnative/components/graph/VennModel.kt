package com.tobiso.tobisoappnative.components.graph

import com.tobiso.tobisoappnative.components.graph.DiagramSupport.lineRegex
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Port of the web's `[vennN]…[/vennN]` blocks (Tobiso.Web.Shared/Graphs/VennRenderer): 2 or 3 sets,
 * elements land in the region given by set membership. Pure Kotlin; drawing lives in `VennView.kt`.
 */

class VennSet(val name: String, val color: String, val elements: List<String>)

/** A selectable term (průnik, sjednocení, rozdíl, doplněk): the regions (bit masks, 0 = outside all sets) it covers. */
class VennOp(val category: String, val key: String, val label: String, val masks: Set<Int>, val description: String)

class Venn {
    var title: String? = null
    var universe = false
    val sets = mutableListOf<VennSet>()
    /** Region mask -> elements placed by `only/both/all`. */
    val explicit = LinkedHashMap<Int, MutableList<String>>()
    val outside = mutableListOf<String>()
    val notes = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val ops = linkedSetOf<String>()
    val showKeys = mutableListOf<String>()

    /** Final element lists per region mask (membership first, then explicit placements). */
    val regions: Map<Int, List<String>> by lazy {
        val result = sortedMapOf<Int, MutableList<String>>()
        fun put(mask: Int, el: String) {
            val list = result.getOrPut(mask) { mutableListOf() }
            if (list.none { it.equals(el, ignoreCase = true) }) list += el
        }
        val seen = mutableListOf<String>()
        for (s in sets) for (el in s.elements) if (seen.none { it.equals(el, ignoreCase = true) }) seen += el
        for (el in seen) {
            var mask = 0
            sets.forEachIndexed { i, s -> if (s.elements.any { it.equals(el, ignoreCase = true) }) mask = mask or (1 shl i) }
            put(mask, el)
        }
        for ((mask, items) in explicit) for (el in items) put(mask, el)
        result
    }
}

object VennParser {
    /** `[vennX] … [/vennX]` on their own lines; the closing tag must repeat the name. */
    val blockRegex = Regex(
        """^[ \t]*\[(venn[\w-]*)][ \t]*\r?\n(.*?)^[ \t]*\[/\1][ \t]*\r?$""",
        setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val defaultColors = listOf("#2f6fdf", "#e8833a", "#3a9d5d")
    val opCategories = listOf("prunik", "sjednoceni", "rozdil", "doplnek")
    private val regionKinds = setOf("only", "jen", "pouze", "both", "prunik", "all", "vse", "outside", "mimo")

    /** Splits on ";" when present, else on ",", else on whitespace. */
    fun splitElements(value: String): List<String> {
        val v = value.trim()
        if (v.isEmpty()) return emptyList()
        val parts = when {
            v.contains(';') -> v.split(';')
            v.contains(',') -> v.split(',')
            else -> v.split(Regex("\\s+"))
        }
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private class RegionLine(val kind: String, val args: String, val items: List<String>)

    fun parse(body: String): Venn {
        val v = Venn()
        val regionLines = mutableListOf<RegionLine>()
        val showLines = mutableListOf<List<String>>()

        for (rawLine in body.split('\n')) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("//")) continue
            val lm = lineRegex.matchEntire(line) ?: continue
            val key = DiagramSupport.normalize(lm.groupValues[1])
            val value = lm.groupValues[3].trim()
            val rawArgs = lm.groups[2]?.value
            val args = DiagramSupport.parseArgs(rawArgs)

            when {
                key == "title" || key == "nazev" || key == "nadpis" -> if (value.isNotEmpty()) v.title = value
                key == "note" || key == "poznamka" -> if (value.isNotEmpty()) v.notes += value
                key == "show" || key == "zobraz" || key == "pojem" ->
                    showLines += if (value.contains(';')) value.split(';').map { it.trim() }.filter { it.isNotEmpty() }
                    else value.split(Regex("\\s+")).filter { it.isNotEmpty() }
                key in setOf("playground", "hriste", "hra", "operations", "operace", "pojmy") ->
                    for (token in value.split(',', ';', ' ').filter { it.isNotEmpty() }.map { DiagramSupport.normalize(it) }) {
                        when (token) {
                            "ano", "yes", "true", "vse", "all", "1" -> v.ops.addAll(opCategories)
                            "ne", "no", "false", "0" -> v.ops.clear()
                            in opCategories -> v.ops += token
                            else -> v.warnings += "Neznámý pojem „$token“ (použij prunik, sjednoceni, rozdil, doplnek nebo ano)."
                        }
                    }
                key == "universe" || key == "univerzum" ->
                    v.universe = DiagramSupport.normalize(value) in setOf("yes", "ano", "true", "1")
                key == "set" || key == "mnozina" -> {
                    if (v.sets.size >= 3) {
                        v.warnings += "Vennův diagram podporuje nejvýše 3 množiny, další jsou ignorovány."
                        continue
                    }
                    var name = args["name"] ?: args["nazev"] ?: args[""] ?: ""
                    if (name.isEmpty()) name = ('A' + v.sets.size).toString()

                    var color = args["color"] ?: args["barva"]
                    val items = splitElements(value).toMutableList()
                    // Whitespace-separated lists may end with a bare colour name (e.g. "1 2 3 BLUE").
                    if (color == null && items.isNotEmpty() && !value.contains(';') && !value.contains(',')
                        && GraphParser.resolveColor(items.last()) != null && items.last() == items.last().uppercase()
                    ) {
                        color = items.removeAt(items.size - 1)
                    }
                    var hex: String? = null
                    if (color != null) {
                        val c = DiagramSupport.color(color)
                        if (!c.known) v.warnings += "Neznámá barva „$color“." else hex = c.hex
                    }
                    v.sets += VennSet(name, hex ?: defaultColors[v.sets.size], items)
                }
                key in regionKinds -> regionLines += RegionLine(key, rawArgs ?: "", splitElements(value))
            }
        }

        // Region lines can only be resolved once all sets (and their names) are known.
        for (rl in regionLines) {
            when (rl.kind) {
                "outside", "mimo" -> v.outside += rl.items
                "all", "vse" -> addExplicit(v, (1 shl v.sets.size) - 1, rl.items)
                else -> {
                    val refs = rl.args.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    var mask = 0
                    var ok = refs.isNotEmpty()
                    for (r in refs) {
                        val idx = findSet(v, r)
                        if (idx < 0) { ok = false; v.warnings += "Množina „$r“ neexistuje."; break }
                        mask = mask or (1 shl idx)
                    }
                    if (ok) addExplicit(v, mask, rl.items)
                }
            }
        }

        for (tokens in showLines) {
            val key = resolveShowKey(v, tokens)
            if (key == null) v.warnings += "Pojem „${tokens.joinToString(" ")}“ se nepodařilo přečíst (např. show: prunik A B, show: rozdil A B, show: doplnek A)."
            else if (key !in v.showKeys) v.showKeys += key
        }

        if (v.sets.size < 2 && v.warnings.isEmpty()) v.warnings += "Vennův diagram potřebuje alespoň 2 množiny."
        return v
    }

    // "prunik A B", "sjednoceni A B C", "rozdil A B" (A \ B), "doplnek A" -> op key, null when invalid.
    private fun resolveShowKey(v: Venn, tokens: List<String>): String? {
        if (tokens.size < 2) return null
        val category = DiagramSupport.normalize(tokens[0])
        val idx = mutableListOf<Int>()
        for (t in tokens.drop(1)) {
            val i = findSet(v, t)
            if (i < 0) { v.warnings += "Množina „$t“ neexistuje."; return null }
            idx += i
        }
        if (idx.distinct().size != idx.size) return null
        return when {
            (category == "prunik" || category == "sjednoceni") && idx.size >= 2 -> "$category:${idx.sorted().joinToString(",")}"
            category == "rozdil" && idx.size == 2 -> "rozdil:${idx[0]},${idx[1]}"
            category == "doplnek" && idx.size == 1 -> "doplnek:${idx[0]}"
            else -> null
        }
    }

    private fun findSet(v: Venn, reference: String): Int {
        for (i in v.sets.indices) if (DiagramSupport.normalize(v.sets[i].name) == DiagramSupport.normalize(reference)) return i
        val n = reference.toIntOrNull()
        if (n != null && n in 1..v.sets.size) return n - 1
        return -1
    }

    private fun addExplicit(v: Venn, mask: Int, items: List<String>) {
        v.explicit.getOrPut(mask) { mutableListOf() }.addAll(items)
    }

    // ── Selectable terms ──────────────────────────────────────────────────

    const val IDLE_TEXT = "Klepni na pojem a ukáže se v diagramu."

    fun categoryTitle(category: String) = when (category) {
        "prunik" -> "Průnik"
        "sjednoceni" -> "Sjednocení"
        "rozdil" -> "Rozdíl"
        else -> "Doplněk"
    }

    /** Every term of every category, each with a plain-language definition and the elements that fall into it. */
    fun buildOps(v: Venn): List<VennOp> {
        val n = v.sets.size
        if (n < 2) return emptyList()
        val full = (1 shl n) - 1
        val ops = mutableListOf<VennOp>()
        fun name(i: Int) = v.sets[i].name
        fun masks(pred: (Int) -> Boolean) = (0..full).filter(pred).toSet()

        fun elements(masks: Set<Int>): String {
            val items = mutableListOf<String>()
            for (m in masks.sorted()) {
                v.regions[m]?.let { items += it }
                if (m == 0) items += v.outside
            }
            val distinct = items.distinctBy { it.lowercase() }
            return if (distinct.isEmpty()) " Prvky: žádné (∅)." else " Prvky: {${distinct.joinToString(", ")}}."
        }

        fun add(category: String, key: String, label: String, masks: Set<Int>, meaning: String) {
            ops += VennOp(category, "$category:$key", label, masks, meaning + elements(masks))
        }
        fun inSet(m: Int, i: Int) = (m shr i) and 1 == 1
        val pairs = (0 until n).flatMap { i -> (i + 1 until n).map { j -> i to j } }
        val allNames = (0 until n)

        for ((i, j) in pairs) add("prunik", "$i,$j", "${name(i)} ∩ ${name(j)}", masks { inSet(it, i) && inSet(it, j) },
            "Průnik množin ${name(i)} a ${name(j)} (${name(i)} ∩ ${name(j)}) tvoří prvky, které patří do ${name(i)} i do ${name(j)}.")
        if (n == 3) add("prunik", "0,1,2", allNames.joinToString(" ∩ ") { name(it) }, masks { it == full },
            "Průnik všech tří množin (${allNames.joinToString(" ∩ ") { name(it) }}) tvoří prvky, které patří do všech tří množin zároveň.")

        for ((i, j) in pairs) add("sjednoceni", "$i,$j", "${name(i)} ∪ ${name(j)}", masks { inSet(it, i) || inSet(it, j) },
            "Sjednocení množin ${name(i)} a ${name(j)} (${name(i)} ∪ ${name(j)}) tvoří prvky, které patří aspoň do jedné z nich (prvky z průniku se počítají jen jednou).")
        if (n == 3) add("sjednoceni", "0,1,2", allNames.joinToString(" ∪ ") { name(it) }, masks { it != 0 },
            "Sjednocení všech tří množin (${allNames.joinToString(" ∪ ") { name(it) }}) tvoří prvky, které patří aspoň do jedné z nich.")

        for (a in 0 until n) for (b in 0 until n) {
            if (a == b) continue
            add("rozdil", "$a,$b", "${name(a)} \\ ${name(b)}", masks { inSet(it, a) && !inSet(it, b) },
                "Rozdíl ${name(a)} \\ ${name(b)} tvoří prvky, které patří do ${name(a)}, ale ne do ${name(b)}.")
        }

        for (a in 0 until n) add("doplnek", "$a", "${name(a)}′", masks { !inSet(it, a) },
            "Doplněk množiny ${name(a)} (${name(a)}′) tvoří všechny prvky univerza (rámečku U), které do ${name(a)} nepatří.")
        return ops
    }

    /** The terms offered to the reader: whole categories from `playground`, plus any term the author picked via `show`. */
    fun visibleOps(v: Venn): List<VennOp> {
        val all = buildOps(v)
        return if (v.ops.isNotEmpty()) all.filter { it.category in v.ops || it.key in v.showKeys }
        else v.showKeys.mapNotNull { k -> all.firstOrNull { it.key == k } }
    }

    fun regionName(v: Venn, mask: Int): String {
        val names = v.sets.indices.filter { mask and (1 shl it) != 0 }.map { v.sets[it].name }
        return if (names.size == 1) "jen ${names[0]}" else names.joinToString(" ∩ ")
    }
}

// ── Geometry (same numbers as the web's SVG, in its viewBox units) ────────

data class VennCircle(val x: Double, val y: Double, val r: Double)
/** Region box: centre plus the room available for text. */
data class VennSlot(val x: Double, val y: Double, val w: Double, val h: Double)
data class VennBox(val x: Double, val y: Double, val w: Double, val h: Double)
data class VennNameAnchor(val x: Double, val y: Double, val anchor: VennAnchor)

enum class VennAnchor { Start, Middle, End }

class VennGeometry(
    val circles: List<VennCircle>,
    val slots: Map<Int, VennSlot>,
    val nameAnchors: List<VennNameAnchor>,
    val universeRect: VennBox,
    val viewUniverse: VennBox,
    val viewTight: VennBox,
    val outsideAnchor: Pair<Double, Double>,
)

object VennGeometries {
    fun of(setCount: Int) = if (setCount == 2) two() else three()

    private fun two(): VennGeometry {
        val r = 125.0; val cy = 185.0; val ax = 215.0; val bx = 345.0
        return VennGeometry(
            circles = listOf(VennCircle(ax, cy, r), VennCircle(bx, cy, r)),
            slots = mapOf(
                1 to VennSlot(ax - 65, cy, 110.0, 190.0),
                2 to VennSlot(bx + 65, cy, 110.0, 190.0),
                3 to VennSlot((ax + bx) / 2, cy, 104.0, 200.0),
            ),
            nameAnchors = listOf(VennNameAnchor(272.0, 46.0, VennAnchor.End), VennNameAnchor(288.0, 46.0, VennAnchor.Start)),
            universeRect = VennBox(20.0, 14.0, 520.0, 324.0),
            viewUniverse = VennBox(0.0, 0.0, 560.0, 352.0),
            viewTight = VennBox(50.0, 24.0, 460.0, 306.0),
            outsideAnchor = 36.0 to 38.0,
        )
    }

    private fun three(): VennGeometry {
        val r = 120.0; val d = 130.0
        val ax = 245.0; val ay = 190.0; val bx = ax + d; val by = ay
        val cx = ax + d / 2; val cy = ay + d * sqrt(3.0) / 2
        val ox = (ax + bx + cx) / 3; val oy = (ay + by + cy) / 3

        fun away(px: Double, py: Double, dist: Double, w: Double, h: Double): VennSlot {
            val dx = px - ox; val dy = py - oy
            val len = sqrt(dx * dx + dy * dy)
            return if (len < 0.001) VennSlot(px, py, w, h) else VennSlot(px + dx / len * dist, py + dy / len * dist, w, h)
        }

        return VennGeometry(
            circles = listOf(VennCircle(ax, ay, r), VennCircle(bx, by, r), VennCircle(cx, cy, r)),
            slots = mapOf(
                1 to away(ax, ay, 0.58 * r, 104.0, 110.0),
                2 to away(bx, by, 0.58 * r, 104.0, 110.0),
                4 to away(cx, cy, 0.58 * r, 110.0, 96.0),
                3 to away((ax + bx) / 2, (ay + by) / 2, 0.45 * r, 78.0, 80.0),
                5 to away((ax + cx) / 2, (ay + cy) / 2, 0.45 * r, 66.0, 80.0),
                6 to away((bx + cx) / 2, (by + cy) / 2, 0.45 * r, 66.0, 80.0),
                7 to VennSlot(ox, oy + 8, 64.0, 60.0),
            ),
            nameAnchors = listOf(
                VennNameAnchor(ax - 0.866 * (r + 12), ay - 0.5 * (r + 12), VennAnchor.End),
                VennNameAnchor(bx + 0.866 * (r + 12), by - 0.5 * (r + 12), VennAnchor.Start),
                VennNameAnchor(cx, cy + r + 20, VennAnchor.Middle),
            ),
            universeRect = VennBox(20.0, 14.0, 580.0, 436.0),
            viewUniverse = VennBox(0.0, 0.0, 620.0, 464.0),
            viewTight = VennBox(10.0, 54.0, 600.0, 410.0),
            outsideAnchor = 36.0 to 38.0,
        )
    }
}

/** Elements joined with ", " and wrapped greedily to a line length. */
fun wrapVennElements(items: List<String>, maxChars: Int): List<String> {
    val lines = mutableListOf<String>()
    val current = StringBuilder()
    items.forEachIndexed { i, item ->
        val piece = if (i < items.size - 1) "$item," else item
        if (current.isNotEmpty() && current.length + 1 + piece.length > maxChars) {
            lines += current.toString()
            current.clear()
        }
        if (current.isNotEmpty()) current.append(' ')
        current.append(piece)
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}

class FittedText(val lines: List<String>, val size: Double, val fits: Boolean)

/** Largest font size at which the region's elements fit; the text is still returned (smallest size) when none does. */
fun fitVennText(items: List<String>, slot: VennSlot): FittedText {
    val sizes = listOf(14.0, 13.0, 12.0, 11.0, 10.0)
    var lines = emptyList<String>()
    var size = sizes.last()
    var fits = false
    for (s in sizes) {
        val maxChars = max(3, floor(slot.w / (s * 0.56)).toInt())
        lines = wrapVennElements(items, maxChars)
        size = s
        if (lines.size * (s + 4) <= slot.h && lines.all { it.length <= maxChars + 6 }) { fits = true; break }
    }
    return FittedText(lines, size, fits)
}
