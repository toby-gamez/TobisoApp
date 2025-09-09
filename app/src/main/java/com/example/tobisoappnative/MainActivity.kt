package com.example.tobisoappnative

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tobisoappnative.ui.theme.TobisoAppNativeTheme
import com.example.tobisoappnative.screens.HomeScreen
import com.example.tobisoappnative.screens.SearchScreen
import com.example.tobisoappnative.screens.MoreScreen
import com.example.tobisoappnative.navigation.BottomBar
import com.example.tobisoappnative.screens.CategoryListScreen
import com.example.tobisoappnative.screens.FeedbackScreen
import com.example.tobisoappnative.screens.AboutScreen
import com.example.tobisoappnative.screens.ChangelogScreen
import com.example.tobisoappnative.screens.FavoritesScreen
import com.example.tobisoappnative.screens.NoInternetScreen
import com.example.tobisoappnative.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import androidx.work.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.util.concurrent.TimeUnit
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenPointsOverlay
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyApp() {
        val context = androidx.compose.ui.platform.LocalContext.current
        val isConnected = remember { mutableStateOf(checkInternetConnection(context)) }
        val mainViewModel: MainViewModel = viewModel()
        val categories by mainViewModel.categories.collectAsState()
        val categoryError by mainViewModel.categoryError.collectAsState()
        val lastAddedPoints by PointsManager.lastAddedPoints.collectAsState()
        var showOverlay by remember { mutableStateOf(false) }
        var overlayPoints by remember { mutableStateOf(0) }
        var showTotalOverlay by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        // periodická kontrola připojení
        LaunchedEffect(context) {
            while (true) {
                isConnected.value = checkInternetConnection(context)
                delay(2000)
            }
        }

        // callback pro ruční obnovu
        val onRetry = {
            isConnected.value = checkInternetConnection(context)
        }

        // timeout stav
        val loadingTimeout = remember { mutableStateOf(false) }
        LaunchedEffect(isConnected.value) {
            if (isConnected.value) {
                loadingTimeout.value = false
            } else {
                loadingTimeout.value = false
                delay(30000)
                if (!isConnected.value && categories.isEmpty() && categoryError == null) {
                    loadingTimeout.value = true
                }
            }
        }

        // načtení kategorií při startu
        LaunchedEffect(Unit) {
            mainViewModel.loadCategories()
        }

        // Inicializace bodů při startu aplikace
        LaunchedEffect(context) {
            PointsManager.init(context)
        }
        val totalPoints by PointsManager.totalPoints.collectAsState()

        // Funkce pro zobrazení pouze celkového počtu bodů
        fun showTotalPointsOverlay() {
            showTotalOverlay = true
        }

        // LaunchedEffect pro první overlay (body s přičítáním)
        LaunchedEffect(lastAddedPoints) {
            if (lastAddedPoints != 0) {
                overlayPoints = lastAddedPoints
                showOverlay = true
                delay(2500)
                showOverlay = false
                PointsManager.resetLastAddedPoints()
            }
        }

        // LaunchedEffect pro druhý overlay (celkový počet bodů)
        LaunchedEffect(showTotalOverlay) {
            if (showTotalOverlay) {
                delay(2500)
                showTotalOverlay = false
            }
        }

        TobisoAppNativeTheme {
            // Nyní můžeme bezpečně přistoupit k MaterialTheme uvnitř TobisoAppNativeTheme
            val systemUiController = rememberSystemUiController()
            val surfaceColor = MaterialTheme.colorScheme.surface

            // nastavení status baru na surface
            SideEffect {
                systemUiController.setStatusBarColor(
                    color = surfaceColor,
                    darkIcons = surfaceColor.luminance() > 0.5f
                )
            }

            // Surface se surface barvou zajistí správné vykreslení pod status bar
            Surface(color = MaterialTheme.colorScheme.surface) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val searchRequestFocus = remember { mutableStateOf(false) }
                    if (!isConnected.value) {
                        NoInternetScreen(onRetry = onRetry)
                    } else if (categories.isEmpty() && categoryError == null) {
                        LoadingToast(timeout = loadingTimeout.value)
                    } else {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val route = navBackStackEntry?.destination?.route
                        Scaffold(
                            bottomBar = {
                                AnimatedVisibility(
                                    visible = (route == null ||
                                            !(route.startsWith("postDetail") ||
                                                    route.startsWith("about") ||
                                                    route.startsWith("feedback") ||
                                                    route.startsWith("changelog") ||
                                                    route.startsWith("videoPlayer") ||
                                                    route.startsWith("streak") ||
                                                    route.startsWith("favorites"))
                                    ) && !showTotalOverlay,
                                    exit = slideOutVertically(targetOffsetY = { it })
                                ) {
                                    BottomBar(navController, searchRequestFocus)
                                }
                            }
                        ) { paddingValues ->
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.padding(paddingValues)
                            ) {
                                composable("home") {
                                    HomeScreen(navController = navController)
                                }
                                composable("search") {
                                    SearchScreen(
                                        navController = navController,
                                        searchRequestFocus = searchRequestFocus
                                    )
                                }
                                composable("more") {
                                    MoreScreen(navController = navController)
                                }
                                composable(
                                    "feedback",
                                    enterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) {
                                    FeedbackScreen(navController = navController)
                                }
                                composable(
                                    "changelog",
                                    enterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) {
                                    ChangelogScreen(navController = navController)
                                }
                                composable(
                                    "about",
                                    enterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) {
                                    AboutScreen(navController = navController)
                                }
                                composable(
                                    "favorites",
                                    enterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) {
                                    FavoritesScreen(navController = navController)
                                }
                                composable("categoryList/{categoryName}") { backStackEntry ->
                                    val categoryName =
                                        backStackEntry.arguments?.getString("categoryName") ?: ""
                                    CategoryListScreen(
                                        parentCategoryName = categoryName,
                                        navController = navController
                                    )
                                }
                                composable(
                                    "postDetail/{postId}",
                                    enterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) { backStackEntry ->
                                    val postId =
                                        backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                                    if (postId != null) {
                                        com.example.tobisoappnative.screens.PostDetailScreen(
                                            postId = postId,
                                            navController = navController
                                        )
                                    } else {
                                        Text(
                                            "Chybný postId",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                composable(
                                    "videoPlayer/{videoUrl}",
                                    enterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) { backStackEntry ->
                                    val videoUrl =
                                        backStackEntry.arguments?.getString("videoUrl") ?: ""
                                    com.example.tobisoappnative.screens.VideoPlayerScreen(
                                        videoUrl = videoUrl,
                                        navController = navController
                                    )
                                }
                                composable(
                                    "streak",
                                    enterTransition = {
                                        slideInVertically(
                                            initialOffsetY = { -it }, // Přichází z vrchu (záporná hodnota)
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutVertically(
                                            targetOffsetY = { -it }, // Odchází nahoru (záporná hodnota)
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInVertically(
                                            initialOffsetY = { -it }, // Při návratu přichází z vrchu
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutVertically(
                                            targetOffsetY = { -it }, // Při návratu odchází nahoru
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) {
                                    com.example.tobisoappnative.screens.StreakScreen(navController = navController)
                                }
                            }
                        }
                    }

                    // OVERLAYS NA SPRÁVNÉM MÍSTĚ - PŘES CELOU OBRAZOVKU
                    if (showOverlay) {
                        FullScreenPointsOverlay(points = overlayPoints, totalPoints = totalPoints)
                    }
                    if (showTotalOverlay) {
                        FullScreenTotalPointsOverlay(totalPoints = totalPoints)
                    }
                }
            }
        }
    }

        @Composable
        fun LoadingToast(timeout: Boolean) {
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

        fun checkInternetConnection(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
