package com.tobiso.tobisoappnative.components.graph

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Port of the web's `[grafN]…[/grafN]` blocks (Tobiso.Web.Shared/Graphs). Pure Kotlin so it is unit-testable;
 * drawing lives in `GraphView.kt`.
 */

// ── Expressions ───────────────────────────────────────────────────────────

sealed class ExpressionResult {
    data class Ok(val f: (Double) -> Double) : ExpressionResult()
    data class Error(val message: String) : ExpressionResult()
}

object GraphExpression {
    private val functions: Map<String, (Double) -> Double> = mapOf(
        "sqrt" to { v: Double -> sqrt(v) }, "odm" to { v: Double -> sqrt(v) },
        "abs" to { v: Double -> abs(v) },
        "sin" to { v: Double -> kotlin.math.sin(v) }, "cos" to { v: Double -> kotlin.math.cos(v) },
        "tan" to { v: Double -> kotlin.math.tan(v) }, "tg" to { v: Double -> kotlin.math.tan(v) },
        "cot" to { v: Double -> 1 / kotlin.math.tan(v) }, "cotg" to { v: Double -> 1 / kotlin.math.tan(v) },
        "asin" to { v: Double -> kotlin.math.asin(v) }, "arcsin" to { v: Double -> kotlin.math.asin(v) },
        "acos" to { v: Double -> kotlin.math.acos(v) }, "arccos" to { v: Double -> kotlin.math.acos(v) },
        "atan" to { v: Double -> kotlin.math.atan(v) }, "arctan" to { v: Double -> kotlin.math.atan(v) }, "arctg" to { v: Double -> kotlin.math.atan(v) },
        "ln" to { v: Double -> kotlin.math.ln(v) }, "log" to { v: Double -> log10(v) },
        "exp" to { v: Double -> kotlin.math.exp(v) },
    )
    private val constants = mapOf("pi" to Math.PI, "e" to Math.E)

    // Longest first so "exp" wins over "e", "cotg" over "cot", "arcsin" over "asin"…
    private val names = (functions.keys + constants.keys + "x").sortedByDescending { it.length }

    private class ExprException(message: String) : Exception(message)

    /** Strips an optional "y =" / "f(x) =" prefix and parses the rest. */
    fun parse(text: String): ExpressionResult {
        var src = text.replace(Regex("""^\s*(?:y|f\s*\(\s*x\s*\))\s*=\s*""", RegexOption.IGNORE_CASE), "")
        src = src.replace('−', '-').replace('–', '-').replace('·', '*').replace('⋅', '*').replace('×', '*')
            .replace('÷', '/').replace("π", "pi").replace("²", "^2").replace("³", "^3")
        src = src.replace(Regex("""(\d),(\d)"""), "$1.$2").lowercase()
        if (src.isBlank()) return ExpressionResult.Error("chybí předpis funkce")
        return try {
            ExpressionResult.Ok(Parser(src).parseAll())
        } catch (e: ExprException) {
            ExpressionResult.Error(e.message ?: "chyba")
        }
    }

    private class Parser(private val src: String) {
        private var pos = 0
        private var absDepth = 0

        fun parseAll(): (Double) -> Double {
            val r = parseSum()
            skipSpaces()
            if (pos < src.length) throw ExprException("nečekaný znak „${src[pos]}“")
            return r
        }

        private fun parseSum(): (Double) -> Double {
            var left = parseProduct()
            while (true) {
                skipSpaces()
                val l = left
                left = when {
                    eat('+') -> { val r = parseProduct(); { x: Double -> l(x) + r(x) } }
                    eat('-') -> { val r = parseProduct(); { x: Double -> l(x) - r(x) } }
                    else -> return left
                }
            }
        }

        private fun parseProduct(): (Double) -> Double {
            var left = parseUnary()
            while (true) {
                skipSpaces()
                val l = left
                left = when {
                    eat('*') -> { val r = parseUnary(); { x: Double -> l(x) * r(x) } }
                    eat('/') -> { val r = parseUnary(); { x: Double -> l(x) / r(x) } }
                    startsImplicitFactor() -> { val r = parsePower(); { x: Double -> l(x) * r(x) } }
                    else -> return left
                }
            }
        }

        private fun startsImplicitFactor(): Boolean {
            if (pos >= src.length) return false
            val c = src[pos]
            return c.isDigit() || c == '.' || c.isLetter() || c == '(' || (c == '|' && absDepth == 0)
        }

