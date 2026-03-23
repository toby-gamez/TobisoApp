package com.tobiso.tobisoappnative.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.viewmodel.updater.ReleaseInfo
import com.tobiso.tobisoappnative.viewmodel.updater.UpdaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val vm: UpdaterViewModel = hiltViewModel()

    val currentVersion by vm.currentVersion.collectAsState()
    val latestVersion by vm.latestVersion.collectAsState()
    val releaseInfo by vm.releaseInfo.collectAsState()
    val isUpToDate by vm.isUpToDate.collectAsState()
    val error by vm.error.collectAsState()
    val isOfflineMode by vm.isOfflineMode.collectAsState()

    // Create local non-delegated copies so Kotlin can smart-cast nullability
    val currentVersionVal = currentVersion
    val latestVersionVal = latestVersion
    val isUpToDateVal = isUpToDate

    LaunchedEffect(Unit) {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
        vm.init(packageInfo?.versionName ?: "?")
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
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            } else if (latestVersion == null) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Vaše verze: $currentVersion", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isUpToDateVal == true) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Aktuální", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Aplikace je aktuální.", color = MaterialTheme.colorScheme.tertiary)
                    } else if (latestVersionVal != null && currentVersionVal > latestVersionVal) {
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
                                text = "Dostupná nová verze: $latestVersionVal",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            releaseInfo?.let { info ->
                                ReleaseInfoCard(info)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { latestVersionVal?.let { openDownloadPage(it) } },
                                enabled = latestVersionVal != null,
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

@Composable
private fun ReleaseInfoCard(info: ReleaseInfo) {
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
