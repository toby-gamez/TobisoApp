package com.tobiso.tobisoappnative.components

/**
 * Renders the common subset of the LaTeX that Tobiso authors write between `$…$` / `$$…$$` as plain
 * Unicode text (exponents, indices, fractions, roots, Greek letters, operators). A full KaTeX
 * engine would add megabytes and a WebView; this covers school-level math and degrades to readable
 * text for anything it doesn't know.
 */
private val MATH_SPAN = Regex("""\$\$(.+?)\$\$|\$(?=\S)([^$\n]+?)(?<=\S)\$""", RegexOption.DOT_MATCHES_ALL)

/** Replaces every math span in [text] with its Unicode rendering; text without `$` is returned untouched. */
fun convertMathSpans(text: String): String =
    if ('$' !in text) text
    else MATH_SPAN.replace(text) { m -> latexToUnicode((m.groups[1] ?: m.groups[2])!!.value.trim()) }

private val SUPERSCRIPTS = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '−' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ',
    'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ', 'k' to 'ᵏ', 'm' to 'ᵐ', 'p' to 'ᵖ', 't' to 'ᵗ'
)
private val SUBSCRIPTS = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '−' to '₋', '=' to '₌', '(' to '₍', ')' to '₎', 'n' to 'ₙ', 'i' to 'ᵢ', 'x' to 'ₓ',
    'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'k' to 'ₖ', 'm' to 'ₘ', 'p' to 'ₚ', 't' to 'ₜ'
)

private val SYMBOLS = mapOf(
    "cdot" to "·", "times" to "×", "div" to "÷", "pm" to "±", "mp" to "∓", "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥",
    "neq" to "≠", "ne" to "≠", "approx" to "≈", "equiv" to "≡", "sim" to "∼", "infty" to "∞", "degree" to "°", "circ" to "°",
    "rightarrow" to "→", "to" to "→", "leftarrow" to "←", "Rightarrow" to "⇒", "Leftarrow" to "⇐", "leftrightarrow" to "↔",
    "Leftrightarrow" to "⇔", "in" to "∈", "notin" to "∉", "subset" to "⊂", "subseteq" to "⊆", "cup" to "∪", "cap" to "∩",
    "emptyset" to "∅", "forall" to "∀", "exists" to "∃", "sum" to "∑", "prod" to "∏", "int" to "∫", "partial" to "∂",
    "nabla" to "∇", "angle" to "∠", "perp" to "⊥", "parallel" to "∥", "ldots" to "…", "dots" to "…", "cdots" to "⋯",
    "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ",
    "eta" to "η", "theta" to "θ", "iota" to "ι", "kappa" to "κ", "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ",
    "pi" to "π", "rho" to "ρ", "sigma" to "σ", "tau" to "τ", "upsilon" to "υ", "phi" to "φ", "varphi" to "φ", "chi" to "χ",
    "psi" to "ψ", "omega" to "ω", "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ", "Xi" to "Ξ", "Pi" to "Π",
    "Sigma" to "Σ", "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω", "neg" to "¬", "land" to "∧", "lor" to "∨",
    "," to " ", ";" to " ", ":" to " ", "!" to "", " " to " ", "%" to "%", "&" to "&", "#" to "#", "_" to "_",
    "{" to "{", "}" to "}", "\\" to " "
)

/** Commands that just wrap their argument (the argument is kept, the command is dropped). */
private val TRANSPARENT = setOf("text", "mathrm", "mathbf", "mathit", "textbf", "textit", "operatorname", "mathbb", "mathcal", "boldsymbol", "vec", "overrightarrow")
private val DROPPED = setOf("left", "right", "displaystyle", "big", "Big", "bigg", "Bigg", "limits")

fun latexToUnicode(latex: String): String = LatexParser(latex).parse().trim()

private class LatexParser(private val s: String) {
    private var i = 0

    fun parse(): String {
        val sb = StringBuilder()
        while (i < s.length) sb.append(next())
        return sb.toString()
    }

    private fun next(): String {
        val c = s[i]
        return when {
            c == '\\' -> command()
            c == '^' -> { i++; script(SUPERSCRIPTS, "^") }
            c == '_' -> { i++; script(SUBSCRIPTS, "_") }
            c == '{' -> { val g = group(); g }
            c == '}' -> { i++; "" }
            c == '~' -> { i++; " " }
            else -> { i++; c.toString() }
        }
    }

    /** Reads `{…}` (rendered) starting at the current `{`, or a single char/command when there are no braces. */
    private fun argument(): String {
        skipSpaces()
        if (i >= s.length) return ""
        return if (s[i] == '{') group() else if (s[i] == '\\') command() else s[i++].toString()
    }

    private fun group(): String {
        // current char is '{'
        var depth = 0
        val start = i
        while (i < s.length) {
            if (s[i] == '{') depth++
            if (s[i] == '}') { depth--; if (depth == 0) { i++; break } }
            i++
        }
        val inner = s.substring(start + 1, (i - 1).coerceAtLeast(start + 1))
        return LatexParser(inner).parse()
    }

    private fun skipSpaces() { while (i < s.length && s[i] == ' ') i++ }

    private fun script(map: Map<Char, Char>, marker: String): String {
        val arg = argument()
        if (arg.isEmpty()) return ""
        val mapped = arg.map { map[it] }
        return if (mapped.all { it != null }) mapped.joinToString("") else "$marker($arg)"
    }

    private fun wrapIfComposite(t: String): String =
        if (t.length > 1 && t.any { it == ' ' || it == '+' || it == '-' || it == '−' || it == '·' || it == '×' }) "($t)" else t

    private fun command(): String {
        i++ // backslash
        if (i >= s.length) return ""
        // single non-letter command: \, \; \% \\ ...
        if (!s[i].isLetter()) {
            val key = s[i++].toString()
            return SYMBOLS[key] ?: key
        }
        val start = i
        while (i < s.length && s[i].isLetter()) i++
        val name = s.substring(start, i)
        return when {
            name == "frac" || name == "dfrac" || name == "tfrac" -> {
                val a = argument(); val b = argument()
                "${wrapIfComposite(a)}/${wrapIfComposite(b)}"
            }
            name == "sqrt" -> {
                // optional index: \sqrt[3]{x}
                var index: String? = null
                if (i < s.length && s[i] == '[') {
                    val end = s.indexOf(']', i)
                    if (end > 0) { index = s.substring(i + 1, end); i = end + 1 }
                }
                val a = argument()
                val root = when (index) { null, "2" -> "√"; "3" -> "∛"; "4" -> "∜"; else -> "${index.map { SUPERSCRIPTS[it] ?: it }.joinToString("")}√" }
                root + wrapIfComposite(a)
            }
            name in TRANSPARENT -> argument()
            name in DROPPED -> { skipDelimiterSpace(); "" }
            name == "overline" || name == "bar" -> argument().map { "$it̅" }.joinToString("")
            name == "hat" -> argument() + "̂"
            else -> SYMBOLS[name] ?: name
        }
    }

    private fun skipDelimiterSpace() { /* `\left(` keeps the bracket char itself; nothing to skip */ }
}
