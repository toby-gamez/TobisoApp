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
    Column {
        contentElements.forEach { element ->
            when (element) {
                is ContentElement.MarkdownText -> {
                    if (element.text.isNotBlank()) {
                        SafeMarkdown(element.text)
                    }
                }

                is ContentElement.HighlightedBlock -> {
                    if (element.text.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                    shape = androidx.compose.material3.MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                        ) {
                            SafeMarkdown(element.text)
                        }
                    }
                }

                is ContentElement.ClickableLink -> {
                    val linkText = element.text.trim()
                    if (linkText.isNotBlank()) {
                        Text(
                            text = linkText,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
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
