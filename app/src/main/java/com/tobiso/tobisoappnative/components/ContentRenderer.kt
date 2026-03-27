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
                        text = element.text,
                        style = style,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                is ContentElement.Paragraph -> {
                    Text(
                        text = element.text,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                is ContentElement.BulletList -> {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        element.items.forEach { item ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.Top, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                                Text(
                                    "•",
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
                                    navController.navigate("post/${element.postId}")
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
}
