package com.tobiso.tobisoappnative.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.navigation.VideoPlayerRoute

@Composable
fun ContentRenderer(
    contentElements: List<ContentElement>,
    isOffline: Boolean,
    posts: List<Post>,
    addendums: List<Addendum>,
    navController: NavController,
    onAddendumSelected: (Addendum) -> Unit,
    showImagePaths: Boolean = false
) {
    // Backwards-compatible single-entry API: render as column of elements
    Column(modifier = Modifier.fillMaxWidth()) {
        contentElements.forEach { element ->
            ElementRenderer(
                element = element,
                isOffline = isOffline,
                posts = posts,
                addendums = addendums,
                navController = navController,
                onAddendumSelected = onAddendumSelected,
                showImagePaths = showImagePaths
            )
        }
    }
}

@Composable
fun ElementRenderer(
    element: ContentElement,
    isOffline: Boolean,
    posts: List<Post>,
    addendums: List<Addendum>,
    navController: NavController,
    onAddendumSelected: (Addendum) -> Unit,
    showImagePaths: Boolean = false
) {
    when (element) {
        is ContentElement.Intra -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                Text(text = element.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        is ContentElement.Heading -> {
            val annotated = remember(element.parts) { buildAnnotatedStringFromParts(element.parts) }
            val style = when (element.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.titleMedium
            }
            Text(text = annotated, style = style, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        }
        is ContentElement.Paragraph -> RenderParagraph(element, isOffline, posts, addendums, navController, onAddendumSelected)
        is ContentElement.InlineText -> RenderInlineText(element, isOffline, posts, addendums, navController, onAddendumSelected)
        is ContentElement.BulletList -> RenderBulletList(element, isOffline, posts, addendums, navController, onAddendumSelected)
        is ContentElement.NumberedList -> {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                element.items.forEachIndexed { idx, item ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.Top, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                        Text("${idx + 1}.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
                        Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        is ContentElement.CodeBlock -> {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small).padding(12.dp)) {
                Text(text = element.code, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
            }
        }
        is ContentElement.BlockQuote -> {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small).padding(12.dp)) {
                Text(text = element.text, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
            }
        }
        is ContentElement.Table -> {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    element.header.forEach { cell ->
                        Text(text = cell, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f).padding(4.dp))
                    }
                }
                element.rows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { cell ->
                            Text(text = cell, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(4.dp))
                        }
                    }
                }
            }
        }
        is ContentElement.Image, is ContentElement.ImageWithMeta -> RenderImage(element, isOffline, posts, navController, showImagePaths)
        is ContentElement.VideoPlayer -> {
            if (isOffline) Text(text = "*[Video nedostupné v offline režimu]*", modifier = Modifier.padding(vertical = 8.dp))
            else DisableSelection {
                OutlinedButton(onClick = { navController.navigate(VideoPlayerRoute(videoUrl = Uri.encode(element.videoUrl))) }, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Přehrát video", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Video", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        is ContentElement.ClickableLink -> {
            val linkText = element.text.trim()
            if (linkText.isNotBlank()) {
                Text(
                    text = linkText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                    modifier = Modifier.clickable {
                        if (element.postId != null) navController.navigate(com.tobiso.tobisoappnative.navigation.PostDetailRoute(element.postId))
                        else if (!isOffline) {
                            val url = element.url
                            val fullUrl = if (url.startsWith("http")) url else "https://files.tobiso.com/" + url.removePrefix("/")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(fullUrl))
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            try { navController.context.startActivity(intent) } catch (e: Exception) { timber.log.Timber.e(e, "Chyba při otevírání odkazu") }
                        }
                    }
                )
            }
        }
        is ContentElement.AddendumReference -> {
            // handled inline
        }
    }
}

