package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import timber.log.Timber
import coil.compose.AsyncImage
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.AstBlockNodeComposer
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.markdown.node.AstBlockNodeType
import com.halilibo.richtext.markdown.node.AstCode
import com.halilibo.richtext.markdown.node.AstEmphasis
import com.halilibo.richtext.markdown.node.AstHardLineBreak
import com.halilibo.richtext.markdown.node.AstHtmlInline
import com.halilibo.richtext.markdown.node.AstImage
import com.halilibo.richtext.markdown.node.AstLink
import com.halilibo.richtext.markdown.node.AstNode
import com.halilibo.richtext.markdown.node.AstParagraph
import com.halilibo.richtext.markdown.node.AstSoftLineBreak
import com.halilibo.richtext.markdown.node.AstStrikethrough
import com.halilibo.richtext.markdown.node.AstStrongEmphasis
import com.halilibo.richtext.markdown.node.AstText
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.string.InlineContent
import com.halilibo.richtext.ui.string.RichTextString
import com.halilibo.richtext.ui.string.Text as RichTextScopeText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.ColorPainter
import kotlin.math.max
import com.tobiso.tobisoappnative.utils.TextUtils
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.model.Post
import timber.log.Timber as TimberAlias

val prefixRegex = Regex("^(ml-|sl-|li-|hv-|m-|ch-|f-|pr-|z-)")

private val inlineFractionRegex = Regex(
    """(?<![\p{L}\p{N}_])([0-9]+(?:[\.,][0-9]+)?|[\p{L}])\/([0-9]+(?:[\.,][0-9]+)?|[\p{L}])(?![\p{L}\p{N}_])"""
)

private fun fractionInlineContent(numerator: String, denominator: String): InlineContent {
    return InlineContent(
        initialSize = {
            val maxLen = max(numerator.length, denominator.length).coerceAtLeast(1)
            val charWidth = 6.sp.toPx()
            val width = (maxLen * charWidth + 8.sp.toPx()).toInt().coerceAtLeast(1)
            val height = (18.sp.toPx()).toInt().coerceAtLeast(1)
            IntSize(width, height)
        },
        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
    ) {
        InlineFraction(
            numerator = numerator,
            denominator = denominator,
            modifier = Modifier
        )
    }
}

private fun inlineChildren(astNode: AstNode): List<AstNode> {
    val out = mutableListOf<AstNode>()
    var child = astNode.links.firstChild
    while (child != null) {
        out.add(child)
        child = child.links.next
    }
    return out
}

private fun appendTextWithFractions(builder: RichTextString.Builder, text: String) {
    var lastIndex = 0
    for (match in inlineFractionRegex.findAll(text)) {
        val start = match.range.first
        val end = match.range.last + 1
        if (start > lastIndex) builder.append(text.substring(lastIndex, start))

        val numerator = match.groupValues[1]
        val denominator = match.groupValues[2]

        val numIsWord = numerator.length > 1 && numerator.all { it.isLetter() }
        val denIsWord = denominator.length > 1 && denominator.all { it.isLetter() }
        if (numIsWord || denIsWord) {
            builder.append(match.value)
        } else {
            builder.appendInlineContent(
                alternateText = match.value,
                content = fractionInlineContent(numerator, denominator)
            )
        }

        lastIndex = end
    }
    if (lastIndex < text.length) builder.append(text.substring(lastIndex))
}

