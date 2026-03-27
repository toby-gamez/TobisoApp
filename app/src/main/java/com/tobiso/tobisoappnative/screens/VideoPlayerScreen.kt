package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import android.content.res.Configuration

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(videoUrl: String, navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    var isFullscreen by remember { mutableStateOf(false) }
    var originalOrientation by remember { mutableStateOf<Int?>(null) }

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val decodedVideoUrl = Uri.decode(videoUrl)

    // Převod na HTTPS a logování
    val secureVideoUrl = when {
        decodedVideoUrl.startsWith("https://www.tobiso.com") -> decodedVideoUrl
        decodedVideoUrl.startsWith("http://www.tobiso.com") -> decodedVideoUrl.replaceFirst("http://", "https://")
        decodedVideoUrl.startsWith("https://tobiso.com") -> decodedVideoUrl.replaceFirst("https://tobiso.com", "https://www.tobiso.com")
        decodedVideoUrl.startsWith("http://tobiso.com") -> decodedVideoUrl.replaceFirst("http://tobiso.com", "https://www.tobiso.com")
        decodedVideoUrl.startsWith("//") -> "https://www.tobiso.com" + decodedVideoUrl.removePrefix("//tobiso.com").removePrefix("//www.tobiso.com")
        decodedVideoUrl.startsWith("/") -> "https://www.tobiso.com$decodedVideoUrl"
        else -> "https://www.tobiso.com/$decodedVideoUrl"
    }
    LaunchedEffect(videoUrl) {
        Log.d("VideoPlayer", "Původní videoUrl: $videoUrl")
        Log.d("VideoPlayer", "Decoded videoUrl: $decodedVideoUrl")
        Log.d("VideoPlayer", "Výsledné secureVideoUrl: $secureVideoUrl")
    }

    // Pokud secureVideoUrl není HTTPS, zobraz chybu a nezkoušej přehrát
    if (!secureVideoUrl.startsWith("https://")) {
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
                Text("URL videa není bezpečné (musí být HTTPS)", color = MaterialTheme.colorScheme.error)
            }
        }
        return
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
    // Funkce pro přepínání fullscreen režimu
    fun toggleFullscreen() {
        val window = activity.window
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        
        if (!isFullscreen) {
            // Vstup do fullscreen režimu
            isFullscreen = true
            
            // Uložit původní orientaci pouze při prvním vstupu do fullscreen
            if (originalOrientation == null) {
                originalOrientation = activity.requestedOrientation
            }
            
            // Pokud nejsme v landscape, přepni na landscape
            if (configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            
            // Skrytí system bars pomocí moderního API
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Výstup z fullscreen režimu
            isFullscreen = false
            
            // Zobrazení system bars
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            
            // Obnovení původní orientace nebo výchozí portrait
            activity.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            originalOrientation = null
        }
    }
    
    // Obnovení orientace při opuštění obrazovky
    DisposableEffect(Unit) {
        onDispose {
            if (isFullscreen) {
                val window = activity.window
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                
                // Obnovení původní orientace nebo výchozí portrait
                activity.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    var playerErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    val exoPlayer = remember {
        Log.d("VideoPlayer", "Přehrávám video z URL: $secureVideoUrl")
        if (!secureVideoUrl.startsWith("https://")) {
            Log.e("VideoPlayer", "Chyba: secureVideoUrl není HTTPS: $secureVideoUrl")
        }
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
        Log.d("VideoPlayer", "Vytvářím MediaItem z: $secureVideoUrl")
        val mediaItem = MediaItem.fromUri(secureVideoUrl.toUri())
        Log.d("VideoPlayer", "MediaItem URI: ${mediaItem.localConfiguration?.uri}")
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

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (isFullscreen) {
        // Fullscreen režim - pouze video přehrávač
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Tlačítko pro výstup z fullscreen v rohu
            IconButton(
                onClick = { toggleFullscreen() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.FullscreenExit,
                    contentDescription = "Ukončit fullscreen",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        // Normální režim se Scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Přehrávač videa", style = MaterialTheme.typography.headlineLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    actions = {
                        // Zobrazení aktivního multiplikátoru
                        MultiplierIndicator()
                        IconButton(onClick = { toggleFullscreen() }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
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
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (isLandscape) {
                                // V landscape režimu větší video i bez fullscreen
                                (configuration.screenHeightDp * 0.6).dp
                            } else {
                                240.dp
                            }
                        )
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
}