@Composable
private fun RenderParagraph(
    element: ContentElement.Paragraph,
    isOffline: Boolean,
    posts: List<Post>,
    addendums: List<Addendum>,
    navController: NavController,
    onAddendumSelected: (Addendum) -> Unit
) {
    val annotated = remember(element.parts) { buildAnnotatedStringFromParts(element.parts) }
    val text = annotated.text
    val addendumRanges = annotated.getStringAnnotations("ADDENDUM", 0, text.length)
    if (addendumRanges.isEmpty()) {
        val urlAnnotations = annotated.getStringAnnotations("URL", 0, text.length)
        if (urlAnnotations.isEmpty()) Text(text = annotated, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        else ClickableText(text = annotated, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp), onClick = { offset ->
            handleAnnotatedClick(annotated, offset, element.parts, posts, isOffline, navController, addendums, onAddendumSelected)
        })
    } else {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            var lastIndex = 0
            for (range in addendumRanges) {
                val start = range.start
                val end = range.end
                if (lastIndex < start) {
                    val subAnnotated = annotated.subSequence(lastIndex, start)
                    val subUrlAnnotations = subAnnotated.getStringAnnotations("URL", 0, subAnnotated.length)
                    if (subUrlAnnotations.isEmpty()) Text(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    else ClickableText(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, lastIndex + offset, element.parts, posts, isOffline, navController, addendums, onAddendumSelected) })
                }
                val addendumId = range.item.toIntOrNull()
                val addendum = addendums.find { it.id == addendumId }
                if (addendum != null) IconButton(onClick = { onAddendumSelected(addendum) }, modifier = Modifier.size(18.dp).padding(horizontal = 2.dp)) { Icon(imageVector = Icons.Default.Help, contentDescription = "Zobrazit dodatek", tint = MaterialTheme.colorScheme.primary) }
                lastIndex = end
            }
            if (lastIndex < text.length) {
                val subAnnotated = annotated.subSequence(lastIndex, text.length)
                val subUrlAnnotations = subAnnotated.getStringAnnotations("URL", 0, subAnnotated.length)
                if (subUrlAnnotations.isEmpty()) Text(text = subAnnotated, style = MaterialTheme.typography.bodyLarge)
                else ClickableText(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, lastIndex + offset, element.parts, posts, isOffline, navController, addendums, onAddendumSelected) })
            }
        }
    }
}

@Composable
private fun RenderInlineText(
    element: ContentElement.InlineText,
    isOffline: Boolean,
    posts: List<Post>,
    addendums: List<Addendum>,
    navController: NavController,
    onAddendumSelected: (Addendum) -> Unit
) {
    val annotated = remember(element.parts) { buildAnnotatedStringFromParts(element.parts) }
    val text = annotated.text
    val addendumRanges = annotated.getStringAnnotations("ADDENDUM", 0, text.length)
    if (addendumRanges.isEmpty()) {
        val urlAnnotations = annotated.getStringAnnotations("URL", 0, text.length)
        if (urlAnnotations.isEmpty()) Text(text = annotated, style = MaterialTheme.typography.bodyLarge)
        else ClickableText(text = annotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, offset, element.parts, posts, isOffline, navController, addendums, onAddendumSelected) })
    } else {
        Row {
            var lastIndex = 0
            for (range in addendumRanges) {
                val start = range.start
                val end = range.end
                if (lastIndex < start) {
                    val subAnnotated = annotated.subSequence(lastIndex, start)
                    val subUrlAnnotations = subAnnotated.getStringAnnotations("URL", 0, subAnnotated.length)
                    if (subUrlAnnotations.isEmpty()) Text(text = subAnnotated, style = MaterialTheme.typography.bodyLarge)
                    else ClickableText(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, lastIndex + offset, element.parts, posts, isOffline, navController, addendums, onAddendumSelected) })
                }
                val addendumId = range.item.toIntOrNull()
                val addendum = addendums.find { it.id == addendumId }
                if (addendum != null) IconButton(onClick = { onAddendumSelected(addendum) }, modifier = Modifier.size(18.dp).padding(horizontal = 2.dp)) { Icon(imageVector = Icons.Default.Help, contentDescription = "Zobrazit dodatek", tint = MaterialTheme.colorScheme.primary) }
                lastIndex = end
            }
            if (lastIndex < text.length) {
                val subAnnotated = annotated.subSequence(lastIndex, text.length)
                ClickableText(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, lastIndex + offset, element.parts, posts, isOffline, navController, addendums, onAddendumSelected) })
            }
        }
    }
}

