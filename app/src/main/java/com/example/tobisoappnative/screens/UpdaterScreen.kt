package com.example.tobisoappnative.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        try {
            val version = fetchLatestVersionFromGithub()
            latestVersion = version
            isUpToDate = version == currentVersion
        } catch (e: Exception) {
            error = "Chyba při kontrole verze: ${e.localizedMessage}"
        }
    }

    fun installApk(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun downloadAndInstallApk(version: String) {
        coroutineScope.launch {
            downloading = true
            error = null
            downloadProgress = 0f
            try {
                val url = URL("https://github.com/toby-gamez/TobisoAppNative/releases/download/v$version/tobiso.apk")
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                if (connection.responseCode != 200) throw Exception("HTTP ${connection.responseCode}")
                val length = connection.contentLength
                val input: InputStream = connection.inputStream
                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "tobiso_update.apk")
                val output = FileOutputStream(apkFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalRead = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (length > 0) downloadProgress = totalRead.toFloat() / length
                }
                output.flush()
                output.close()
                input.close()
                downloading = false
                installApk(apkFile)
            } catch (e: Exception) {
                downloading = false
                error = "Chyba při stahování: ${e.localizedMessage}"
            }
        }
    }
    LargeTopAppBar(
        title = { Text("Aktualizátor") },
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
        if (error != null) {
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
                    if (downloading) {
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth(0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stahuji aktualizaci...")
                    } else {
                        Button(onClick = {
                            error = null
                            downloading = true
                            downloadProgress = 0f
                            downloadAndInstallApk(latestVersion!!)
                        }) {
                            Text("Stáhnout a nainstalovat")
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
