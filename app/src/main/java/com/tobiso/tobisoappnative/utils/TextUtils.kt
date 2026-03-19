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
}