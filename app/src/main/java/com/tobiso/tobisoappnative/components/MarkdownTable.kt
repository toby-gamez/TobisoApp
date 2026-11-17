package com.tobiso.tobisoappnative.components

/** A markdown pipe table split into [rows] (header first, separator row dropped) and any [outside] text. */
data class MarkdownTableParts(val rows: List<List<String>>, val outside: String)

private val separatorRow = Regex("""^\|?[\s:|-]+\|?$""")

fun parseMarkdownTable(markdown: String): MarkdownTableParts {
    val lines = markdown.replace("\r\n", "\n").lines()
    val isTableLine = { l: String -> l.trim().startsWith("|") }
    val rows = lines.filter(isTableLine)
        .filterNot { separatorRow.matches(it.trim()) }
        .map { l -> l.trim().trim('|').split("|").map { it.trim() } }
    val outside = lines.filterNot(isTableLine).joinToString("\n").trim()
    return MarkdownTableParts(rows, outside)
}
