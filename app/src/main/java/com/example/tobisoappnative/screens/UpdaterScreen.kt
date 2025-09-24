package com.example.tobisoappnative.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(
    navController: NavController,
    mainViewModel: com.example.tobisoappnative.viewmodel.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val currentVersion = packageInfo?.versionName ?: "?"
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isUpToDate by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val isOfflineMode by mainViewModel.isOffline.collectAsState()

    LaunchedEffect(Unit) {
        if (!isOfflineMode) {
            try {
                val version = fetchLatestVersionFromGithub()
                latestVersion = version
                isUpToDate = version == currentVersion
            } catch (e: Exception) {
                error = "Chyba při kontrole verze: ${e.localizedMessage}"
            }
        }
    }

    fun openDownloadPage(version: String) {
        val downloadUrl = "https://github.com/toby-gamez/TobisoAppNative/releases/download/v$version/tobiso.apk"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Aktualizátor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isOfflineMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Vaše verze: $currentVersion", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Jste v offline režimu.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aktualizátor je funkční pouze v online režimu.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (error != null) {
                Text(text = error!!, color = Color.Red)
            } else if (latestVersion == null) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Vaše verze: $currentVersion", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isUpToDate == true) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Aktuální", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Aplikace je aktuální.", color = MaterialTheme.colorScheme.tertiary)
                    } else if (latestVersion != null && currentVersion > latestVersion!!) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Debug", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Jste v Debug verzi aplikace!", color = MaterialTheme.colorScheme.tertiary)
                    } else {
                        Text(text = "Dostupná nová verze: $latestVersion", color = Color.Blue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            openDownloadPage(latestVersion!!)
                        }) {
                            Text("Stáhnout")
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchLatestVersionFromGithub(): String = withContext(Dispatchers.IO) {
    val url = URL("https://api.github.com/repos/toby-gamez/TobisoAppNative/releases/latest")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    connection.connect()
    if (connection.responseCode != 200) {
        throw Exception("HTTP ${connection.responseCode}")
    }
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(response)
    val tag = json.getString("tag_name")
    // Očekává se formát vX.X
    return@withContext tag.removePrefix("v")
}
