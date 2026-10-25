

package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.components.graph.GraphParser
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
    /** `!!!tip … !!!` / `!!!sidefact … !!!` box (same syntax as the web); [children] are parsed like normal content. */
    data class Callout(val kind: CalloutKind, val children: List<ContentElement>) : ContentElement()
    /** `[grafN] … [/grafN]` block; [source] is the raw body, parsed by `GraphParser` when rendered. */
    data class Graph(val source: String) : ContentElement()
    /** `_[label](documents/x.docx)` download link. */
    data class Document(val label: String, val url: String) : ContentElement()
    /** `| title :: description |` lines: a vertical stack ([isTree] = false) or, when indented, a tree. */
    data class BoxDiagram(val roots: List<BoxNode>, val isTree: Boolean) : ContentElement()
}

data class BoxNode(val title: String, val description: String?, val children: List<BoxNode> = emptyList())

enum class CalloutKind(val label: String) {
    Tip("Tip"),
    SideFact("Zajímavost")
}

private const val GRAPH_MARKER = "GRAPH_META::"
private val BOX_ROW_REGEX = Regex("""^\s*\|(.*)\|\s*$""")
private val BOX_SEPARATOR_REGEX = Regex("""^[|\s:\-]+$""")
private val VIDEO_LINE_REGEX = Regex("""^\*\[([^\]]*)]\(([^)]+)\)\s*$""")
private val DOC_LINE_REGEX = Regex("""^_\[([^\]]*)]\(([^)]+)\)\s*$""")
private const val FILES_BASE = "https://files.tobiso.com/"

private fun resolveFileUrl(url: String) = if (url.startsWith("http")) url else FILES_BASE + url.removePrefix("/")

/** Swaps every graph block outside ``` fences for a `GRAPH_META::<urlencoded source>` paragraph. */
private fun replaceGraphBlocks(text: String): String {
    val fences = Regex("```").findAll(text).map { it.range.first }.toList()
    return GraphParser.blockRegex.replace(text) { m ->
        val insideFence = fences.count { it < m.range.first } % 2 == 1
        if (insideFence) m.value
        else "\n\n" + GRAPH_MARKER + URLEncoder.encode(m.groupValues[2], "UTF-8").replace(".", "%2E") + "\n\n"
    }
}

private fun parseBoxRow(line: String): Pair<Int, BoxNode>? {
    val inner = BOX_ROW_REGEX.matchEntire(line)?.groupValues?.get(1)?.trim() ?: return null
    var t = inner
    if (t.startsWith(",")) t = t.substring(1).trimStart()
    if (t.endsWith(",")) t = t.dropLast(1).trimEnd()
    if (t.isEmpty()) return null
    val idx = t.indexOf("::")
    val title = (if (idx >= 0) t.substring(0, idx) else t).trim()
    val desc = if (idx >= 0) t.substring(idx + 2).trim().ifEmpty { null } else null
    var indent = 0
    for (c in line) { if (c == ' ') indent++ else if (c == '\t') indent += 4 else break }
    return indent to BoxNode(title, desc)
}

/** Builds roots from (indent, node) rows; each row hangs under the closest less-indented row above it. */
private fun buildBoxTree(rows: List<Pair<Int, BoxNode>>): List<BoxNode> {
    class Mut(val node: BoxNode, val kids: MutableList<Mut> = mutableListOf())
    val roots = mutableListOf<Mut>()
    val stack = ArrayDeque<Pair<Int, Mut>>()
    for ((indent, node) in rows) {
        while (stack.isNotEmpty() && stack.last().first >= indent) stack.removeLast()
        val m = Mut(node)
        (stack.lastOrNull()?.second?.kids ?: roots).add(m)
        stack.addLast(indent to m)
    }
    fun freeze(m: Mut): BoxNode = m.node.copy(children = m.kids.map(::freeze))
    return roots.map(::freeze)
}