        private fun parseUnary(): (Double) -> Double {
            skipSpaces()
            if (eat('-')) { val inner = parseUnary(); return { x -> -inner(x) } }
            if (eat('+')) return parseUnary()
            return parsePower()
        }

        // Right-associative and tighter than unary minus: -x^2 = -(x^2), 2^-1 = 0.5.
        private fun parsePower(): (Double) -> Double {
            val b = parsePrimary()
            skipSpaces()
            if (!eat('^')) return b
            val e = parseUnary()
            return { x -> b(x).pow(e(x)) }
        }

        private fun parsePrimary(): (Double) -> Double {
            skipSpaces()
            if (pos >= src.length) throw ExprException("výraz končí předčasně")
            val c = src[pos]
            if (c.isDigit() || c == '.') {
                val start = pos
                while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
                val number = src.substring(start, pos).toDoubleOrNull()
                    ?: throw ExprException("špatné číslo „${src.substring(start, pos)}“")
                return { number }
            }
            if (eat('(')) {
                val inner = parseSum()
                skipSpaces()
                if (!eat(')')) throw ExprException("chybí zavírací závorka )")
                return inner
            }
            if (c == '|' && eat('|')) {
                absDepth++
                val inner = parseSum()
                absDepth--
                skipSpaces()
                if (!eat('|')) throw ExprException("chybí zavírací |")
                return { x -> abs(inner(x)) }
            }
            if (c.isLetter()) {
                val name = names.firstOrNull { src.startsWith(it, pos) }
                    ?: throw ExprException("neznámý název „${readWord()}“")
                pos += name.length
                if (name == "x") return { x -> x }
                constants[name]?.let { k -> return { k } }
                // sin(x) takes just the bracket, so sin(x)^2 = (sin x)^2; without brackets the argument is the next power-level factor.
                val fn = functions.getValue(name)
                skipSpaces()
                val arg = if (pos < src.length && src[pos] == '(') parsePrimary() else parsePower()
                return { x -> fn(arg(x)) }
            }
            throw ExprException("nečekaný znak „$c“")
        }

        private fun readWord(): String {
            var end = pos
            while (end < src.length && src[end].isLetter()) end++
            return src.substring(pos, end)
        }

        private fun eat(c: Char): Boolean {
            skipSpaces()
            if (pos < src.length && src[pos] == c) { pos++; return true }
            return false
        }

        private fun skipSpaces() { while (pos < src.length && src[pos].isWhitespace()) pos++ }
    }
}

// ── Parsed graph ──────────────────────────────────────────────────────────

class GraphAxis {
    var label: String = ""
    var min: Double? = null
    var max: Double? = null
    var step: Double? = null
}

data class GraphPoint(val x: Double, val y: Double)
/** [color] is an ARGB hex string ("#rrggbb"), or null for "follow the text colour" (BLACK/WHITE). */
data class GraphSeries(val name: String?, val points: List<GraphPoint>, val smooth: Boolean, val dashed: Boolean, val showDots: Boolean, val color: String?)
data class GraphMarker(val name: String?, val x: Double, val y: Double, val color: String?)
class GraphFunction(val name: String?, val f: (Double) -> Double, val from: Double?, val to: Double?, val dashed: Boolean, val color: String?)

class Graph {
    val x = GraphAxis()
    val y = GraphAxis()
    val series = mutableListOf<GraphSeries>()
    val markers = mutableListOf<GraphMarker>()
    val functions = mutableListOf<GraphFunction>()
    val notes = mutableListOf<String>()
    val errors = mutableListOf<String>()
    var title: String? = null
    var hasAxis = false

    val isEmpty get() = !hasAxis && series.isEmpty() && markers.isEmpty() && functions.isEmpty()
}

