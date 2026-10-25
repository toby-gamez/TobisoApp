package com.tobiso.tobisoappnative

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.tobiso.tobisoappnative.navigation.BOTTOM_BAR_ROUTES
import com.tobiso.tobisoappnative.navigation.AuthRoute
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.navigation.AiChatRoute
import com.tobiso.tobisoappnative.navigation.CalendarRoute
import com.tobiso.tobisoappnative.navigation.HomeRoute
import com.tobiso.tobisoappnative.ui.theme.TobisoAppNativeTheme
import com.tobiso.tobisoappnative.navigation.BottomBar
import com.tobiso.tobisoappnative.viewmodel.MainViewModel
import com.tobiso.tobisoappnative.viewmodel.MainIntent
import com.tobiso.tobisoappnative.viewmodel.MainEffect
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.tobiso.tobisoappnative.components.TtsPlayer
import com.tobiso.tobisoappnative.utils.checkPointsAchievements
import com.tobiso.tobisoappnative.navigation.AppNavHost
import com.tobiso.tobisoappnative.components.PointsOverlayHost

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TobisoApp(navigateTo: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainViewModel: MainViewModel = hiltViewModel()
    val isConnected by mainViewModel.connectivityFlow.collectAsStateWithLifecycle(initialValue = false)
    val ttsViewModel: TtsViewModel = hiltViewModel()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // načtení kategorií při startu
    LaunchedEffect(Unit) {
        mainViewModel.onIntent(MainIntent.LoadCategories)
    }

    // Zpracování one-shot efektů z MainViewModel
    LaunchedEffect(Unit) {
        mainViewModel.effect.collect { effect ->
            when (effect) {
                is MainEffect.ShowToast -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Kontrola milníků a achievementů při startu aplikace (po inicializaci UI)
    LaunchedEffect(context) {
        StreakMilestoneManager.checkStreakMilestones(context)
        checkPointsAchievements(context)
    }
    TobisoAppNativeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                val searchRequestFocus = remember { mutableStateOf(false) }
                val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val destination = navBackStackEntry?.destination
                    val showBottomBar = destination == null ||
                        BOTTOM_BAR_ROUTES.any { destination.hasRoute(it) }
                    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                    val sessionExpired by AuthSession.sessionExpired.collectAsStateWithLifecycle()
                    LaunchedEffect(sessionExpired) {
                        if (sessionExpired) {
                            val result = snackbarHostState.showSnackbar(
                                message = "Přihlášení vypršelo. Pokračuješ jako host.",
                                actionLabel = "Přihlásit se",
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                            AuthSession.consumeExpired()
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                navController.navigate(AuthRoute)
                            }
                        }
                    }
                    Scaffold(
                        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            AnimatedVisibility(
                                visible = showBottomBar,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                                ) + androidx.compose.animation.fadeIn(tween(250, delayMillis = 80)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(durationMillis = 200, delayMillis = 0)
                                )
                            ) {
                                BottomBar(navController, searchRequestFocus)
                            }
                        }
                    ) { paddingValues ->
                        AppNavHost(
                            navController = navController,
                            startDestination = if (navigateTo == "calendar") CalendarRoute else HomeRoute,
                            ttsViewModel = ttsViewModel,
                            modifier = Modifier
                                .padding(paddingValues)
                                .consumeWindowInsets(paddingValues)
                                .imePadding()
                        )
                    }
                    // Persistent TTS player shown above other content (bottom-aligned).
                    val ttsManagerInstance = ttsViewModel.ttsManager
                    val bottomBarVisible = showBottomBar

                    if (ttsManagerInstance != null) {
                        val raiseBy = 100.dp
                        val bottomPadding = if (bottomBarVisible) 72.dp else 12.dp
                        val adjustedBottom = bottomPadding + raiseBy
                        TtsPlayer(
                            ttsManager = ttsManagerInstance,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(start = 8.dp, end = 8.dp, bottom = adjustedBottom)
                        )
                    }

                PointsOverlayHost()
            }
        }
    }
}

@Composable
fun AppLoadingToast(timeout: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (timeout) {
                Text(
                    text = "Načítání trvá příliš dlouho...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Zkontrolujte připojení k internetu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Načítání...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
