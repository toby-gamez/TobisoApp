package com.example.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineManagerScreen(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val isOfflineMode by mainViewModel.isOffline.collectAsState()

    var categoriesCount by remember { mutableStateOf<Int?>(null) }
    var postsCount by remember { mutableStateOf<Int?>(null) }
    var questionsCount by remember { mutableStateOf<Int?>(null) }
    var questionsPostsCount by remember { mutableStateOf<Int?>(null) }
    var relatedPostsCount by remember { mutableStateOf<Int?>(null) }
    var addendumsCount by remember { mutableStateOf<Int?>(null) }
    var exercisesCount by remember { mutableStateOf<Int?>(null) }
    var lastUpdateFormatted by remember { mutableStateOf<String?>(null) }
    var lastUpdateTimestamp by remember { mutableStateOf<Long?>(null) }
    var cacheFresh15 by remember { mutableStateOf<Boolean?>(null) }
    val offlineManager = OfflineDataManager(context)
    val toastMessage by mainViewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineDownloading by mainViewModel.offlineDownloading.collectAsState()
    val offlineProgress by mainViewModel.offlineDownloadProgress.collectAsState()

    suspend fun loadCacheInfo() {
        withContext(Dispatchers.IO) {
            try {
                val cats = offlineManager.getCachedCategories()
                val posts = offlineManager.getCachedPosts()
                val questions = offlineManager.getCachedQuestions()
                val qposts = offlineManager.getCachedQuestionsPosts()
                val relatedPosts = offlineManager.getCachedRelatedPosts()
                val addendums = offlineManager.getCachedAddendums()
                val exercises = offlineManager.getCachedExercises()
                val last = offlineManager.getLastUpdateFormatted()
                val lastTs = offlineManager.getLastUpdateTimestamp()
                val fresh = offlineManager.isCacheFresh(15)

                categoriesCount = cats?.size
                postsCount = posts?.size
                questionsCount = questions?.size
                questionsPostsCount = qposts?.size
                relatedPostsCount = relatedPosts?.size
                addendumsCount = addendums?.size
                exercisesCount = exercises?.size
                lastUpdateFormatted = last
                lastUpdateTimestamp = lastTs
                cacheFresh15 = fresh
            } catch (e: Exception) {
                categoriesCount = null
                postsCount = null
                questionsCount = null
                questionsPostsCount = null
                relatedPostsCount = null
                addendumsCount = null
                exercisesCount = null
                lastUpdateFormatted = null
                cacheFresh15 = null
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCacheInfo()
    }

    val coroutineScope = rememberCoroutineScope()

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

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
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
                            // refresh view of cache info
                            coroutineScope.launch { loadCacheInfo() }
                        }) {
                            Text("Obnovit")
                        }

                        Button(onClick = {
                            // spustit manuální stažení přes viewModel
                            mainViewModel.downloadAllOfflineData(context)
                        }) {
                            Text("Stáhnout offline data")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Snackbar for toastMessage (shown at bottom)
        LaunchedEffect(toastMessage) {
            toastMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                mainViewModel.clearToast()
            }
        }

        // When background offline download finishes, refresh cached info here and show feedback
        LaunchedEffect(offlineDownloading) {
            if (!offlineDownloading) {
                // reload cache info to refresh timestamps/counts
                coroutineScope.launch { loadCacheInfo() }
                // if viewModel didn't emit a toast, show local snackbar
                // (check current toastMessage after a small delay to allow VM to set it)
                kotlinx.coroutines.delay(300)
                if (mainViewModel.toastMessage.value == null) {
                    snackbarHostState.showSnackbar("Offline obsah byl aktualizován")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
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