private fun appendInlineNode(builder: RichTextString.Builder, node: AstNode) {
    when (val t = node.type) {
        is AstText -> appendTextWithFractions(builder, t.literal)
        is AstSoftLineBreak -> builder.append(" ")
        is AstHardLineBreak -> builder.append("\n")
        is AstHtmlInline -> builder.append(t.literal)
        is AstCode -> {
            val idx = builder.pushFormat(RichTextString.Format.Code)
            builder.append(t.literal)
            builder.pop(idx)
        }
        is AstEmphasis -> {
            val idx = builder.pushFormat(RichTextString.Format.Italic)
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstStrongEmphasis -> {
            val idx = builder.pushFormat(RichTextString.Format.Bold)
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstStrikethrough -> {
            val idx = builder.pushFormat(RichTextString.Format.Strikethrough)
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstLink -> {
            val idx = builder.pushFormat(RichTextString.Format.Link(t.destination))
            inlineChildren(node).forEach { appendInlineNode(builder, it) }
            builder.pop(idx)
        }
        is AstImage -> {
            if (t.title.isNotBlank()) builder.append(t.title)
        }
        else -> {
        }
    }
}

@Composable
fun SafeMarkdown(content: String?, modifier: Modifier = Modifier) {
    val safeContent = content
        ?.replace("\u0000", "")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return

    val displayMarkdown = remember(safeContent) {
        runCatching { TextUtils.preprocessMarkdownForDisplay(safeContent) }.getOrElse { safeContent }
    }

    val renderError = remember(safeContent) {
        try {
            safeContent.length
            null
        } catch (e: Exception) {
            Timber.e(e, "Content validation failed")
            e
        }
    }

    if (renderError != null) {
        Text(
            text = "Chyba při načítání obsahu",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
            color = MaterialTheme.colorScheme.error
        )
    } else {
        val result = runCatching {
            val parser = remember { CommonmarkAstNodeParser() }
            val astNode = remember(displayMarkdown) { parser.parse(displayMarkdown) }

            // track nodes we've already consumed (image + following caption/source)
            val consumed = remember { mutableSetOf<Int>() }

            fun paragraphPlainText(node: AstNode): String {
                val sb = StringBuilder()
                inlineChildren(node).forEach { ch ->
                    when (val t = ch.type) {
                        is AstText -> sb.append(t.literal)
                        is AstLink -> inlineChildren(ch).forEach { sub -> if (sub.type is AstText) sb.append((sub.type as AstText).literal) }
                        is AstImage -> if ((t as AstImage).title.isNotBlank()) sb.append(t.title)
                        is AstHtmlInline -> sb.append((t as AstHtmlInline).literal)
                        else -> {}
                    }
                }
                return sb.toString()
            }

            val paragraphComposer = remember {
                object : AstBlockNodeComposer {
                    override fun predicate(astBlockNodeType: AstBlockNodeType): Boolean {
                        return astBlockNodeType == AstParagraph
                    }

                    @Composable
                    override fun RichTextScope.Compose(
                        astNode: AstNode,
                        visitChildren: @Composable (AstNode) -> Unit
                    ) {
                        if (consumed.contains(astNode.hashCode())) return

                        val children = inlineChildren(astNode)

                        if (children.size == 1 && children.first().type is AstImage) {
                            val imgNode = children.first().type as AstImage

                            // look ahead up to 3 paragraph siblings to find Zdroj/Autor
                            val captionNodes = mutableListOf<AstNode>()
                            var srcNode: AstNode? = null
                            var nxt = astNode.links.next
                            var steps = 0
                            while (nxt != null && steps < 3) {
                                if (nxt.type == AstParagraph) {
                                    val txt = paragraphPlainText(nxt).trim()
                                    if (txt.isNotEmpty()) {
                                        if (txt.startsWith("Zdroj") || txt.startsWith("Autor")) {
                                            srcNode = nxt
                                            break
                                        } else {
                                            captionNodes.add(nxt)
                                        }
                                    }
                                }
                                nxt = nxt.links.next
                                steps++
                            }

                            AsyncImage(
                                model = imgNode.destination,
                                contentDescription = imgNode.title.ifBlank { "image" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 400.dp)
                                    .padding(vertical = 6.dp)
                                    .background(Color.White),
                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                error = ColorPainter(MaterialTheme.colorScheme.onErrorContainer),
                                contentScale = ContentScale.Crop
                            )

                            if (srcNode != null) {
                                // mark consumed so subsequent paragraph composers skip these nodes
                                consumed.add(astNode.hashCode())
                                captionNodes.forEach { consumed.add(it.hashCode()) }
                                consumed.add(srcNode.hashCode())

                                val sourceText = paragraphPlainText(srcNode).trim()
                                val captionText = captionNodes.joinToString(" ") { paragraphPlainText(it).trim() }
                                val isTobiso = sourceText.contains("Tobiso", ignoreCase = true)

                                if (isTobiso) {
                                    // render intermediate captions as normal markdown text and source as a small Card
                                    captionNodes.forEach { node ->
                                        val builder = RichTextString.Builder(inlineChildren(node).sumOf { (it.type as? AstText)?.literal?.length ?: 8 })
                                        inlineChildren(node).forEach { appendInlineNode(builder, it) }
                                        val rich = builder.toRichTextString()
                                        RichTextScopeText(text = rich)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(text = sourceText, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                } else {
                                    // external source: wrap caption+source in a Card under the image
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (captionText.isNotBlank()) Text(text = captionText, style = MaterialTheme.typography.bodyMedium)
                                            Text(text = sourceText, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                return
                            } else {
                                // no source found - nothing extra to render
                                return
                            }
                        }

                        val builder = RichTextString.Builder(children.sumOf { (it.type as? AstText)?.literal?.length ?: 8 })
                        children.forEach { appendInlineNode(builder, it) }
                        val rich = builder.toRichTextString()
                        RichTextScopeText(text = rich)
                    }
                }
            }

            RichText(modifier = modifier) {
                BasicMarkdown(
                    astNode = astNode,
                    astBlockNodeComposer = paragraphComposer
                )
            }
        }
        if (result.isFailure) {
            Timber.e("Markdown render failed", result.exceptionOrNull())
            Text(
                text = safeContent,
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier
            )
        } else {
            result.getOrNull()
        }
    }
}

sealed class ContentElement {
    data class MarkdownText(val text: String) : ContentElement()
    data class HighlightedBlock(val text: String) : ContentElement()
    data class ClickableLink(val text: String, val url: String, val postId: Int?) : ContentElement()
    data class VideoPlayer(val videoUrl: String, val posterUrl: String?) : ContentElement()
    data class AddendumReference(val addendumId: Int) : ContentElement()
}

fun parseContentToElements(
    content: String,
    isOffline: Boolean,
    posts: List<Post>
): List<ContentElement> {
    try {
        val imageRegex = Regex("!\\[(.*?)]\\((images/[^)]+)\\)")
        val processedContent = if (!isOffline) {
            content.replace(imageRegex) {
                val alt = it.groups[1]?.value ?: ""
                val path = it.groups[2]?.value ?: ""
                "![${alt}](https://files.tobiso.com/${path})"
            }
        } else {
            content.replace(imageRegex) {
                val alt = it.groups[1]?.value ?: ""
                "\n\n**[Obrázek: $alt - nedostupný v offline režimu]**\n\n"
            }
        }

        val blockRegex = Regex("\\.\\.\\.\\s*([\\s\\S]*?)\\s*\\.\\.\\.")
        val linkRegex = Regex("(?<!!)\\[(.+?)\\]\\((.+?)\\)")
        val videoRegex = Regex("<video[^>]*src=\"([^\"]+)\"[^>]*>(.*?)</video>", RegexOption.DOT_MATCHES_ALL)
        val addendumRegex = Regex("\\(--DOD-(\\d+)--\\)")

        val allMatches = mutableListOf<Triple<Int, Int, Pair<String, MatchResult>>>()

        blockRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "block" to it))
        }
        linkRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "link" to it))
        }
        videoRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "video" to it))
        }
        addendumRegex.findAll(processedContent).forEach {
            allMatches.add(Triple(it.range.first, it.range.last + 1, "addendum" to it))
        }

        allMatches.sortBy { it.first }

        if (allMatches.isEmpty()) {
            return listOf(ContentElement.MarkdownText(processedContent))
        }

        val elements = mutableListOf<ContentElement>()
        var lastIndex = 0

        for ((start, end, typeAndMatch) in allMatches) {
            if (start > lastIndex && start <= processedContent.length) {
                try {
                    val textBefore = processedContent.substring(lastIndex, start)
                        .replace("\u0000", "")
                        .trim()
                    if (textBefore.isNotBlank()) {
                        elements.add(ContentElement.MarkdownText(textBefore))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error extracting text before element")
                }
            }

            when (typeAndMatch.first) {
                "block" -> {
                    val blockText = typeAndMatch.second.groups[1]?.value?.trim() ?: ""
                    if (blockText.isNotBlank()) {
                        elements.add(ContentElement.HighlightedBlock(blockText))
                    }
                }
                "link" -> {
                    val linkText = typeAndMatch.second.groups[1]?.value?.trim() ?: ""
                    val url = typeAndMatch.second.groups[2]?.value?.trim() ?: ""
                    if (linkText.isNotBlank() && url.isNotBlank()) {
                        var filePath = url
                        if (filePath.endsWith(".html")) {
                            filePath = filePath.removeSuffix(".html") + ".md"
                        }
                        filePath = filePath.replace(prefixRegex, "")
                        if (!filePath.startsWith("/")) {
                            filePath = "/$filePath"
                        }
                        val post = posts.find { it.filePath == filePath }
                        elements.add(ContentElement.ClickableLink(linkText, url, post?.id))
                    }
                }
                "video" -> {
                    val videoSrc = typeAndMatch.second.groups[1]?.value ?: ""
                    if (videoSrc.isNotBlank()) {
                        val videoUrl = if (videoSrc.startsWith("http")) videoSrc else "https://tobiso.com/$videoSrc"
                        elements.add(ContentElement.VideoPlayer(videoUrl, null))
                    }
                }
                "addendum" -> {
                    val addendumIdStr = typeAndMatch.second.groups[1]?.value ?: ""
                    val addendumId = addendumIdStr.toIntOrNull()
                    if (addendumId != null) {
                        elements.add(ContentElement.AddendumReference(addendumId))
                    }
                }
            }

            lastIndex = end
        }

        if (lastIndex < processedContent.length) {
            try {
                val textAfter = processedContent.substring(lastIndex)
                    .replace("\u0000", "")
                    .trim()
                if (textAfter.isNotBlank()) {
                    elements.add(ContentElement.MarkdownText(textAfter))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error extracting text after elements")
            }
        }

        return elements
    } catch (e: Exception) {
        Timber.e(e, "Chyba při parsování obsahu")
        val safeContent = content.replace("\u0000", "").trim()
        return if (safeContent.isNotBlank()) {
            listOf(ContentElement.MarkdownText(safeContent))
        } else {
            emptyList()
        }
    }
}
