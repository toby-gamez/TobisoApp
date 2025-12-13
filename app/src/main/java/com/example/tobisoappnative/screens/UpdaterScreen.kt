package com.example.tobisoappnative.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val version: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val author: String,
    val downloadCount: Int,
    val htmlUrl: String
)

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
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var isUpToDate by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val isOfflineMode by mainViewModel.isOffline.collectAsState()

    LaunchedEffect(Unit) {
        if (!isOfflineMode) {
            try {
                val info = fetchLatestVersionFromGithub()
                latestVersion = info.version
                releaseInfo = info
                isUpToDate = info.version == currentVersion
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Aktualizátor", style = MaterialTheme.typography.headlineLarge) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
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
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
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
                        Text(text = "Jste v debug verzi aplikace!", color = MaterialTheme.colorScheme.tertiary)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Dostupná nová verze: $latestVersion",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            releaseInfo?.let { info ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Co je nového:",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (info.name.isNotEmpty()) {
                                            Text(
                                                text = info.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        
                                        if (info.body.isNotEmpty()) {
                                            val bulletPoints = info.body.lines()
                                                .filter { it.startsWith("- ") }
                                                .map { it.removePrefix("- ").trim() }
                                            
                                            bulletPoints.forEach { point ->
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                ) {
                                                    Text(
                                                        "•",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                                    )
                                                    Text(
                                                        point,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Datum vydání: ${info.publishedAt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (info.author.isNotEmpty()) {
                                            Text(
                                                text = "Autor: ${info.author}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (info.downloadCount > 0) {
                                            Text(
                                                text = "Počet stažení: ${info.downloadCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { openDownloadPage(latestVersion!!) },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("Stáhnout aktualizaci")
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchLatestVersionFromGithub(): ReleaseInfo = withContext(Dispatchers.IO) {
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
    
    val tag = json.getString("tag_name").removePrefix("v")
    val name = json.optString("name", "")
    val bodyRaw = json.optString("body", "Žádné poznámky k vydání.")
    val body = bodyRaw.lines().drop(1).joinToString("\n").trim()
    val publishedAtRaw = json.optString("published_at", "").substringBefore("T")
    val publishedAt = if (publishedAtRaw.isNotEmpty()) {
        val parts = publishedAtRaw.split("-")
        if (parts.size == 3) "${parts[2]}. ${parts[1]}. ${parts[0]}" else publishedAtRaw
    } else ""
    val authorRaw = json.optJSONObject("author")?.optString("login", "") ?: ""
    val author = if (authorRaw == "toby-gamez") "Taneq" else authorRaw
    
    var downloadCount = 0
    val assets = json.optJSONArray("assets")
    if (assets != null) {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            downloadCount += asset.optInt("download_count", 0)
        }
    }
    
    val htmlUrl = json.optString("html_url", "")
    
    return@withContext ReleaseInfo(
        version = tag,
        name = name,
        body = body,
        publishedAt = publishedAt,
        author = author,
        downloadCount = downloadCount,
        htmlUrl = htmlUrl
    )
}