@Composable
private fun RenderBulletList(
    element: ContentElement.BulletList,
    isOffline: Boolean,
    posts: List<Post>,
    addendums: List<Addendum>,
    navController: NavController,
    onAddendumSelected: (Addendum) -> Unit
) {
    val indent = when (element.level) { 1 -> 16.dp; 2 -> 32.dp; 3 -> 48.dp; 4 -> 64.dp; else -> 16.dp }
    Column(modifier = Modifier) {
        element.items.forEach { itemParts ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top, modifier = Modifier.padding(start = indent)) {
                Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
                val annotated = remember(itemParts) { buildAnnotatedStringFromParts(itemParts) }
                val text = annotated.text
                val addendumRanges = annotated.getStringAnnotations("ADDENDUM", 0, text.length)
                if (addendumRanges.isEmpty()) {
                    val urlAnnotations = annotated.getStringAnnotations("URL", 0, text.length)
                    if (urlAnnotations.isEmpty()) Text(text = annotated, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    else ClickableText(text = annotated, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), onClick = { offset -> handleAnnotatedClick(annotated, offset, itemParts, posts, isOffline, navController, addendums, onAddendumSelected) })
                } else {
                    Row(modifier = Modifier.weight(1f)) {
                        var lastIndex = 0
                        for (range in addendumRanges) {
                            val start = range.start; val end = range.end
                            if (lastIndex < start) {
                                val subAnnotated = annotated.subSequence(lastIndex, start)
                                val subUrlAnnotations = subAnnotated.getStringAnnotations("URL", 0, subAnnotated.length)
                                if (subUrlAnnotations.isEmpty()) Text(text = subAnnotated, style = MaterialTheme.typography.bodyLarge)
                                else ClickableText(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, lastIndex + offset, itemParts, posts, isOffline, navController, addendums, onAddendumSelected) })
                            }
                            val addendumId = range.item.toIntOrNull()
                            val addendum = addendums.find { it.id == addendumId }
                            if (addendum != null) IconButton(onClick = { onAddendumSelected(addendum) }, modifier = Modifier.size(18.dp).padding(horizontal = 2.dp)) { Icon(imageVector = Icons.Default.Help, contentDescription = "Zobrazit dodatek", tint = MaterialTheme.colorScheme.primary) }
                            lastIndex = end
                        }
                        if (lastIndex < text.length) {
                            val subAnnotated = annotated.subSequence(lastIndex, text.length)
                            ClickableText(text = subAnnotated, style = MaterialTheme.typography.bodyLarge, onClick = { offset -> handleAnnotatedClick(annotated, lastIndex + offset, itemParts, posts, isOffline, navController, addendums, onAddendumSelected) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderImage(
    element: ContentElement,
    isOffline: Boolean,
    posts: List<Post>,
    navController: NavController,
    showImagePaths: Boolean
) {
    val alt: String; val url: String; var caption: String? = null; var source: String? = null; var isTobiso = false
    if (element is ContentElement.Image) { alt = element.alt; url = element.url }
    else { val meta = element as ContentElement.ImageWithMeta; alt = meta.alt; url = meta.url; caption = meta.caption; source = meta.source; isTobiso = meta.isTobiso }

    val originalUrl = url
    val baseAppliedUrl = if (originalUrl.startsWith("http")) originalUrl else "https://files.tobiso.com/" + originalUrl.removePrefix("/")
    val isBlockedByOffline = isOffline && baseAppliedUrl.contains("images/")
    val finalUrlForImage = if (isBlockedByOffline) null else baseAppliedUrl

    if (finalUrlForImage != null) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            AsyncImage(model = finalUrlForImage, contentDescription = alt, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 400.dp).padding(vertical = 6.dp))
            if (!caption.isNullOrBlank() || !source.isNullOrBlank()) {
                DisableSelection {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (!caption.isNullOrBlank()) Text(text = caption, style = MaterialTheme.typography.bodyMedium)
                            if (!source.isNullOrBlank()) {
                                val sourceParts = remember(source) { parseInlineParts(source, posts) }
                                val sourceAnnotated = remember(sourceParts) { buildAnnotatedStringFromParts(sourceParts) }
                                val sourceUrlAnnotations = sourceAnnotated.getStringAnnotations("URL", 0, sourceAnnotated.length)
                                if (sourceUrlAnnotations.isEmpty()) Text(text = sourceAnnotated, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(top = 6.dp))
                                else ClickableText(text = sourceAnnotated, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(top = 6.dp), onClick = { offset ->
                                    sourceAnnotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                                        val url = ann.item
                                        val linkPart = sourceParts.filterIsInstance<InlinePart.Link>().find { it.url == url }
                                        if (linkPart?.postId != null) navController.navigate(com.tobiso.tobisoappnative.navigation.PostDetailRoute(linkPart.postId))
                                        else if (!isOffline) {
                                            val fullUrl = if (url.startsWith("http")) url else "https://files.tobiso.com/" + url.removePrefix("/")
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            try { navController.context.startActivity(intent) } catch (e: Exception) { timber.log.Timber.e(e, "Chyba při otevírání zdrojového odkazu") }
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
            if (showImagePaths) {
                Text(text = "Original: $originalUrl", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                Text(text = "Used: $baseAppliedUrl", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                Text(text = "Offline blocked: $isBlockedByOffline", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
            }
        }
    } else {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text("[Obrázek: $alt - nedostupný v offline režimu]", style = MaterialTheme.typography.bodySmall)
            if (showImagePaths) {
                Text(text = "Original: $originalUrl", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                Text(text = "Used: $baseAppliedUrl", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                Text(text = "Offline blocked: $isBlockedByOffline", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private fun handleAnnotatedClick(
    annotated: AnnotatedString,
    offset: Int,
    parts: List<InlinePart>,
    posts: List<Post>,
    isOffline: Boolean,
    navController: NavController,
    addendums: List<Addendum>,
    onAddendumSelected: (Addendum) -> Unit
) {
    annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
        val url = ann.item
        val linkPart = parts.filterIsInstance<InlinePart.Link>().find { it.url == url }
        if (linkPart != null) {
            if (linkPart.postId != null) navController.navigate(com.tobiso.tobisoappnative.navigation.PostDetailRoute(linkPart.postId))
            else if (!isOffline && linkPart.url.startsWith("http")) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(linkPart.url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try { navController.context.startActivity(intent) } catch (e: Exception) { timber.log.Timber.e(e, "Chyba při otevírání odkazu") }
            }
        }
    }
    annotated.getStringAnnotations("ADDENDUM", offset, offset).firstOrNull()?.let { ann ->
        val addendumId = ann.item.toIntOrNull()
        if (addendumId != null) {
            val addendum = addendums.find { it.id == addendumId }
            if (addendum != null) onAddendumSelected(addendum)
        }
    }
}

fun buildAnnotatedStringFromParts(parts: List<InlinePart>): AnnotatedString {
    return buildAnnotatedString {
        for (part in parts) {
            when (part) {
                is InlinePart.Text -> append(part.text)
                is InlinePart.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
                is InlinePart.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(part.text) }
                is InlinePart.BoldItalic -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(part.text) }
                is InlinePart.Link -> {
                    val start = length
                    val linkStyle = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    append(part.text)
                    addStyle(linkStyle, start, start + part.text.length)
                    addStringAnnotation("URL", part.url, start, start + part.text.length)
                }
                is InlinePart.Addendum -> {
                    val start = length
                    append("\uFFFC")
                    addStringAnnotation("ADDENDUM", part.addendumId.toString(), start, start + 1)
                }
            }
        }
    }
}
