package com.tobiso.tobisoappnative.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tobiso.tobisoappnative.components.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlayerScreen(videoId: String, startSeconds: Int, navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Survives recreation (e.g. process death); rotation itself no longer recreates MainActivity.
    var resumeSeconds by rememberSaveable { mutableFloatStateOf(startSeconds.toFloat()) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var exitFullscreen by remember { mutableStateOf<(() -> Unit)?>(null) }
    val isFullscreen = fullscreenView != null

    fun setSystemBars(visible: Boolean) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    LaunchedEffect(isFullscreen) {
        setSystemBars(!isFullscreen)
        activity.requestedOrientation =
            if (isFullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    DisposableEffect(Unit) {
        onDispose {
            setSystemBars(true)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    BackHandler(enabled = isFullscreen) { exitFullscreen?.invoke() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (!isFullscreen) TopAppBar(
                    title = { Text("Přehrávač videa", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                        }
                    }
                )
            }
        ) { padding ->
            AndroidView(
                modifier = Modifier.fillMaxSize().padding(padding),
                factory = { ctx ->
                    YouTubePlayerView(ctx).apply {
                        // Manual init so the web player shows its own controls incl. the fullscreen button.
                        enableAutomaticInitialization = false
                        lifecycle.addObserver(this)
                        addFullscreenListener(object : FullscreenListener {
                            override fun onEnterFullscreen(view: View, exitAction: () -> Unit) {
                                exitFullscreen = exitAction
                                fullscreenView = view
                            }

                            override fun onExitFullscreen() {
                                fullscreenView = null
                                exitFullscreen = null
                            }
                        })
                        val listener = object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youTubePlayer.loadVideo(videoId, resumeSeconds)
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                resumeSeconds = second
                            }
                        }
                        val options = IFramePlayerOptions.Builder(ctx)
                            .controls(1)
                            .fullscreen(1)
                            .build()
                        initialize(listener, true, options)
                    }
                },
                onRelease = { it.release() }
            )
        }

        fullscreenView?.let { view ->
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        addView(view, FrameLayout.LayoutParams(-1, -1))
                    }
                },
                onRelease = { (it as FrameLayout).removeAllViews() }
            )
        }
    }
}
