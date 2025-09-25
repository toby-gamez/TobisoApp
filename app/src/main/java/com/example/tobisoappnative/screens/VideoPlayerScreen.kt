package com.example.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.Player
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import android.net.Uri

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(videoUrl: String, navController: NavController) {
    val context = LocalContext.current
    val decodedVideoUrl = Uri.decode(videoUrl)
    
    // Ujisti se, že URL je HTTPS pro bezpečnost
    val secureVideoUrl = when {
        decodedVideoUrl.startsWith("http://") -> decodedVideoUrl.replace("http://", "https://")
        decodedVideoUrl.startsWith("https://") -> decodedVideoUrl
        decodedVideoUrl.startsWith("//") -> "https:$decodedVideoUrl"
        decodedVideoUrl.startsWith("/") -> "https://www.tobiso.com$decodedVideoUrl"
        else -> "https://www.tobiso.com/$decodedVideoUrl"
    }
    
    if (videoUrl.isBlank()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Přehrávač videa") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("URL videa není zadána", color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }
    var playerErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    val exoPlayer = remember {
        Log.d("VideoPlayer", "Přehrávám video z URL: $secureVideoUrl")
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("TobisoApp-Android/2.0.1")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setDefaultRequestProperties(mapOf(
                "Accept" to "video/*,*/*;q=0.1",
                "Accept-Encoding" to "identity",
                "Connection" to "keep-alive",
                "User-Agent" to "TobisoApp-Android/2.0.1"
            ))
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaItem = MediaItem.fromUri(secureVideoUrl.toUri())
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            )
            .build().apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        val errorMsg = buildString {
                            append("${error.message}\nKód chyby: ${error.errorCodeName} (${error.errorCode})")
                            error.cause?.let {
                                append("\nPříčina chyby: $it")
                            }
                        }
                        Log.e("VideoPlayer", errorMsg)
                        Log.e("VideoPlayer", "Stacktrace:", error)
                        playerErrors = playerErrors + errorMsg
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> Log.d("VideoPlayer", "Stav: BUFFERING")
                            Player.STATE_READY -> Log.d("VideoPlayer", "Stav: READY")
                            Player.STATE_ENDED -> Log.d("VideoPlayer", "Stav: ENDED")
                            Player.STATE_IDLE -> Log.d("VideoPlayer", "Stav: IDLE")
                        }
                    }
                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        Log.d("VideoPlayer", "isLoading: $isLoading")
                    }
                })
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Přehrávač videa") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
            
            if (playerErrors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                playerErrors.forEach { err ->
                    Text(
                        text = "Chyba videa: $err",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