private val CALLOUT_REGEX = Regex("""!!!(tip|sidefact)\s+(.*?)\s*!!!""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

sealed class InlinePart {
    data class Text(val text: String) : InlinePart()
    data class Bold(val text: String) : InlinePart()
    data class Italic(val text: String) : InlinePart()
    data class BoldItalic(val text: String) : InlinePart()
    data class Link(val text: String, val url: String, val postId: Int?) : InlinePart()
    data class Addendum(val addendumId: Int) : InlinePart()
    data class Strikethrough(val parts: List<InlinePart>) : InlinePart()
    data class Subscript(val text: String) : InlinePart()
    data class Superscript(val text: String) : InlinePart()
    data class Code(val text: String) : InlinePart()
    data class Fraction(val numerator: List<InlinePart>, val denominator: List<InlinePart>) : InlinePart()
    // name = text as it appears in content (may be inflected); canonicalName = lookup key for API
    data class PersonMention(val name: String, val canonicalName: String = name) : InlinePart()
}

data class TocEntry(val text: String, val level: Int, val elementIndex: Int)

fun inlinePartsToText(parts: List<InlinePart>): String =
    parts.joinToString("") { part ->
        when (part) {
            is InlinePart.Text -> part.text
            is InlinePart.Bold -> part.text
            is InlinePart.Italic -> part.text
            is InlinePart.BoldItalic -> part.text
            is InlinePart.Link -> part.text
            is InlinePart.PersonMention -> part.name
            is InlinePart.Strikethrough -> inlinePartsToText(part.parts)
            is InlinePart.Subscript -> part.text
            is InlinePart.Superscript -> part.text
            is InlinePart.Code -> part.text
            is InlinePart.Fraction -> inlinePartsToText(part.numerator) + "/" + inlinePartsToText(part.denominator)
            is InlinePart.Addendum -> ""
        }
    }

fun extractToc(elements: List<ContentElement>): List<TocEntry> =
    elements.mapIndexedNotNull { index, el ->
        if (el is ContentElement.Heading)
            TocEntry(inlinePartsToText(el.parts), el.level, index)
        else null
    }




fun parseContentToElements(
    content: String,
    isOffline: Boolean,
    posts: List<Post>,
    allowIntra: Boolean = true
): List<ContentElement> {
    val elements = mutableListOf<ContentElement>()
    // Invisible fillers (Hangul fillers U+3164/U+FFA0/U+115F/U+1160, zero-width chars) are removed entirely, so they can't split e.g. fractions.
    val text = content.replace("\u0000", "")
        .replace(Regex("[\u3164\uFFA0\u115F\u1160\u200B\u2060\uFEFF]"), "")
        .trim()
    if (text.isBlank()) return emptyList()

    // Graph blocks first (like the web): park each behind a one-line marker so no other rule touches its source.
    if (text.contains("[graf", ignoreCase = true)) {
        val replaced = replaceGraphBlocks(text)
        if (replaced != text) return parseContentToElements(replaced, isOffline, posts, allowIntra)
    }

    // 1. Intra (odděleno prvními dvěma výskyty "...")
    val intraRegex = Regex("\\.\\.\\.")
    val intraMatches = intraRegex.findAll(text).toList()
    var workingText = text
    if (allowIntra && intraMatches.size >= 2) {
        val first = intraMatches[0].range.first
        val second = intraMatches[1].range.first
        val intraText = text.substring(first + 3, second).trim()
        elements.add(ContentElement.Intra(intraText))
        workingText = text.substring(0, first) + text.substring(second + 3)
    }

    // Callout boxes: split the text around them and parse each side (and the box content) on its own.
    if (CALLOUT_REGEX.containsMatchIn(workingText)) {
        var last = 0
        for (m in CALLOUT_REGEX.findAll(workingText)) {
            val before = workingText.substring(last, m.range.first)
            if (before.isNotBlank()) elements.addAll(parseContentToElements(before, isOffline, posts, allowIntra = false))
            val kind = if (m.groupValues[1].equals("tip", ignoreCase = true)) CalloutKind.Tip else CalloutKind.SideFact
            elements.add(ContentElement.Callout(kind, parseContentToElements(m.groupValues[2], isOffline, posts, allowIntra = false)))
            last = m.range.last + 1
        }
        val after = workingText.substring(last)
        if (after.isNotBlank()) elements.addAll(parseContentToElements(after, isOffline, posts, allowIntra = false))
        return elements
    }

    // Preprocess paragraphs to detect image+caption+source groups
    fun preprocessImageGroups(contentStr: String): String {
        if (contentStr.isBlank()) return contentStr
        val paras = Regex("\r?\n[ \t]*\r?\n+").split(contentStr)
            .map { if (it.trimStart().startsWith("|")) it.trim('\n', '\r').trimEnd() else it.trim() }.toMutableList()
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
        if (line.startsWith(GRAPH_MARKER)) {
            elements.add(ContentElement.Graph(URLDecoder.decode(line.removePrefix(GRAPH_MARKER), "UTF-8")))
            i++
            continue
        }
        // Video / dokument na samostatném řádku: *[alt](videos/x.mp4), _[název](documents/x.docx)
        val videoMatch = VIDEO_LINE_REGEX.matchEntire(line.trim())
        if (videoMatch != null) {
            elements.add(ContentElement.VideoPlayer(resolveFileUrl(videoMatch.groupValues[2]), null))
            i++
            continue
        }
        val docMatch = DOC_LINE_REGEX.matchEntire(line.trim())
        if (docMatch != null) {
            elements.add(ContentElement.Document(docMatch.groupValues[1], resolveFileUrl(docMatch.groupValues[2])))
            i++
            continue
        }
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
        // Odrážky (-, * nebo +; úroveň podle odsazení po 2 mezerách, max. 4 úrovně)
        val bulletRegex = Regex("^( *)[-*+] ")
        val bulletMatch = bulletRegex.find(line)
        if (bulletMatch != null) {
            fun levelOf(ln: String): Int? = bulletRegex.find(ln)?.let { minOf(it.groupValues[1].length / 2, 3) + 1 }
            val level = levelOf(line)!!
            val items = mutableListOf<List<InlinePart>>()
            items.add(parseInlineParts(line.replaceFirst(bulletRegex, ""), posts))
            i++
            while (i < lines.size && levelOf(lines[i].trimEnd()) == level) {
                items.add(parseInlineParts(lines[i].trimEnd().replaceFirst(bulletRegex, ""), posts))
                i++
            }
            elements.add(ContentElement.BulletList(items, level))
            continue
        }
        // Číslované odrážky
        val numberedRegex = Regex("^\\d+[.)] ")
        if (numberedRegex.containsMatchIn(line)) {
            val items = mutableListOf(line.replaceFirst(numberedRegex, ""))
            i++
            while (i < lines.size && numberedRegex.containsMatchIn(lines[i].trimEnd())) {
                items.add(lines[i].trimEnd().replaceFirst(numberedRegex, ""))
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
        // Box diagram: consecutive "| title :: description |" lines without a table separator row
        if (BOX_ROW_REGEX.matches(line)) {
            var j = i
            val block = mutableListOf<String>()
            while (j < lines.size && BOX_ROW_REGEX.matches(lines[j].trimEnd())) { block.add(lines[j].trimEnd()); j++ }
            val looksLikeTable = block.any { BOX_SEPARATOR_REGEX.matches(it.trim()) && it.contains('-') }
            val rows = if (looksLikeTable) emptyList() else block.mapNotNull { parseBoxRow(it) }
            if (rows.isNotEmpty()) {
                val isTree = rows.map { it.first }.distinct().size > 1
                val roots = if (isTree) buildBoxTree(rows) else rows.map { it.second }
                elements.add(ContentElement.BoxDiagram(roots, isTree))
                i = j
                continue
            }
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
fun parseInlineParts(rawText: String, posts: List<Post>? = null): List<InlinePart> {
    val text = convertMathSpans(rawText).replace("->", "→")
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
            // `inline code`
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end > i + 1) {
                    result.addAll(parseCodeSpan(text.substring(i + 1, end), posts))
                    i = end + 1
                } else {
                    result.add(InlinePart.Text("`"))
                    i += 1
                }
            }
            // <sub>...</sub> / <sup>...</sup>
            text.startsWith("<sub>", i) || text.startsWith("<sup>", i) -> {
                val isSub = text.startsWith("<sub>", i)
                val closeTag = if (isSub) "</sub>" else "</sup>"
                val end = text.indexOf(closeTag, i + 5)
                if (end != -1) {
                    val inner = text.substring(i + 5, end)
                    result.add(if (isSub) InlinePart.Subscript(inner) else InlinePart.Superscript(inner))
                    i = end + closeTag.length
                } else {
                    result.add(InlinePart.Text("<"))
                    i += 1
                }
            }
            // Strikethrough #s#...#s#
            text.startsWith("#s#", i) -> {
                val end = text.indexOf("#s#", i + 3)
                if (end != -1) {
                    result.add(InlinePart.Strikethrough(parseInlineParts(text.substring(i + 3, end), posts)))
                    i = end + 3
                } else {
                    result.add(InlinePart.Text("#s#"))
                    i += 3
                }
            }
            // Fraction — supports {num}/{den}, {num}/token, token/{den}, token/token
            text.startsWith("{", i) -> {
                if (i > 0 && text[i - 1] == '/') {
                    // token/{den} — numerator is the trailing token of the last Text part
                    val closingIdx = text.indexOf("}", i + 1)
                    if (closingIdx != -1) {
                        val numStr = run {
                            val lastText = result.lastOrNull() as? InlinePart.Text ?: return@run ""
                            val beforeSlash = lastText.text.dropLast(1)
                            val spaceIdx = beforeSlash.indexOfLast { it.isWhitespace() }
                            val num = if (spaceIdx == -1) beforeSlash else beforeSlash.substring(spaceIdx + 1)
                            val prefix = if (spaceIdx == -1) "" else beforeSlash.substring(0, spaceIdx + 1)
                            result.removeLast()
                            if (prefix.isNotEmpty()) result.add(InlinePart.Text(prefix))
                            num
                        }
                        result.add(InlinePart.Fraction(
                            numerator = parseInlineParts(numStr, posts),
                            denominator = parseInlineParts(text.substring(i + 1, closingIdx), posts)
                        ))
                        i = closingIdx + 1
                    } else {
                        result.add(InlinePart.Text("{"))
                        i += 1
                    }
                } else {
                    // Find the closing brace of THIS { (not greedily searching for }/{)
                    val closingBrace = text.indexOf("}", i + 1)
                    if (closingBrace != -1) {
                        val afterBrace = closingBrace + 1
                        when {
                            // {num}/{den}
                            afterBrace + 1 < n && text[afterBrace] == '/' && text[afterBrace + 1] == '{' -> {
                                val denClosing = text.indexOf("}", afterBrace + 2)
                                if (denClosing != -1) {
                                    result.add(InlinePart.Fraction(
                                        numerator = parseInlineParts(text.substring(i + 1, closingBrace), posts),
                                        denominator = parseInlineParts(text.substring(afterBrace + 2, denClosing), posts)
                                    ))
                                    i = denClosing + 1
                                } else {
                                    result.add(InlinePart.Text("{"))
                                    i += 1
                                }
                            }
                            // {num}/token
                            afterBrace < n && text[afterBrace] == '/' &&
                            afterBrace + 1 < n && text[afterBrace + 1] != '{' && !text[afterBrace + 1].isWhitespace() -> {
                                val denStart = afterBrace + 1
                                val denEnd = (denStart until n).firstOrNull { text[it].isWhitespace() } ?: n
                                result.add(InlinePart.Fraction(
                                    numerator = parseInlineParts(text.substring(i + 1, closingBrace), posts),
                                    denominator = parseInlineParts(text.substring(denStart, denEnd), posts)
                                ))
                                i = denEnd
                            }
                            else -> {
                                result.add(InlinePart.Text("{"))
                                i += 1
                            }
                        }
                    } else {
                        result.add(InlinePart.Text("{"))
                        i += 1
                    }
                }
            }
            else -> {
                val next = listOf(
                    text.indexOf("***", i),
                    text.indexOf("**", i),
                    text.indexOf("*", i),
                    text.indexOf("[", i),
                    text.indexOf("(--DOD-", i),
                    text.indexOf("#s#", i),
                    text.indexOf("<sub>", i),
                    text.indexOf("<sup>", i),
                    text.indexOf("{", i),
                    text.indexOf("`", i)
                ).filter { it >= 0 }.minOrNull() ?: n
                val chunk = text.substring(i, next)
                // Detect plain numeric fractions (e.g. 2/2) within text runs. Words like
                // "proč/kde" are not fractions — explicit fractions use {num}/{den}.
                var last = 0
                var ci = 0
                while (ci < chunk.length) {
                    if (chunk[ci] == '/') {
                        val numStart = (ci - 1 downTo last).firstOrNull { !chunk[it].isLetterOrDigit() }?.let { it + 1 } ?: last
                        val numToken = chunk.substring(numStart, ci)
                        val denEnd = (ci + 1 until chunk.length).firstOrNull { !chunk[it].isLetterOrDigit() } ?: chunk.length
                        val denToken = chunk.substring(ci + 1, denEnd)
                        if (numToken.isNotEmpty() && denToken.isNotEmpty() &&
                            numToken.all { it.isDigit() || it == '.' } &&
                            denToken.all { it.isDigit() || it == '.' }
                        ) {
                            if (numStart > last) result.add(InlinePart.Text(chunk.substring(last, numStart)))
                            result.add(InlinePart.Fraction(
                                numerator = listOf(InlinePart.Text(numToken)),
                                denominator = listOf(InlinePart.Text(denToken))
                            ))
                            last = denEnd
                            ci = denEnd
                        } else {
                            ci++
                        }
                    } else {
                        ci++
                    }
                }
                if (last < chunk.length) result.add(InlinePart.Text(chunk.substring(last)))
                i = next
            }
        }
    }
    return result
}

/**
 * Content of a `code span`. Stays monospace, but fractions (1/2, {a+b}/{c}) inside it are still parsed into
 * [InlinePart.Fraction]s. Spans containing `*` skip this so code like `a*b*c` isn't turned into italics.
 */
private fun parseCodeSpan(inner: String, posts: List<Post>?): List<InlinePart> {
    if (!inner.contains('/') || inner.contains('*')) return listOf(InlinePart.Code(inner))
    fun asCode(parts: List<InlinePart>): List<InlinePart> = parts.map { part ->
        when (part) {
            is InlinePart.Fraction -> InlinePart.Fraction(asCode(part.numerator), asCode(part.denominator))
            else -> InlinePart.Code(inlinePartsToText(listOf(part)))
        }
    }
    val parts = parseInlineParts(inner, posts)
    return if (parts.any { it is InlinePart.Fraction }) asCode(parts) else listOf(InlinePart.Code(inner))
}

// Builds a regex matching all inflected Czech forms of a canonical person name.
// Each word is matched as a prefix followed by optional ASCII suffix letters (covers all Czech cases).
private fun buildPersonPattern(canonicalName: String): Regex? {
    val words = canonicalName.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (words.isEmpty()) return null
    val pattern = words.joinToString("\\s+") { word ->
        Regex.escape(word.trimEnd('.', ',', ';')) + "\\w*\\.?"
    }
    return Regex(pattern, RegexOption.IGNORE_CASE)
}

fun injectPersonMentions(parts: List<InlinePart>, personNames: List<String>): List<InlinePart> {
    if (personNames.isEmpty()) return parts
    // Longer names first to prefer more specific matches
    val namePatterns = personNames
        .sortedByDescending { it.length }
        .mapNotNull { name -> buildPersonPattern(name)?.let { name to it } }
    if (namePatterns.isEmpty()) return parts

    fun injectIntoText(text: String): List<InlinePart> {
        if (text.isEmpty()) return emptyList()
        var bestMatch: MatchResult? = null
        var bestCanonical = ""
        for ((canonical, regex) in namePatterns) {
            val m = regex.find(text) ?: continue
            if (bestMatch == null || m.range.first < bestMatch!!.range.first) {
                bestMatch = m
                bestCanonical = canonical
            }
        }
        if (bestMatch == null) return listOf(InlinePart.Text(text))
        val result = mutableListOf<InlinePart>()
        if (bestMatch.range.first > 0) result.add(InlinePart.Text(text.substring(0, bestMatch.range.first)))
        result.add(InlinePart.PersonMention(
            name = bestMatch.value,         // inflected form as displayed in content
            canonicalName = bestCanonical   // canonical name passed to API on tap
        ))
        result.addAll(injectIntoText(text.substring(bestMatch.range.last + 1)))
        return result
    }

    return parts.flatMap { part ->
        when (part) {
            is InlinePart.Text -> injectIntoText(part.text)
            else -> listOf(part)
        }
    }
}

fun injectPersonMentionsIntoElements(
    elements: List<ContentElement>,
    personNames: List<String>
): List<ContentElement> {
    if (personNames.isEmpty()) return elements
    fun inject(parts: List<InlinePart>) = injectPersonMentions(parts, personNames)
    return elements.map { element ->
        when (element) {
            is ContentElement.Paragraph -> element.copy(parts = inject(element.parts))
            is ContentElement.Heading -> element.copy(parts = inject(element.parts))
            is ContentElement.BulletList -> element.copy(items = element.items.map { inject(it) })
            is ContentElement.InlineText -> element.copy(parts = inject(element.parts))
            is ContentElement.Callout -> element.copy(children = injectPersonMentionsIntoElements(element.children, personNames))
            else -> element
        }
    }
}

// Přidání nové InlinePart pro odkazy

