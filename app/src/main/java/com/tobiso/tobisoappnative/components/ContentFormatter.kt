

package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.model.Post
import java.net.URLDecoder
import java.net.URLEncoder

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
    data class ImageWithMeta(val alt: String, val url: String, val caption: String?, val source: String?, val isTobiso: Boolean) : ContentElement()
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

    // Preprocess paragraphs to detect image+caption+source groups
    fun preprocessImageGroups(contentStr: String): String {
        if (contentStr.isBlank()) return contentStr
        val paras = Regex("\r?\n[ \t]*\r?\n+").split(contentStr).map { it.trim() }.toMutableList()
        val imgRx = Regex("^!\\[([^\\]]*)\\]\\(([^)]+)\\)\\s*$")
        val out = mutableListOf<String>()
        var idx = 0
        while (idx < paras.size) {
            val p = paras[idx]
            if (p.isBlank()) { idx++; continue }
            val m = imgRx.matchEntire(p)
            if (m == null) {
                out.add(p)
                idx++
                continue
            }
            // found an image paragraph
            val alt = m.groupValues[1]
            val src = m.groupValues[2]
            // look ahead up to 3 paragraphs for Zdroj/Autor
            var srcIdx = -1
            var look = 0
            var j = idx + 1
            while (j < paras.size && look < 3) {
                val cand = paras[j].trim()
                if (cand.isNotEmpty()) {
                    if (cand.startsWith("Zdroj") || cand.startsWith("Autor")) { srcIdx = j; break }
                }
                j++; look++
            }
            if (srcIdx < 0) {
                out.add(p)
                idx++
                continue
            }
            val captionParts = mutableListOf<String>()
            for (k in idx + 1 until srcIdx) {
                val part = paras[k].trim()
                if (part.isNotEmpty()) captionParts.add(part)
            }
            val captionText = if (captionParts.isEmpty()) "" else captionParts.joinToString(" ")
            val sourceText = paras[srcIdx].trim()
            val isTob = sourceText.contains("Tobiso", ignoreCase = true)

            // encode parts to be safe in marker
            val marker = listOf(
                URLEncoder.encode(alt, "UTF-8"),
                URLEncoder.encode(src, "UTF-8"),
                URLEncoder.encode(captionText, "UTF-8"),
                URLEncoder.encode(sourceText, "UTF-8"),
                isTob.toString()
            ).joinToString("::")
            out.add("IMG_META::" + marker)
            idx = srcIdx + 1
        }
        return out.joinToString("\n\n")
    }

    workingText = preprocessImageGroups(workingText)

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
        // Tabulka (řeší standardní Markdown tabulky s oddělovačem řádku s pomlčkami)
        fun splitPipeLine(ln: String): List<String> {
            var s = ln.trim()
            if (s.startsWith("|")) s = s.substring(1)
            if (s.endsWith("|")) s = s.substring(0, s.length - 1)
            return s.split("|").map { it.trim() }
        }

        fun isTableSeparatorLine(ln: String): Boolean {
            val s = ln.trim()
            if (!s.contains("|")) return false
            var t = s
            if (t.startsWith("|")) t = t.substring(1)
            if (t.endsWith("|")) t = t.substring(0, t.length - 1)
            val parts = t.split("|").map { it.trim() }
            if (parts.isEmpty()) return false
            return parts.all { part ->
                // valid separator part looks like --- or :---: with optional spaces
                part.matches(Regex("^:?-+:?$"))
            }
        }

        if (line.contains("|") && i + 1 < lines.size && isTableSeparatorLine(lines[i + 1])) {
            val header = splitPipeLine(line)
            val rows = mutableListOf<List<String>>()
            var j = i + 2
            while (j < lines.size && lines[j].contains("|")) {
                val row = splitPipeLine(lines[j])
                // pad row to header length if necessary
                val padded = if (row.size < header.size) row + List(header.size - row.size) { "" } else row
                rows.add(padded)
                j++
            }
            elements.add(ContentElement.Table(header, rows))
            i = j
            continue
        }
        // Paragraph-level image metadata marker injected by preprocessImageGroups
        if (line.startsWith("IMG_META::")) {
            val payload = line.removePrefix("IMG_META::")
            val parts = payload.split("::")
            if (parts.size >= 5) {
                val alt = URLDecoder.decode(parts[0], "UTF-8")
                val url = URLDecoder.decode(parts[1], "UTF-8")
                val captionRaw = URLDecoder.decode(parts[2], "UTF-8")
                val sourceRaw = URLDecoder.decode(parts[3], "UTF-8")
                val isTob = parts[4].toBoolean()
                val caption = if (captionRaw.isBlank()) null else captionRaw
                elements.add(ContentElement.ImageWithMeta(alt, url, caption, sourceRaw, isTob))
                i++
                continue
            }
        }

        // Obrázek ![alt](url) (fallback single-line image)
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

