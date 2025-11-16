package com.example.tobisoappnative.utils

object TextUtils {
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