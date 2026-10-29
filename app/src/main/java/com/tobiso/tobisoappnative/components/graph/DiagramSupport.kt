package com.tobiso.tobisoappnative.components.graph

/** Small helpers shared by the flowchart and Venn blocks (port of the web's `DiagramSupport`). */
internal object DiagramSupport {
    /** `key(args): value` — same line shape the `[grafN]` blocks use. */
    val lineRegex = Regex("""^([\p{L}_][\p{L}\p{N}_]*)\s*(?:\(([^)]*)\))?\s*:\s*(.*)$""")

    fun normalize(s: String) = GraphParser.normalize(s)

    /** "id: a, color: RED" becomes {id=a, color=RED}. A bare token is stored under "". */
    fun parseArgs(args: String?): Map<String, String> {
        val result = HashMap<String, String>()
        if (args.isNullOrBlank()) return result
        for (part in args.split(',')) {
            val colon = part.indexOf(':')
            if (colon < 0) {
                if (part.trim().isNotEmpty()) result[""] = part.trim()
                continue
            }
            result[normalize(part.substring(0, colon))] = part.substring(colon + 1).trim()
        }
        return result
    }

    /** Resolved colour of a name/hex token: `known` = false for an unknown token, `hex` = null for "follow the text colour". */
    class ColorToken(val hex: String?, val known: Boolean)

    fun color(token: String): ColorToken {
        val resolved = GraphParser.resolveColor(token) ?: return ColorToken(null, false)
        return ColorToken(if (resolved == "currentColor") null else resolved, true)
    }

    /** Greedy word wrap on spaces; a word longer than the limit stays on its own line. */
    fun wrap(text: String, maxChars: Int): List<String> {
        val lines = ArrayList<String>()
        val current = StringBuilder()
        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxChars) {
                lines += current.toString()
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) lines += current.toString()
        if (lines.isEmpty()) lines += ""
        return lines
    }
}
