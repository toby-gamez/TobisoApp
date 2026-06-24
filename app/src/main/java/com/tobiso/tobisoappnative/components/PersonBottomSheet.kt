package com.tobiso.tobisoappnative.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.PersonResponse
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonBottomSheet(
    personName: String,
    clientId: String,
    deviceId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var person by remember(personName) { mutableStateOf<PersonResponse?>(null) }
    var isLoading by remember(personName) { mutableStateOf(true) }
    var error by remember(personName) { mutableStateOf<String?>(null) }

    LaunchedEffect(personName) {
        isLoading = true
        error = null
        person = null
        try {
            person = ApiClient.apiService.getPerson(clientId, deviceId, personName)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load person: $personName")
            error = "Informace nejsou k dispozici."
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = person?.name ?: personName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                person != null -> {
                    val p = person!!

                    // Photo + role/years row
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        if (!p.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = p.photoUrl,
                                contentDescription = p.name,
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(end = 12.dp)
                            )
                        }
                        Column {
                            if (!p.role.isNullOrBlank()) {
                                Text(
                                    text = p.role,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            val yearsText = when {
                                p.birthYear != null && p.deathYear != null -> "(${p.birthYear}–${p.deathYear})"
                                p.birthYear != null -> "(*${p.birthYear})"
                                p.deathYear != null -> "(†${p.deathYear})"
                                else -> null
                            }
                            if (yearsText != null) {
                                Text(
                                    text = yearsText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }

                    // Bio
                    if (!p.bio.isNullOrBlank()) {
                        Text(
                            text = p.bio,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // External link
                    if (!p.externalLink.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(p.externalLink))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to open person link")
                                }
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(siteName(p.externalLink))
                        }
                    }
                }
            }

            // AI disclaimer
            Text(
                text = "AI může dělat chyby, vždy si zkontrolujte odpovědi.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun siteName(url: String): String = try {
    val host = Uri.parse(url).host ?: return "Odkaz"
    when {
        host.contains("wikipedia") -> "Wikipedia"
        host.contains("britannica") -> "Britannica"
        host.contains("wikidata") -> "Wikidata"
        host.contains("imdb") -> "IMDb"
        host.contains("musicbrainz") -> "MusicBrainz"
        host.contains("spotify") -> "Spotify"
        host.contains("youtube") -> "YouTube"
        else -> host.removePrefix("www.")
    }
} catch (_: Exception) { "Odkaz" }
