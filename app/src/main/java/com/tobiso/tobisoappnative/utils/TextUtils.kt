package com.tobiso.tobisoappnative.utils

object TextUtils {
    /**
     * Předzpracuje Markdown pro zobrazení:
     * - "->" na šipku "→"
     *
     * Zlomky se nevykreslují jako HTML (CommonMark renderer inline HTML ignoruje).
     * Pro „skutečné“ zlomky používáme vlastní inline rendering v `SafeMarkdown`.
     *
     * Neprovádí změny uvnitř code bloků (```...```) ani inline kódu (`...`).
     */
    fun preprocessMarkdownForDisplay(markdownContent: String): String {
        if (markdownContent.isBlank()) return markdownContent

        fun transformTextSegment(segment: String): String {
            return segment.replace("->", "→")
        }

        fun transformInlineCode(line: String): String {
            if (!line.contains('`')) return transformTextSegment(line)

            val out = StringBuilder()
            val chunk = StringBuilder()
            var inInlineCode = false
            for (ch in line) {
                if (ch == '`') {
                    if (!inInlineCode) {
                        out.append(transformTextSegment(chunk.toString()))
                        chunk.setLength(0)
                        inInlineCode = true
                        out.append('`')
                    } else {
                        out.append(chunk.toString())
                        chunk.setLength(0)
                        inInlineCode = false
                        out.append('`')
                    }
                } else {
                    chunk.append(ch)
                }
            }
            if (chunk.isNotEmpty()) {
                if (inInlineCode) out.append(chunk.toString()) else out.append(transformTextSegment(chunk.toString()))
            }
            return out.toString()
        }

        val lines = markdownContent.split('\n')
        val sb = StringBuilder(markdownContent.length)
        var inFence = false
        for (index in lines.indices) {
            val line = lines[index]
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                inFence = !inFence
                sb.append(line)
            } else {
                sb.append(if (inFence) line else transformInlineCode(line))
            }
            if (index != lines.lastIndex) sb.append('\n')
        }
        return sb.toString()
    }

    /**
     * Extrahuje čistý text z markdown obsahu pro TTS
     */
    fun extractPlainTextForTts(markdownContent: String): String {
        return markdownContent
            // Odstranění obrázků
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
            // Nahrazení odkazů jen textem
            .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
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
            .replace(Regex("\\n\\s*\\n"), "\n\n")
            // Normalizace mezer
            .replace(Regex("\\s+"), " ")
            .trim()
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
        s = s.replace(Regex("!\\[[^\\]]*\\]\\([^\\)]*\\)"), "")

        // Remove heading lines completely
        s = s.replace(Regex("(?m)^[\\t ]*#{1,6}.*$"), "")

        // Remove lines that contain only dots (e.g., "..." or "…..")
        s = s.replace(Regex("(?m)^[\\t ]*\\.{3,}\\s*$"), "")

        // Convert markdown links [text](url) -> text
        s = s.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

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