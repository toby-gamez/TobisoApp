package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.viewmodel.offlinemanager.OfflineManagerViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.platform.LocalView
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineManagerScreen(
    navController: NavController
) {
    val vm: OfflineManagerViewModel = hiltViewModel()
    val isOfflineMode by vm.isOffline.collectAsState()
    val cacheInfo by vm.cacheInfo.collectAsState()
    val toastMessage by vm.toastMessage.collectAsState()
    val view = LocalView.current
    val offlineDownloading by vm.offlineDownloading.collectAsState()
    val offlineProgress by vm.offlineDownloadProgress.collectAsState()
    val lastError by vm.lastError.collectAsState()
    // Guard to skip the initial composition where offlineDownloading is already false
    var downloadEverStarted by remember { mutableStateOf(false) }
    // Tracks whether a toast was already shown for the current download session
    var toastShownDuringDownload by remember { mutableStateOf(false) }

    val categoriesCount = cacheInfo.categoriesCount
    val postsCount = cacheInfo.postsCount
    val questionsCount = cacheInfo.questionsCount
    val questionsPostsCount = cacheInfo.questionsPostsCount
    val relatedPostsCount = cacheInfo.relatedPostsCount
    val addendumsCount = cacheInfo.addendumsCount
    val exercisesCount = cacheInfo.exercisesCount
    val eventsCount = cacheInfo.eventsCount
    val lastUpdateFormatted = cacheInfo.lastUpdateFormatted
    val lastUpdateTimestamp = cacheInfo.lastUpdateTimestamp
    val cacheFresh15 = cacheInfo.cacheFresh15
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadCacheInfo()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Správce offline dat", style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                if (offlineDownloading) {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        CircularProgressIndicator(
                            progress = offlineProgress.coerceIn(0f, 1f),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isOfflineMode) {
                // When offline: show detailed cached info but do NOT show action buttons
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Text(text = "Jste v offline režimu.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Nemáte připojení k internetu — nemážete spravovat data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Kategorie")
                        Text(text = categoriesCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Články")
                        Text(text = postsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Otázky")
                        Text(text = questionsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Vysvětlení otázek")
                        Text(text = questionsPostsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Související články")
                        Text(text = relatedPostsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Dodatky")
                        Text(text = addendumsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Cvičení")
                        Text(text = exercisesCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Události")
                        Text(text = eventsCount?.toString() ?: "—")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Text(text = "Poslední aktualizace:")
                        Text(text = lastUpdateFormatted ?: "—")
                        val relative = lastUpdateTimestamp?.let { formatRelativeTime(it) }
                        if (relative != null) {
                            Text(text = "($relative)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Jsou data aktuální? (<=15 min): ${cacheFresh15?.let { if (it) "ANO" else "NE" } ?: "—"}")
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Kategorie")
                        Text(text = categoriesCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Články")
                        Text(text = postsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Otázky")
                        Text(text = questionsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Vysvětlení otázek")
                        Text(text = questionsPostsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Související články")
                        Text(text = relatedPostsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Dodatky")
                        Text(text = addendumsCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Cvičení")
                        Text(text = exercisesCount?.toString() ?: "—")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Události")
                        Text(text = eventsCount?.toString() ?: "—")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Poslední aktualizace:")
                            Text(text = lastUpdateFormatted ?: "—")
                            val relative = lastUpdateTimestamp?.let { formatRelativeTime(it) }
                            if (relative != null) {
                                Text(text = "($relative)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Jsou data aktuální? (<=15 min): ${cacheFresh15?.let { if (it) "ANO" else "NE" } ?: "—"}")

                    Spacer(modifier = Modifier.height(8.dp))

                    // relative time shown below (see top-level helper)

                    Spacer(modifier = Modifier.height(20.dp))


                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = {
                            vm.loadCacheInfo()
                        }) {
                            Text("Obnovit")
                        }

                        Button(onClick = {
                            downloadEverStarted = true
                            toastShownDuringDownload = false
                            vm.downloadAllOfflineData()
                        }) {
                            Text("Stáhnout offline data")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lastError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Chyba stahování:", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Text(text = lastError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        LaunchedEffect(toastMessage) {
            toastMessage?.let { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                // remember that a toast was shown for this download session
                toastShownDuringDownload = true
                vm.clearToast()
            }
        }

        // When download finishes, reload cache info
        // downloadEverStarted guard prevents this from firing on initial composition
        LaunchedEffect(offlineDownloading) {
            if (!offlineDownloading && downloadEverStarted) {
                vm.loadCacheInfo()
                kotlinx.coroutines.delay(300)

                if (vm.toastMessage.value == null && !toastShownDuringDownload) {
                    Toast.makeText(context, "Offline obsah byl aktualizován", Toast.LENGTH_SHORT).show()
                }
                // reset guard so future downloads behave correctly
                toastShownDuringDownload = false
                downloadEverStarted = false
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Top-level helper to format relative time in Czech (simple)
private fun formatRelativeTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        diff < 60_000 -> "právě teď"
        minutes < 60 -> "před ${minutes} min"
        hours < 24 -> "před ${hours} hod"
        days < 7 -> "před ${days} dny"
        else -> java.text.SimpleDateFormat("dd. MM. yyyy", java.util.Locale.getDefault()).format(java.util.Date(ts))
    }
}
