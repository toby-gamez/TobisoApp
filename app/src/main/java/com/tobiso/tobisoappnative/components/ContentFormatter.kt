

package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.model.Post

val prefixRegex = Regex("^(ml-|sl-|li-|hv-|m-|ch-|f-|pr-|z-)")

// --- Custom Markdown Parser ---



sealed class ContentElement {
    data class Intra(val text: String) : ContentElement()
    data class Heading(val parts: List<InlinePart>, val level: Int) : ContentElement()
    data class Paragraph(val parts: List<InlinePart>) : ContentElement()
        data class BulletList(val items: List<List<InlinePart>>, val level: Int) : ContentElement()
    data class NumberedList(val items: List<String>) : ContentElement()
    data class CodeBlock(val code: String) : ContentElement()
    data class BlockQuote(val text: String) : ContentElement()
    data class Table(val header: List<String>, val rows: List<List<String>>) : ContentElement()
    data class ClickableLink(val text: String, val url: String, val postId: Int?) : ContentElement()
    data class VideoPlayer(val videoUrl: String, val posterUrl: String?) : ContentElement()
    data class Image(val alt: String, val url: String) : ContentElement()
    data class AddendumReference(val addendumId: Int) : ContentElement()
    data class InlineText(val parts: List<InlinePart>) : ContentElement() // pro případné další použití
}

