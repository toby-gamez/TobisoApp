

package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.model.Post

val prefixRegex = Regex("^(ml-|sl-|li-|hv-|m-|ch-|f-|pr-|z-)")

// --- Custom Markdown Parser ---


sealed class ContentElement {
    data class Intra(val text: String) : ContentElement()
    data class Heading(val text: String, val level: Int) : ContentElement()
    data class Paragraph(val text: String) : ContentElement()
    data class BulletList(val items: List<String>) : ContentElement()
    data class NumberedList(val items: List<String>) : ContentElement()
    data class CodeBlock(val code: String) : ContentElement()
    data class BlockQuote(val text: String) : ContentElement()
    data class Table(val header: List<String>, val rows: List<List<String>>) : ContentElement()
    data class ClickableLink(val text: String, val url: String, val postId: Int?) : ContentElement()
    data class VideoPlayer(val videoUrl: String, val posterUrl: String?) : ContentElement()
    data class Image(val alt: String, val url: String) : ContentElement()
    data class AddendumReference(val addendumId: Int) : ContentElement()
}



fun parseContentToElements(
    content: String,
    isOffline: Boolean,
    posts: List<Post>
): List<ContentElement> {
    val elements = mutableListOf<ContentElement>()
    val text = content.replace("\u0000", "").trim()
    if (text.isBlank()) return emptyList()

    // 1. Intra (odděleno prvními dvěma výskyty "...")
    val intraRegex = Regex("\\.\\.\\.")
    val intraMatches = intraRegex.findAll(text).toList()
    var workingText = text
    if (intraMatches.size >= 2) {
        val first = intraMatches[0].range.first
        val second = intraMatches[1].range.first
        val intraText = text.substring(first + 3, second).trim()
        elements.add(ContentElement.Intra(intraText))
        workingText = text.substring(0, first) + text.substring(second + 3)
    }

    // 2. Rozdělení na řádky/bloky
    val lines = workingText.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trimEnd()
        // Nadpisy
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headingText = line.drop(level).trim()
            elements.add(ContentElement.Heading(headingText, level))
            i++
            continue
        }
        // Kódový blok (```)
        if (line.startsWith("```") ) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].startsWith("```") ) {
                codeLines.add(lines[i])
                i++
            }
            i++ // přeskočit koncové ```
            elements.add(ContentElement.CodeBlock(codeLines.joinToString("\n")))
            continue
        }
        // Citace
        if (line.startsWith(">")) {
            val quoteLines = mutableListOf(line.removePrefix("> ").removePrefix(">"))
            i++
            while (i < lines.size && lines[i].startsWith(">")) {
                quoteLines.add(lines[i].removePrefix("> ").removePrefix(">"))
                i++
            }
            elements.add(ContentElement.BlockQuote(quoteLines.joinToString("\n")))
            continue
        }
        // Odrážky
        if (line.matches("^[-*+] ".toRegex())) {
            val items = mutableListOf(line.drop(2))
            i++
            while (i < lines.size && lines[i].matches("^[-*+] ".toRegex())) {
                items.add(lines[i].drop(2))
                i++
            }
            elements.add(ContentElement.BulletList(items))
            continue
        }
        // Číslované odrážky
        if (line.matches("^\\d+\\. ".toRegex())) {
            val items = mutableListOf(line.replace("^\\d+\\. ".toRegex(), ""))
            i++
            while (i < lines.size && lines[i].matches("^\\d+\\. ".toRegex())) {
                items.add(lines[i].replace("^\\d+\\. ".toRegex(), ""))
                i++
            }
            elements.add(ContentElement.NumberedList(items))
            continue
        }
        // Tabulka (řádek s | a další řádky s |)
        if (line.contains("|") && i + 1 < lines.size && lines[i + 1].contains("|")) {
            val header = line.split("|").map { it.trim() }
            val rows = mutableListOf<List<String>>()
            i++
            while (i < lines.size && lines[i].contains("|")) {
                rows.add(lines[i].split("|").map { it.trim() })
                i++
            }
            elements.add(ContentElement.Table(header, rows))
            continue
        }
        // Obrázek ![alt](url)
        val imageMatch = Regex("!\\[(.*?)]\\((.*?)\\)").find(line)
        if (imageMatch != null) {
            val alt = imageMatch.groupValues[1]
            val url = imageMatch.groupValues[2]
            elements.add(ContentElement.Image(alt, url))
            i++
            continue
        }
        // Video <video src="...">
        val videoMatch = Regex("<video[^>]*src=\"([^\"]+)\"[^>]*>").find(line)
        if (videoMatch != null) {
            val videoUrl = videoMatch.groupValues[1]
            elements.add(ContentElement.VideoPlayer(videoUrl, null))
            i++
            continue
        }
        // Dodatek (--DOD-číslo--)
        val addendumMatch = Regex("\\(--DOD-(\\d+)--\\)").find(line)
        if (addendumMatch != null) {
            val addendumId = addendumMatch.groupValues[1].toIntOrNull()
            if (addendumId != null) elements.add(ContentElement.AddendumReference(addendumId))
            i++
            continue
        }
        // Odkaz [text](url)
        val linkMatch = Regex("(?<!!)\\[(.+?)\\]\\((.+?)\\)").find(line)
        if (linkMatch != null) {
            val linkText = linkMatch.groupValues[1]
            val url = linkMatch.groupValues[2]
            var filePath = url
            if (filePath.endsWith(".html")) filePath = filePath.removeSuffix(".html") + ".md"
            filePath = filePath.replace(prefixRegex, "")
            if (!filePath.startsWith("/")) filePath = "/$filePath"
            val post = posts.find { it.filePath == filePath }
            elements.add(ContentElement.ClickableLink(linkText, url, post?.id))
            i++
            continue
        }
        // Odstavec
        if (line.isNotBlank()) {
            elements.add(ContentElement.Paragraph(line))
        }
        i++
    }
    return elements
}

