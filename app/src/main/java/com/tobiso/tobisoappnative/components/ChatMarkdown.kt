package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
private val bulletRegex = Regex("^(\\s*)[-*+•]\\s+(.*)$")
private val numberedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val linkRegex = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

/** Lightweight markdown renderer for AI chat replies (headings, lists, bold/italic, code, links). */
@Composable
fun ChatMarkdownText(
    text: String,
    color: Color,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    val codeBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val annotated = remember(text, color, codeBg) { markdownToAnnotated(text, codeBg) }
    SelectionContainer {
        Text(text = annotated, style = style, color = color, modifier = modifier)
    }
}

internal fun markdownToAnnotated(text: String, codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        var inFence = false
        val lines = text.replace("\r\n", "\n").split("\n")
        lines.forEachIndexed { index, line ->
            if (index > 0) append("\n")
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                inFence = !inFence
                return@forEachIndexed
            }
            if (inFence) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                    append(line)
                }
                return@forEachIndexed
            }
            val heading = headingRegex.matchEntire(trimmed)
            val bullet = bulletRegex.matchEntire(line)
            val numbered = numberedRegex.matchEntire(line)
            when {
                heading != null -> {
                    val level = heading.groupValues[1].length
                    val size = when (level) { 1 -> 20.sp; 2 -> 18.sp; 3 -> 16.sp; else -> 15.sp }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)) {
                        appendInline(heading.groupValues[2], codeBackground)
                    }
                }
                bullet != null -> {
                    val indent = "  ".repeat(bullet.groupValues[1].length / 2)
                    append("$indent•  ")
                    appendInline(bullet.groupValues[2], codeBackground)
                }
                numbered != null -> {
                    val indent = "  ".repeat(numbered.groupValues[1].length / 2)
                    append("$indent${numbered.groupValues[2]}.  ")
                    appendInline(numbered.groupValues[3], codeBackground)
                }
                else -> appendInline(line, codeBackground)
            }
        }
    }

private fun AnnotatedString.Builder.appendInline(raw: String, codeBackground: Color) {
    var i = 0
    val n = raw.length
    while (i < n) {
        when {
            raw.startsWith("`", i) -> {
                val end = raw.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append('`'); i++ }
            }
            raw.startsWith("***", i) || raw.startsWith("___", i) -> {
                val marker = raw.substring(i, i + 3)
                val end = raw.indexOf(marker, i + 3)
                if (end > i + 3) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        appendInline(raw.substring(i + 3, end), codeBackground)
                    }
                    i = end + 3
                } else { append(marker); i += 3 }
            }
            raw.startsWith("**", i) || raw.startsWith("__", i) -> {
                val marker = raw.substring(i, i + 2)
                val end = raw.indexOf(marker, i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInline(raw.substring(i + 2, end), codeBackground)
                    }
                    i = end + 2
                } else { append(marker); i += 2 }
            }
            raw[i] == '*' -> {
                val end = raw.indexOf('*', i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInline(raw.substring(i + 1, end), codeBackground)
                    }
                    i = end + 1
                } else { append('*'); i++ }
            }
            raw[i] == '[' -> {
                val m = linkRegex.matchAt(raw, i)
                if (m != null) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(m.groupValues[1])
                    }
                    i = m.range.last + 1
                } else { append('['); i++ }
            }
            else -> { append(raw[i]); i++ }
        }
    }
}
