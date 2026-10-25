package com.tobiso.tobisoappnative.utils

object TextUtils {
    private val addendumRegex = Regex("\\(--DOD-(\\d+)--\\)")

    /**
     * Extrahuje čistý text z markdown obsahu pro TTS
     */
    fun extractPlainTextForTts(markdownContent: String): String {
        return stripImageGroups(
            markdownContent
                // Grafy [grafN]…[/grafN] a samostatné řádky s videem/dokumentem se nečtou
                .replace(com.tobiso.tobisoappnative.components.graph.GraphParser.blockRegex, "")
                .replace(Regex("(?m)^[*_]\\[[^\\]]*]\\([^)]*\\)\\s*$"), "")
        )
            // Odstranění obrázků
            .replace(Regex("!\\[.*?](?:\\(.*?\\))?"), "")
            // Nahrazení odkazů jen textem
            .replace(Regex("\\[([^]]+)](?:\\(.*?\\))?"), "$1")
            // Odstranění HTML tagů
            .replace(Regex("<[^>]+>"), "")
            // Odstranění video tagů
            .replace(Regex("<video[^>]*>(.*?)</video>", RegexOption.DOT_MATCHES_ALL), "")
            // Odstranění tučného textu (ponechání obsahu)
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            // Odstranění kurzívy (ponechání obsahu)
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            // Odstranění nadpisů
            .replace(Regex("#{1,6}\\s*"), "")
            // Odstranění kódových bloků
            .replace(Regex("```[\\s\\S]*?```"), "")
            // Odstranění inline kódu (ponechání obsahu)
            .replace(Regex("`([^`]+)`"), "$1")
            // Extrakce zvýrazněných bloků (obsah mezi ...)
            .replace(Regex("\\.{3}\\s*([\\s\\S]*?)\\s*\\.{3}"), "$1")
            // Odstranění horizontálních čar
            .replace(Regex("^-{3,}$", RegexOption.MULTILINE), "")
            // Normalizace řádkových zlomů
            .replace(addendumRegex, "")
            .replace(Regex("\\n\\s*\\n"), "\n\n")
            // Normalizace mezer
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Odstraní obrázky spolu s popisem a zdrojem (odstavce "Zdroj…"/"Autor…" za obrázkem)
     * a samostatné odstavce se zdrojem.
     */
    private fun stripImageGroups(content: String): String {
        val imgRx = Regex("^!\\[[^\\]]*]\\([^)]+\\)\\s*$")
        fun isSource(p: String) = p.startsWith("Zdroj") || p.startsWith("Autor")
        val paras = Regex("\r?\n[ \t]*\r?\n+").split(content).map { it.trim() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < paras.size) {
            val p = paras[i]
            if (imgRx.matches(p)) {
                var j = i + 1
                var look = 0
                var srcIdx = -1
                while (j < paras.size && look < 3) {
                    if (paras[j].isNotEmpty() && isSource(paras[j])) { srcIdx = j; break }
                    j++; look++
                }
                i = if (srcIdx >= 0) srcIdx + 1 else i + 1
                continue
            }
            if (!isSource(p)) out.add(p)
            i++
        }
        return out.joinToString("\n\n")
    }

    /**
     * Rozdělí dlouhý text na kratší segmenty vhodné pro TTS
     */
    fun splitTextForTts(text: String, maxLength: Int = 4000): List<String> {
        if (text.length <= maxLength) {
            return listOf(text)
        }
        
        val segments = mutableListOf<String>()
        var remaining = text
        
        while (remaining.length > maxLength) {
            var splitIndex = maxLength
            
            // Hledáme vhodné místo k rozdělení
            val sentenceEnd = remaining.lastIndexOf('.', maxLength)
            val paragraphEnd = remaining.lastIndexOf('\n', maxLength)
            val exclamationEnd = remaining.lastIndexOf('!', maxLength)
            val questionEnd = remaining.lastIndexOf('?', maxLength)
            
            // Použijeme nejlepší místo k rozdělení
            val bestEnd = listOf(sentenceEnd, exclamationEnd, questionEnd)
                .filter { it > maxLength / 2 }
                .maxOrNull()
            
            when {
                bestEnd != null && bestEnd > maxLength / 2 -> splitIndex = bestEnd + 1
                paragraphEnd > maxLength / 2 -> splitIndex = paragraphEnd + 1
                else -> {
                    // Hledáme alespoň mezeru
                    val spaceIndex = remaining.lastIndexOf(' ', maxLength)
                    if (spaceIndex > maxLength / 2) {
                        splitIndex = spaceIndex + 1
                    }
                }
            }
            
            segments.add(remaining.substring(0, splitIndex).trim())
            remaining = remaining.substring(splitIndex).trim()
        }
        
        if (remaining.isNotEmpty()) {
            segments.add(remaining)
        }
        
        return segments
    }

    /**
     * Convert markdown content to a short plain-text snippet suitable for search results.
     * Rules:
     * - Remove lines that are only dots ("..." or longer)
     * - Remove heading lines starting with '#'
     * - Remove images (markdown and HTML)
     * - Convert markdown links to plain text (keep the link text)
     * - Remove code blocks and inline code markers
     * - Normalize whitespace and truncate to maxLength (prefer breaking at word boundary)
     */
    fun markdownToSearchSnippet(markdownContent: String, query: String? = null, maxLength: Int = 120): String {
        if (markdownContent.isBlank()) return ""

        var s = markdownContent

        // Remove fenced code blocks
        s = s.replace(Regex("```[\\s\\S]*?```"), "")

        // Remove HTML <img> and <video> tags and any other HTML tags
        s = s.replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "")
        // Use inline flags (?is) to enable DOT_MATCHES_ALL and IGNORE_CASE together
        s = s.replace(Regex("(?is)<video[^>]*>(.*?)</video>"), "")
        s = s.replace(Regex("<[^>]+>"), "")

        // Remove markdown images ![alt](url)
        s = s.replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")

        // Remove addendum markers like (--DOD-123--)
        s = s.replace(addendumRegex, "")

        // Remove heading lines completely
        s = s.replace(Regex("(?m)^[\\t ]*#{1,6}.*$"), "")

        // Remove lines that contain only dots (e.g., "..." or "…..")
        s = s.replace(Regex("(?m)^[\\t ]*\\.{3,}\\s*$"), "")

        // Convert markdown links [text](url) -> text
        s = s.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")

        // Remove bold/italic markers but keep content
        s = s.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        s = s.replace(Regex("\\*([^*]+)\\*"), "$1")
        s = s.replace(Regex("__([^_]+)__"), "$1")
        s = s.replace(Regex("_([^_]+)_"), "$1")

        // Remove inline code markers
        s = s.replace(Regex("`([^`]+)`"), "$1")

        // Remove horizontal rules
        s = s.replace(Regex("(?m)^-{3,}$"), "")

        // Normalize blank lines and whitespace
        s = s.replace(Regex("\\r\\n|\\r"), "\n")
        s = s.replace(Regex("\\n\\s*\\n+"), "\n\n")
        s = s.replace(Regex("\\s+"), " ")
        s = s.trim()

        // If a query is provided, try to center the snippet around its first occurrence.
        if (!query.isNullOrBlank()) {
            val normQuery = normalizeText(query)

            // Build normalized string with mapping from normalized index -> original index
            val normBuilder = StringBuilder()
            val normIndexToOriginal = mutableListOf<Int>()
            for (i in s.indices) {
                val nc = normalizeText(s[i].toString())
                if (nc.isEmpty()) continue
                for (ch in nc) {
                    normBuilder.append(ch)
                    normIndexToOriginal.add(i)
                }
            }
            val normS = normBuilder.toString()
            val found = normS.indexOf(normQuery)
            if (found >= 0) {
                val origIndex = normIndexToOriginal.getOrNull(found) ?: 0

                // Center snippet around origIndex
                val half = maxLength / 2
                var start = (origIndex - half).coerceAtLeast(0)
                // Try to move start to nearest space after start but before origIndex for nicer cut
                val spaceBefore = s.lastIndexOf(' ', origIndex)
                if (spaceBefore in (start + 1) until origIndex) start = spaceBefore + 1

                var end = (start + maxLength).coerceAtMost(s.length)
                // Try to extend end to next space for nicer cut
                val spaceAfter = s.indexOf(' ', origIndex + normQuery.length)
                if (spaceAfter in origIndex until s.length && spaceAfter < start + maxLength) end = spaceAfter

                var snippet = s.substring(start, end).trim()
                if (start > 0) snippet = "..." + snippet
                if (end < s.length) snippet = snippet.trimEnd() + "..."
                return snippet
            }
        }

        if (s.length <= maxLength) return s

        // Truncate smartly at last space before maxLength
        val cut = s.substring(0, maxLength)
        val lastSpace = cut.lastIndexOf(' ')
        val snippet = if (lastSpace > maxLength / 2) cut.substring(0, lastSpace) else cut
        return snippet.trimEnd() + "..."
    }
}