object GraphParser {
    /** `[grafX] … [/grafX]` on their own lines; the closing tag must repeat the name. */
    val blockRegex = Regex(
        """^[ \t]*\[(graf[\w-]*)][ \t]*\r?\n(.*?)^[ \t]*\[/\1][ \t]*\r?$""",
        setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val lineRegex = Regex("""^([\p{L}_][\p{L}\p{N}_]*)\s*(?:\(([^)]*)\))?\s*:\s*(.*)$""")
    private const val NUM = """-?\d+(?:[.,]\d+)?"""
    private val rangeRegex = Regex("""^($NUM)\s*(?:-|–|\.\.)\s*($NUM)\s*(?:\(\s*(\d+(?:[.,]\d+)?)\s*\))?$""")
    private val pointRegex = Regex("""\[\s*($NUM)\s*;\s*($NUM)\s*]""")
    private val hexRegex = Regex("""^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$""")
    private val numOnly = Regex("^$NUM$")

    private const val CURRENT = "currentColor"
    private val namedColors = mapOf(
        "RED" to "#d1495b", "CERVENA" to "#d1495b",
        "DARK_RED" to "#8f1d2c", "TMAVE_CERVENA" to "#8f1d2c",
        "BLUE" to "#2f6fdf", "MODRA" to "#2f6fdf",
        "LIGHT_BLUE" to "#3fa7e0", "SVETLE_MODRA" to "#3fa7e0",
        "DARK_BLUE" to "#1d3f8f", "TMAVE_MODRA" to "#1d3f8f",
        "GREEN" to "#3a9d5d", "ZELENA" to "#3a9d5d",
        "LIGHT_GREEN" to "#7cc66e", "SVETLE_ZELENA" to "#7cc66e",
        "DARK_GREEN" to "#22663a", "TMAVE_ZELENA" to "#22663a",
        "ORANGE" to "#e8833a", "ORANZOVA" to "#e8833a",
        "YELLOW" to "#d9a400", "ZLUTA" to "#d9a400",
        "PURPLE" to "#8e5cc4", "FIALOVA" to "#8e5cc4",
        "PINK" to "#e05a9b", "RUZOVA" to "#e05a9b",
        "BROWN" to "#8a5a3b", "HNEDA" to "#8a5a3b",
        "GRAY" to "#8a8580", "GREY" to "#8a8580", "SEDA" to "#8a8580",
        "BLACK" to CURRENT, "CERNA" to CURRENT, "WHITE" to CURRENT, "BILA" to CURRENT,
    )
    private val defaultPalette = listOf("#d1495b", "#2f6fdf", "#3a9d5d", "#e8833a", "#8e5cc4", "#3fa7e0", "#d9a400", "#e05a9b")

    /** Colour string for a name/hex token; `null` = unknown token. "currentColor" is mapped to the empty marker below. */
    private fun resolveColor(token: String): String? {
        val t = token.trim()
        if (hexRegex.matches(t)) return t
        val key = normalize(t).uppercase().replace('-', '_')
        return namedColors[key]
    }

    /** Lower-case + strip diacritics so "Křivka"/"krivka" and "ČERVENÁ"/"cervena" match. */
    fun normalize(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun num(s: String) = s.replace(',', '.').toDouble()
    private fun themeColor(c: String?) = if (c == CURRENT) null else c

    fun parse(body: String): Graph {
        val g = Graph()
        var colorIndex = 0
        fun nextColor() = defaultPalette[colorIndex++ % defaultPalette.size]

        for (rawLine in body.split(Regex("\r?\n"))) {
            val line = rawLine.replace(Regex("""(^|\s)//.*$"""), "").trim()
            if (line.isEmpty()) continue
            val m = lineRegex.matchEntire(line) ?: continue
            val key = normalize(m.groupValues[1]).replace(Regex("\\d+$"), "")
            val args = m.groupValues[2].trim()
            val value = m.groupValues[3].trim()

            when (key) {
                "x" -> { parseAxis(g.x, args, value); g.hasAxis = true }
                "y" -> { parseAxis(g.y, args, value); g.hasAxis = true }
                "curve", "krivka", "line", "cara" -> {
                    val a = parseArgs(args)
                    val points = pointRegex.findAll(value).map { GraphPoint(num(it.groupValues[1]), num(it.groupValues[2])) }.toList()
                    if (points.isEmpty()) continue
                    var smooth = false; var dashed = false; var dots = true
                    var color = a.color
                    for (token in pointRegex.replace(value, " ").split(Regex("\\s+")).filter { it.isNotEmpty() }) {
                        when (normalize(token)) {
                            "linear", "linearni", "primka", "rovna" -> smooth = false
                            "smooth", "curved", "hladka", "zakrivena", "oblouk" -> smooth = true
                            "dashed", "carkovana", "prerusovana" -> dashed = true
                            "nodots", "bezbodu" -> dots = false
                            else -> if (color == null) color = resolveColor(token)
                        }
                    }
                    g.series += GraphSeries(a.name, points, smooth, dashed, dots, themeColor(color ?: nextColor()))
                }
                "point", "bod" -> {
                    val a = parseArgs(args)
                    val p = pointRegex.find(value) ?: continue
                    var color = a.color
                    for (token in pointRegex.replace(value, " ").split(Regex("\\s+")).filter { it.isNotEmpty() }) {
                        if (color == null) color = resolveColor(token)
                    }
                    g.markers += GraphMarker(a.name, num(p.groupValues[1]), num(p.groupValues[2]), themeColor(color ?: nextColor()))
                }
                "function", "funkce", "fce" -> {
                    val a = parseArgs(args)
                    val words = value.split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
                    var dashed = false
                    var color = a.color
                    while (words.size > 1) {
                        val last = words.last()
                        val option = normalize(last)
                        if (option == "dashed" || option == "carkovana" || option == "prerusovana") dashed = true
                        else if (option in setOf("linear", "linearni", "smooth", "hladka", "nodots", "bezbodu")) Unit
                        else if (resolveColor(last) != null) { if (color == null) color = resolveColor(last) }
                        else break
                        words.removeAt(words.size - 1)
                    }
                    val formula = words.joinToString(" ")
                    when (val r = GraphExpression.parse(formula)) {
                        is ExpressionResult.Error -> g.errors += "Funkci „$formula“ se nepodařilo přečíst: ${r.message}."
                        is ExpressionResult.Ok -> g.functions += GraphFunction(a.name ?: formula, r.f, a.from, a.to, dashed, themeColor(color ?: nextColor()))
                    }
                }
                "note", "poznamka", "popis" -> if (value.isNotEmpty()) g.notes += value
                "title", "nazev" -> if (value.isNotEmpty()) g.title = value
            }
        }
        return g
    }

    private fun parseAxis(axis: GraphAxis, args: String, value: String) {
        axis.label = args
        val m = rangeRegex.matchEntire(value) ?: return
        val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
        axis.min = min(a, b); axis.max = max(a, b)
        if (m.groupValues[3].isNotEmpty()) {
            val step = num(m.groupValues[3])
            if (step > 0) axis.step = step
        }
    }

    private class Args(val name: String?, val color: String?, val from: Double?, val to: Double?)

    // "(name: robot1, color: RED)", "(robot1)", "(od: 0, do: 6,28)". A comma splits only before "key:" or when followed by a space.
    private fun parseArgs(args: String): Args {
        if (args.isEmpty()) return Args(null, null, null, null)
        var name: String? = null; var color: String? = null; var from: Double? = null; var to: Double? = null
        for (part in args.split(Regex(""",(?=\s*[\p{L}_]+\s*:)|,\s+"""))) {
            val idx = part.indexOf(':')
            if (idx < 0) {
                val bare = part.trim()
                if (bare.isNotEmpty() && name == null) name = bare
                continue
            }
            val k = normalize(part.substring(0, idx))
            val v = part.substring(idx + 1).trim()
            if (v.isEmpty()) continue
            when {
                k in setOf("name", "nazev", "jmeno", "popisek", "label") -> name = v
                k in setOf("color", "colour", "barva") -> color = resolveColor(v)
                (k == "from" || k == "od") && numOnly.matches(v) -> from = num(v)
                (k == "to" || k == "do") && numOnly.matches(v) -> to = num(v)
            }
        }
        return Args(name, color, from, to)
    }
}

// ── Layout ────────────────────────────────────────────────────────────────

class GraphLayout(
    val xMin: Double, val xMax: Double, val xStep: Double,
    val yMin: Double, val yMax: Double, val yStep: Double,
    /** One list per [Graph.functions] entry; NaN y marks a gap. */
    val samples: List<List<GraphPoint>>,
)

object GraphMath {
    private const val FUNCTION_SAMPLES = 600

    fun layout(g: Graph): GraphLayout {
        val allX = (g.series.flatMap { s -> s.points.map { it.x } } + g.markers.map { it.x } +
            g.functions.flatMap { listOfNotNull(it.from, it.to) }).toMutableList()
        if (allX.isEmpty() && g.functions.isNotEmpty()) allX.addAll(listOf(-10.0, 10.0))
        var (xMin, xMax) = resolveRange(g.x, allX)
        val xStep = resolveStep(g.x.step, xMin, xMax)
        snapToStep(xMin, xMax, xStep).let { xMin = it.first; xMax = it.second }

        val samples = g.functions.map { sample(it, xMin, xMax) }
        val allY = (g.series.flatMap { s -> s.points.map { it.y } } + g.markers.map { it.y } + functionYExtent(samples)).toMutableList()
        var (yMin, yMax) = resolveRange(g.y, allY)
        val yStep = resolveStep(g.y.step, yMin, yMax)
        snapToStep(yMin, yMax, yStep).let { yMin = it.first; yMax = it.second }
        return GraphLayout(xMin, xMax, xStep, yMin, yMax, yStep, samples)
    }

    private fun sample(f: GraphFunction, xMin: Double, xMax: Double): List<GraphPoint> {
        val lo = max(xMin, f.from ?: xMin)
        val hi = min(xMax, f.to ?: xMax)
        if (hi <= lo) return emptyList()
        return (0..FUNCTION_SAMPLES).map { i ->
            val x = lo + (hi - lo) * i / FUNCTION_SAMPLES
            val y = try { f.f(x) } catch (_: Exception) { Double.NaN }
            GraphPoint(x, if (y.isFinite()) y else Double.NaN)
        }
    }

    // Ignore the outer 2 % so an asymptote doesn't squash the rest of the graph.
    private fun functionYExtent(samples: List<List<GraphPoint>>): List<Double> {
        val ys = samples.flatMap { s -> s.map { it.y } }.filter { it.isFinite() }.sorted()
        if (ys.isEmpty()) return emptyList()
        return listOf(ys[floor((ys.size - 1) * 0.02).toInt()], ys[ceil((ys.size - 1) * 0.98).toInt()])
    }

    fun resolveRange(axis: GraphAxis, data: List<Double>): Pair<Double, Double> {
        var lo: Double; var hi: Double
        val aMin = axis.min; val aMax = axis.max
        if (aMin != null && aMax != null) {
            lo = aMin; hi = aMax
        } else if (data.isNotEmpty()) {
            lo = min(0.0, data.min()); hi = max(0.0, data.max())
            val step = niceStep((hi - lo) / 8)
            lo = floor(lo / step) * step
            hi = ceil(hi / step) * step
        } else {
            lo = 0.0; hi = 10.0
        }
        if (hi - lo < 1e-9) { lo -= 1; hi += 1 }
        return lo to hi
    }

    fun resolveStep(step: Double?, lo: Double, hi: Double): Double {
        val span = hi - lo
        if (step != null && step > 0 && span / step <= 40) return step
        if (span in 2.0..20.0) return 1.0
        return niceStep(span / 8)
    }

    fun snapToStep(lo: Double, hi: Double, step: Double): Pair<Double, Double> {
        val a = floor(lo / step + 1e-9) * step
        val b = ceil(hi / step - 1e-9) * step
        return if (b - a > 1e-9) a to b else lo to hi
    }

    fun niceStep(raw: Double): Double {
        if (raw <= 0 || raw.isNaN() || raw.isInfinite()) return 1.0
        val exp = 10.0.pow(floor(log10(raw)))
        val f = raw / exp
        val nice = if (f < 1.5) 1 else if (f < 3) 2 else if (f < 7) 5 else 10
        return nice * exp
    }

    fun ticks(lo: Double, hi: Double, step: Double): List<Double> {
        val start = ceil(lo / step - 1e-9) * step
        val out = ArrayList<Double>()
        for (i in 0..100) {
            val v = start + i * step
            if (v > hi + step * 1e-9) break
            out += if (abs(v) < step * 1e-9) 0.0 else v
        }
        return out
    }

    /** Czech number formatting (decimal comma, true minus) with as many decimals as the step needs. */
    fun formatTick(v: Double, step: Double): String {
        var decimals = 0
        while (decimals < 6 && abs(step * 10.0.pow(decimals) - round(step * 10.0.pow(decimals))) > 1e-6) decimals++
        val s = java.math.BigDecimal(v).setScale(decimals, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return (if (s == "-0") "0" else s).replace('.', ',').replace("-", "−")
    }

    /** How many ticks apart labels must be so a label of [labelSize] fits between them. */
    fun labelEvery(labelSize: Double, tickSpacing: Double): Int =
        if (tickSpacing <= 0) 1 else max(1, ceil(labelSize / tickSpacing).toInt())
}
