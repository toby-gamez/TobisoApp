package com.tobiso.tobisoappnative.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.navigation.VideoPlayerRoute
import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.Post
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle

@Composable
fun ContentRenderer(
    contentElements: List<ContentElement>,
    isOffline: Boolean,
    posts: List<Post>,
    addendums: List<Addendum>,
    navController: NavController,
    onAddendumSelected: (Addendum) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        contentElements.forEach { element ->
            when (element) {
                is ContentElement.Intra -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.material3.MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = element.text,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                    }
                }
                is ContentElement.Heading -> {
                    val style = when (element.level) {
                        1 -> androidx.compose.material3.MaterialTheme.typography.headlineLarge
                        2 -> androidx.compose.material3.MaterialTheme.typography.headlineMedium
                        3 -> androidx.compose.material3.MaterialTheme.typography.headlineSmall
                        else -> androidx.compose.material3.MaterialTheme.typography.titleMedium
                    }
                    Text(
                        text = buildAnnotatedStringFromParts(element.parts),
                        style = style,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                is ContentElement.Paragraph -> {
                    Text(
                        text = buildAnnotatedStringFromParts(element.parts),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                is ContentElement.InlineText -> {
                    Text(
                        text = buildAnnotatedStringFromParts(element.parts),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
                is ContentElement.BulletList -> {
                    val indent = when (element.level) {
                        1 -> 16.dp
                        2 -> 32.dp
                        3 -> 48.dp
                        4 -> 64.dp
                        else -> 16.dp
                    }
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        element.items.forEach { itemParts ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.Top, modifier = Modifier.padding(start = indent, bottom = 4.dp)) {
                                Text(
                                    "•",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                val annotated = buildAnnotatedStringFromParts(itemParts)
                                androidx.compose.foundation.text.ClickableText(
                                    text = annotated,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    onClick = { offset ->
                                        annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                                            val linkPart = itemParts.filterIsInstance<InlinePart.Link>().find { link ->
                                                val start = annotated.text.indexOf(link.text)
                                                offset in start until (start + link.text.length)
                                            }
                                            if (linkPart != null) {
                                                if (linkPart.postId != null) {
                                                    navController.navigate(com.tobiso.tobisoappnative.navigation.PostDetailRoute(linkPart.postId))
                                                } else if (!isOffline && linkPart.url.startsWith("http")) {
                                                    val url = linkPart.url
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    try {
                                                        navController.context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        timber.log.Timber.e(e, "Chyba při otevírání odkazu")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                is ContentElement.NumberedList -> {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        element.items.forEachIndexed { idx, item ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.Top, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                                Text(
                                    "${idx + 1}.",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                Text(
                                    item,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is ContentElement.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.material3.MaterialTheme.shapes.small)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = element.code,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                    }
                }
                is ContentElement.BlockQuote -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.material3.MaterialTheme.shapes.small)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = element.text,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
                is ContentElement.Table -> {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)) {
                            element.header.forEach { cell ->
                                Text(
                                    text = cell,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    modifier = Modifier.weight(1f).padding(4.dp)
                                )
                            }
                        }
                        element.rows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f).padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                is ContentElement.Image -> {
                    // Pokud je obrázek offline, zobrazit placeholder
                    val url = if (isOffline && element.url.contains("images/")) null else element.url
                    if (url != null) {
                        coil.compose.AsyncImage(
                            model = url,
                            contentDescription = element.alt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 400.dp)
                                .padding(vertical = 6.dp)
                        )
                    } else {
                        Text("[Obrázek: ${element.alt} - nedostupný v offline režimu]", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                }
                is ContentElement.VideoPlayer -> {
                    if (isOffline) {
                        Text(text = "*[Video nedostupné v offline režimu]*", modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        OutlinedButton(onClick = {
                            navController.navigate(VideoPlayerRoute(videoUrl = Uri.encode(element.videoUrl)))
                        }, modifier = Modifier.padding(vertical = 8.dp)) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Přehrát video", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is ContentElement.ClickableLink -> {
                    val linkText = element.text.trim()
                    if (linkText.isNotBlank()) {
                        Text(
                            text = linkText,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable {
                                if (element.postId != null) {
                                    navController.navigate(com.tobiso.tobisoappnative.navigation.PostDetailRoute(element.postId))
                                } else if (!isOffline) {
                                    val url = element.url
                                    val fullUrl = if (url.startsWith("http")) url else "https://files.tobiso.com/" + url.removePrefix("/")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try {
                                        navController.context.startActivity(intent)
                                    } catch (e: Exception) {
                                        timber.log.Timber.e(e, "Chyba při otevírání odkazu")
                                    }
                                }
                            }
                        )
                    }
                }
                is ContentElement.AddendumReference -> {
                    val addendum = addendums.find { it.id == element.addendumId }
                    if (addendum != null) {
                        androidx.compose.material3.IconButton(onClick = { onAddendumSelected(addendum) }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Help, contentDescription = "Zobrazit dodatek", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(text = "[Dodatek #${element.addendumId}]", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    // DEBUG: Výpis ContentElementů až pod hlavním obsahem
    Spacer(modifier = Modifier.height(48.dp))
    Text(
        text = "DEBUG ContentElements:\n" + contentElements.joinToString("\n") { it.toString() },
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(8.dp)
    )
    }

    @Composable
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
                        append(part.text)
                        addStyle(SpanStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), start, start + part.text.length)
                        addStringAnnotation("URL", part.url, start, start + part.text.length)
                    }
                }
            }
        }
    }
