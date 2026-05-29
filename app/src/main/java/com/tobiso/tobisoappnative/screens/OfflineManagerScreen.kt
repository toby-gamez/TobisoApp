package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.viewmodel.offlinemanager.OfflineManagerViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
            title = { Text("Správce offline dat", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                if (offlineDownloading) {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        CircularProgressIndicator(
                            progress = { offlineProgress.coerceIn(0f, 1f) },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Offline banner
                if (isOfflineMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Offline režim",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Bez připojení nelze spravovat data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Cache stats card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        val stats = listOf(
                            "Kategorie" to (categoriesCount?.toString() ?: "—"),
                            "Články" to (postsCount?.toString() ?: "—"),
                            "Otázky" to (questionsCount?.toString() ?: "—"),
                            "Vysvětlení otázek" to (questionsPostsCount?.toString() ?: "—"),
                            "Související články" to (relatedPostsCount?.toString() ?: "—"),
                            "Dodatky" to (addendumsCount?.toString() ?: "—"),
                            "Cvičení" to (exercisesCount?.toString() ?: "—"),
                            "Události" to (eventsCount?.toString() ?: "—")
                        )
                        stats.forEachIndexed { index, (label, value) ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Last update + freshness
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (cacheFresh15 == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Poslední aktualizace: ${lastUpdateFormatted ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val relative = lastUpdateTimestamp?.let { formatRelativeTime(it) }
                            if (relative != null) {
                                Text(text = relative, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // Action buttons (online only)
                if (!isOfflineMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { vm.loadCacheInfo() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Obnovit")
                        }
                        Button(
                            onClick = {
                                downloadEverStarted = true
                                toastShownDuringDownload = false
                                vm.downloadAllOfflineData()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stáhnout data")
                        }
                    }
                    if (lastError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Text(
                                text = lastError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
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