sealed class InlinePart {
    data class Text(val text: String) : InlinePart()
    data class Bold(val text: String) : InlinePart()
    data class Italic(val text: String) : InlinePart()
    data class BoldItalic(val text: String) : InlinePart()
    data class Link(val text: String, val url: String, val postId: Int?) : InlinePart()
    data class Addendum(val addendumId: Int) : InlinePart()
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
        var didAddBullet = false
        // Nadpisy
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headingText = line.drop(level).trim()
                elements.add(ContentElement.Heading(parseInlineParts(headingText, posts), level))
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
        // Odrážky (každý řádek začínající - , * , + je vždy odrážka, i kdyby byl sám)
        // Podpora až 4 úrovní odrážek podle počtu mezer před "- "
        val bulletLevels = listOf(
            Regex("^- "),        // level 1
            Regex("^  - "),      // level 2
            Regex("^    - "),    // level 3
            Regex("^      - ")   // level 4
        )
        for ((level, regex) in bulletLevels.withIndex()) {
            if (regex.containsMatchIn(line)) {
                val items = mutableListOf<List<InlinePart>>()
                    items.add(parseInlineParts(line.replaceFirst(regex, ""), posts))
                i++
                while (i < lines.size && regex.containsMatchIn(lines[i].trimEnd())) {
                        items.add(parseInlineParts(lines[i].trimEnd().replaceFirst(regex, ""), posts))
                    i++
                }
                elements.add(ContentElement.BulletList(items, level + 1))
                didAddBullet = true
                break
            }
        }
        // Číslované odrážky
            if (line.matches("^\\\\d+\\. ".toRegex())) {
                val items = mutableListOf(line.replace("^\\\\d+\\. ".toRegex(), ""))
            i++
                while (i < lines.size && lines[i].matches("^\\\\d+\\. ".toRegex())) {
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
        // Video <video src="..."> ... </video>
        val videoTagMatch = Regex("<video[^>]*src=\"([^\"]+)\"[^>]*>").find(line)
        if (videoTagMatch != null) {
            val videoUrl = videoTagMatch.groupValues[1]
            elements.add(ContentElement.VideoPlayer(videoUrl, null))
            // Najdi případný uzavírací tag </video> a přeskoč vše až do něj
            if (line.contains("</video>")) {
                i++ // pouze jeden řádek s <video>...</video>
            } else {
                i++
                while (i < lines.size && !lines[i].contains("</video>")) {
                    i++
                }
                if (i < lines.size && lines[i].contains("</video>")) {
                    i++ // přeskoč řádek s </video>
                }
            }
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
        // Odkaz [text](url) - nyní řešíme pouze samostatné řádky, inline odkazy řeší parseInlineParts
            val linkMatch = Regex("^(?<![!\\-\\\\d])\\[(.+?)\\]\\((.+?)\\)").find(line)
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
        if (!didAddBullet && line.isNotBlank()) {
                elements.add(ContentElement.Paragraph(parseInlineParts(line, posts)))
        }
        if (!didAddBullet) {
            i++
        }
    }
    return elements
}

// --- Inline Markdown Parser pro tučné, kurzívu a inline odkazy ---
fun parseInlineParts(text: String, posts: List<Post>? = null): List<InlinePart> {
    // Podpora **tučné**, *kurzíva*, ***tučné kurzíva***, [odkaz](url), (--DOD-číslo--)
    val result = mutableListOf<InlinePart>()
    var i = 0
    val n = text.length
    while (i < n) {
        when {
            // BoldItalic with link support
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    val inner = text.substring(i + 3, end)
                    val innerParts = parseInlineParts(inner, posts)
                    // Pokud je uvnitř pouze jeden Link, vrať Link s tučným kurzívním textem
                    if (innerParts.size == 1 && innerParts[0] is InlinePart.Link) {
                        val link = innerParts[0] as InlinePart.Link
                        result.add(InlinePart.Link(link.text, link.url, link.postId))
                    } else {
                        result.add(InlinePart.BoldItalic(inner))
                    }
                    i = end + 3
                } else {
                    result.add(InlinePart.Text("***"))
                    i += 3
                }
            }
            // Bold with link support
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val inner = text.substring(i + 2, end)
                    val innerParts = parseInlineParts(inner, posts)
                    if (innerParts.size == 1 && innerParts[0] is InlinePart.Link) {
                        val link = innerParts[0] as InlinePart.Link
                        result.add(InlinePart.Link(link.text, link.url, link.postId))
                    } else {
                        result.add(InlinePart.Bold(inner))
                    }
                    i = end + 2
                } else {
                    result.add(InlinePart.Text("**"))
                    i += 2
                }
            }
            // Italic with link support
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    val inner = text.substring(i + 1, end)
                    val innerParts = parseInlineParts(inner, posts)
                    if (innerParts.size == 1 && innerParts[0] is InlinePart.Link) {
                        val link = innerParts[0] as InlinePart.Link
                        result.add(InlinePart.Link(link.text, link.url, link.postId))
                    } else {
                        result.add(InlinePart.Italic(inner))
                    }
                    i = end + 1
                } else {
                    result.add(InlinePart.Text("*"))
                    i += 1
                }
            }
            // Inline dodatek (--DOD-číslo--)
            text.startsWith("(--DOD-", i) -> {
                val end = text.indexOf("--)", i + 8)
                if (end != -1) {
                    val idStr = text.substring(i + 7, end)
                    val id = idStr.toIntOrNull()
                    if (id != null) {
                        result.add(InlinePart.Addendum(id))
                        i = end + 3
                    } else {
                        result.add(InlinePart.Text("(--DOD-"))
                        i += 7
                    }
                } else {
                    result.add(InlinePart.Text("(--DOD-"))
                    i += 7
                }
            }
            // Inline odkaz [text](url)
            text.startsWith("[", i) -> {
                val closeBracket = text.indexOf("]", i)
                val openParen = if (closeBracket != -1) text.indexOf("(", closeBracket) else -1
                val closeParen = if (openParen != -1) text.indexOf(")", openParen) else -1
                if (closeBracket != -1 && openParen == closeBracket + 1 && closeParen != -1) {
                    val linkText = text.substring(i + 1, closeBracket)
                    val url = text.substring(openParen + 1, closeParen)
                    var postId: Int? = null
                    if (posts != null) {
                        var filePath = url
                        if (filePath.endsWith(".html")) filePath = filePath.removeSuffix(".html") + ".md"
                        filePath = filePath.replace(prefixRegex, "")
                        if (!filePath.startsWith("/")) filePath = "/$filePath"
                        val post = posts.find { it.filePath == filePath }
                        postId = post?.id
                    }
                    result.add(InlinePart.Link(linkText, url, postId))
                    i = closeParen + 1
                } else {
                    result.add(InlinePart.Text("["))
                    i += 1
                }
            }
            else -> {
                // Najdi další * nebo ** nebo *** nebo [ nebo (--DOD-
                val next = listOf(
                    text.indexOf("***", i),
                    text.indexOf("**", i),
                    text.indexOf("*", i),
                    text.indexOf("[", i),
                    text.indexOf("(--DOD-", i)
                ).filter { it >= 0 }.minOrNull() ?: n
                result.add(InlinePart.Text(text.substring(i, next)))
                i = next
            }
        }
    }
    return result
}

// Přidání nové InlinePart pro odkazy

