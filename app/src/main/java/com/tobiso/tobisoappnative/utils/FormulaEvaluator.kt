package com.tobiso.tobisoappnative.utils

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Evaluates the formulas of the "Vzorce" AI tool, e.g. `m * a` or `0.5 m v^2`, for named variables.
 * Same grammar as the graph expressions (`+ - * / ^`, brackets, `|x|`, implicit multiplication, decimal comma,
 * `sqrt sin cos tan ln log exp abs`, `pi`, `e`) but with caller-supplied variable names instead of just `x`.
 */
object FormulaEvaluator {

    sealed class Compiled {
        data class Ok(val evaluate: (Map<String, Double>) -> Double) : Compiled()
        data class Error(val message: String) : Compiled()
    }

    private val functions: Map<String, (Double) -> Double> = mapOf(
        "sqrt" to { v -> sqrt(v) }, "abs" to { v -> abs(v) },
        "sin" to { v -> kotlin.math.sin(v) }, "cos" to { v -> kotlin.math.cos(v) },
        "tan" to { v -> kotlin.math.tan(v) }, "tg" to { v -> kotlin.math.tan(v) },
        "ln" to { v -> kotlin.math.ln(v) }, "log" to { v -> kotlin.math.log10(v) }, "exp" to { v -> kotlin.math.exp(v) }
    )
    private val constants = mapOf("pi" to Math.PI)

    private class ExprException(message: String) : Exception(message)

    /** Parses [text] (an optional `F =` / `v =` prefix is dropped) for the given [variables]. */
    fun compile(text: String, variables: Collection<String>): Compiled {
        var src = text.substringAfter('=', text)
        src = src.replace('−', '-').replace('–', '-').replace('·', '*').replace('⋅', '*').replace('×', '*')
            .replace('÷', '/').replace("π", "pi").replace("²", "^2").replace("³", "^3")
        src = src.replace(Regex("""(\d),(\d)"""), "$1.$2")
        if (src.isBlank()) return Compiled.Error("chybí předpis vzorce")
        // Longest first so a variable like "vt" wins over "v"; case matters (m ≠ M).
        val names = (variables.filter { it.isNotBlank() } + functions.keys + constants.keys).distinct().sortedByDescending { it.length }
        return try {
            Compiled.Ok(Parser(src, names, variables.toSet()).parseAll())
        } catch (e: ExprException) {
            Compiled.Error(e.message ?: "chyba")
        }
    }

    private class Parser(private val src: String, private val names: List<String>, private val variables: Set<String>) {
        private var pos = 0
        private var absDepth = 0

        fun parseAll(): (Map<String, Double>) -> Double {
            val r = parseSum()
            skipSpaces()
            if (pos < src.length) throw ExprException("nečekaný znak „${src[pos]}“")
            return r
        }

        private fun parseSum(): (Map<String, Double>) -> Double {
            var left = parseProduct()
            while (true) {
                skipSpaces()
                val l = left
                left = when {
                    eat('+') -> { val r = parseProduct(); { v: Map<String, Double> -> l(v) + r(v) } }
                    eat('-') -> { val r = parseProduct(); { v: Map<String, Double> -> l(v) - r(v) } }
                    else -> return left
                }
            }
        }

        private fun parseProduct(): (Map<String, Double>) -> Double {
            var left = parseUnary()
            while (true) {
                skipSpaces()
                val l = left
                left = when {
                    eat('*') -> { val r = parseUnary(); { v: Map<String, Double> -> l(v) * r(v) } }
                    eat('/') -> { val r = parseUnary(); { v: Map<String, Double> -> l(v) / r(v) } }
                    startsImplicitFactor() -> { val r = parsePower(); { v: Map<String, Double> -> l(v) * r(v) } }
                    else -> return left
                }
            }
        }

        private fun startsImplicitFactor(): Boolean {
            if (pos >= src.length) return false
            val c = src[pos]
            return c.isDigit() || c == '.' || c.isLetter() || c == '(' || (c == '|' && absDepth == 0)
        }

        private fun parseUnary(): (Map<String, Double>) -> Double {
            skipSpaces()
            if (eat('-')) { val inner = parseUnary(); return { v -> -inner(v) } }
            if (eat('+')) return parseUnary()
            return parsePower()
        }

        private fun parsePower(): (Map<String, Double>) -> Double {
            val b = parsePrimary()
            skipSpaces()
            if (!eat('^')) return b
            val e = parseUnary()
            return { v -> b(v).pow(e(v)) }
        }

        private fun parsePrimary(): (Map<String, Double>) -> Double {
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
                return { v -> abs(inner(v)) }
            }
            if (c.isLetter()) {
                val name = names.firstOrNull { src.startsWith(it, pos) }
                    ?: throw ExprException("neznámý název „${readWord()}“")
                pos += name.length
                if (name in variables) return { v -> v[name] ?: Double.NaN }
                constants[name]?.let { k -> return { k } }
                val fn = functions.getValue(name)
                skipSpaces()
                val arg = if (pos < src.length && src[pos] == '(') parsePrimary() else parsePower()
                return { v -> fn(arg(v)) }
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

/** Formats a calculator value for display: up to 4 decimals, no trailing zeros, Czech decimal comma. */
fun formatFormulaValue(value: Double): String = when {
    value.isNaN() -> "–"
    value.isInfinite() -> if (value > 0) "∞" else "−∞"
    else -> java.math.BigDecimal(value).setScale(4, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString().replace('.', ',')
}
