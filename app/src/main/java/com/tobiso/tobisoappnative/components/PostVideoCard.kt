package com.tobiso.tobisoappnative.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.model.PostVideo
import com.tobiso.tobisoappnative.navigation.VideoPlayerRoute
import com.tobiso.tobisoappnative.navigation.YouTubePlayerRoute

private const val PREFS = "video_prefs"
private const val KEY_YOUTUBE_MODE = "youtube_mode"
private const val MODE_IN_APP = "in_app"
private const val MODE_EXTERNAL = "external"

private fun Context.openExternally(video: PostVideo) {
    val id = video.youtubeId
    val uri = (if (id != null) "https://www.youtube.com/watch?v=$id&t=${video.timestamp}s" else video.youtubeUrl).toUri()
    startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** "Video k článku" – YouTube videos let the user pick in-app player vs. the YouTube app (choice can be remembered). */
@Composable
fun PostVideoCard(video: PostVideo, navController: NavController, isOffline: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var showChoice by remember { mutableStateOf(false) }
    val youtubeId = video.youtubeId

    fun play(mode: String) {
        if (mode == MODE_IN_APP && youtubeId != null) {
            navController.navigate(YouTubePlayerRoute(youtubeId, video.timestamp))
        } else {
            context.openExternally(video)
        }
    }

    fun onPlayClicked() {
        when {
            youtubeId == null && video.youtubeUrl.endsWith(".mp4", ignoreCase = true) ->
                navController.navigate(VideoPlayerRoute(android.net.Uri.encode(video.youtubeUrl)))
            youtubeId == null -> context.openExternally(video)
            else -> when (val saved = prefs.getString(KEY_YOUTUBE_MODE, null)) {
                MODE_IN_APP, MODE_EXTERNAL -> play(saved)
                else -> showChoice = true
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Video k článku", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(video.label.ifBlank { "Video k tématu" }, style = MaterialTheme.typography.titleMedium)
                if (isOffline) Text("Dostupné pouze online", style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = ::onPlayClicked, enabled = !isOffline) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("Přehrát")
            }
        }
    }

    if (showChoice) {
        var remember by remember { mutableStateOf(false) }
        fun choose(mode: String) {
            if (remember) prefs.edit().putString(KEY_YOUTUBE_MODE, mode).apply()
            showChoice = false
            play(mode)
        }
        AlertDialog(
            onDismissRequest = { showChoice = false },
            title = { Text("Kde přehrát video?") },
            text = {
                Column {
                    Text("Video je z YouTube. Můžeš ho přehrát přímo v aplikaci, nebo otevřít v aplikaci YouTube.")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        Checkbox(checked = remember, onCheckedChange = { remember = it })
                        Text("Pamatovat si volbu")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choose(MODE_IN_APP) }) { Text("V aplikaci") } },
            dismissButton = { TextButton(onClick = { choose(MODE_EXTERNAL) }) { Text("V YouTube") } }
        )
    }
}
