package com.example.tobisoappnative

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tobisoappnative.ui.theme.TobisoAppNativeTheme
import com.example.tobisoappnative.screens.HomeScreen
import com.example.tobisoappnative.screens.CalendarScreen
import com.example.tobisoappnative.screens.EventDetailScreen
import com.example.tobisoappnative.screens.ProfileScreen
import com.example.tobisoappnative.screens.BackpackScreen
import com.example.tobisoappnative.navigation.BottomBar
import com.example.tobisoappnative.screens.CategoryListScreen
import com.example.tobisoappnative.screens.FeedbackScreen
import com.example.tobisoappnative.screens.AboutScreen
import com.example.tobisoappnative.screens.ChangelogScreen
import com.example.tobisoappnative.screens.FavoritesScreen
import com.example.tobisoappnative.screens.NoInternetScreen
import com.example.tobisoappnative.screens.UpdaterScreen
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.viewmodel.MainIntent
import com.example.tobisoappnative.viewmodel.MainEffect
import com.example.tobisoappnative.viewmodel.tts.TtsViewModel
import kotlinx.coroutines.delay
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.tobisoappnative.components.FullScreenPointsOverlay
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.FullScreenMilestoneOverlay
import com.example.tobisoappnative.components.FullScreenAchievementOverlay
import androidx.compose.runtime.rememberCoroutineScope
import com.example.tobisoappnative.components.TtsPlayer
import com.example.tobisoappnative.screens.StreakScreen
import com.example.tobisoappnative.screens.ShopScreen
import com.example.tobisoappnative.screens.checkPointsAchievements
import com.example.tobisoappnative.utils.NetworkUtils
import com.example.tobisoappnative.components.FloatingSearchBar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TobisoApp(navigateTo: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isConnected = remember { mutableStateOf(NetworkUtils.isOnline(context)) }
    val mainViewModel: MainViewModel = viewModel()
    val ttsViewModel: TtsViewModel = viewModel()
    val mainState by mainViewModel.uiState.collectAsState()
    val categories = mainState.categories
    val categoryError = mainState.categoryError
    val isOffline = mainState.isOffline
    val hasUserDismissedNoInternet = mainState.hasUserDismissedNoInternet
    val lastAddedPoints by PointsManager.lastAddedPoints.collectAsState()
    val lastMilestone by PointsManager.lastMilestone.collectAsState()
    val lastAchievement by PointsManager.lastAchievement.collectAsState()
    var showOverlay by remember { mutableStateOf(false) }
    var overlayPoints by remember { mutableStateOf(0) }
    var showTotalOverlay by remember { mutableStateOf(false) }
    var showMilestoneOverlay by remember { mutableStateOf(false) }
    var showAchievementOverlay by remember { mutableStateOf(false) }
    var milestoneDay by remember { mutableStateOf(0) }
    var achievementPoints by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // periodická kontrola připojení
    LaunchedEffect(context) {
        while (true) {
            isConnected.value = NetworkUtils.isOnline(context)
            delay(2000)
        }
    }

    // callback pro ruční obnovu
    val onRetry = {
        isConnected.value = NetworkUtils.isOnline(context)
        if (isConnected.value) {
            mainViewModel.onIntent(MainIntent.ResetNoInternetDismiss)
        }
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
    val totalPoints by PointsManager.totalPoints.collectAsState()

    // Funkce pro zobrazení pouze celkového počtu bodů
    fun showTotalPointsOverlay() {
        showTotalOverlay = true
    }

    // LaunchedEffect pro milník overlay (priorita)
    LaunchedEffect(lastMilestone) {
        if (lastMilestone != null) {
            println("=== MILESTONE OVERLAY DEBUG ===")
            println("Milestone detected: $lastMilestone days")
            println("Points to show: $lastAddedPoints")
            println("Total points: $totalPoints")

            overlayPoints = lastAddedPoints
            milestoneDay = lastMilestone!!
            showMilestoneOverlay = true
            delay(3000) // Delší zobrazení pro milník
            showMilestoneOverlay = false
            PointsManager.resetLastAddedPoints()

            println("Milestone overlay finished and reset")
            println("=== END MILESTONE OVERLAY DEBUG ===")
        }
    }

    // LaunchedEffect pro achievement overlay (priorita)
    LaunchedEffect(lastAchievement) {
        if (lastAchievement != null) {
            println("=== ACHIEVEMENT OVERLAY DEBUG ===")
            println("Achievement detected: $lastAchievement points")
            println("Points to show: $lastAddedPoints")
            println("Total points: $totalPoints")

            overlayPoints = lastAddedPoints
            achievementPoints = lastAchievement!!
            showAchievementOverlay = true
            delay(3000) // Delší zobrazení pro achievement
            showAchievementOverlay = false
            PointsManager.resetLastAddedPoints()

            println("Achievement overlay finished and reset")
            println("=== END ACHIEVEMENT OVERLAY DEBUG ===")
        }
    }

    // LaunchedEffect pro běžný overlay (body s přičítáním)
    LaunchedEffect(lastAddedPoints) {
        if (lastAddedPoints != 0 && lastMilestone == null && lastAchievement == null) {
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

    // LaunchedEffect pro milník overlay
    LaunchedEffect(showMilestoneOverlay) {
        if (showMilestoneOverlay) {
            delay(3000)
            showMilestoneOverlay = false
        }
    }

    TobisoAppNativeTheme {
        val systemUiController = rememberSystemUiController()
        val surfaceColor = MaterialTheme.colorScheme.surface

        SideEffect {
            systemUiController.setStatusBarColor(
                color = surfaceColor,
                darkIcons = surfaceColor.luminance() > 0.5f
            )
        }

        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                val searchRequestFocus = remember { mutableStateOf(false) }
                if (!isConnected.value && !hasUserDismissedNoInternet) {
                    NoInternetScreen(
                        onRetry = onRetry,
                        onOfflineMode = {
                            mainViewModel.onIntent(MainIntent.EnableOfflineMode)
                            mainViewModel.onIntent(MainIntent.ConfirmOfflineModeTransition)
                        }
                    )
                } else if (categories.isEmpty() && categoryError == null && !isOffline) {
                    AppLoadingToast(timeout = loadingTimeout.value)
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
                                                route.startsWith("favorites") ||
                                                route.startsWith("updater") ||
                                                route.startsWith("questions") ||
                                                route.startsWith("mixedQuiz") ||
                                                route.startsWith("eventDetail") ||
                                                route.startsWith("shop") ||
                                                route.startsWith("backpack") ||
                                                route.startsWith("plainText") ||
                                                route.startsWith("exerciseTimeline") ||
                                                route.startsWith("exerciseDragDrop") ||
                                                route.startsWith("exerciseMatching") ||
                                                route.startsWith("exerciseCircuit") ||
                                                route.startsWith("offlineManager") ||
                                                route.startsWith("aiChat"))
                                        ),
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(durationMillis = 250, delayMillis = 100)
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(durationMillis = 200, delayMillis = 0)
                                )
                            ) {
                                BottomBar(navController, searchRequestFocus)
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = if (navigateTo == "calendar") "calendar" else "home",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("home") {
                                HomeScreen(navController = navController)
                            }
                            composable("calendar") {
                                CalendarScreen(navController = navController)
                            }
                            composable("calendar/{year}/{month}") { backStackEntry ->
                                val year = backStackEntry.arguments?.getString("year")?.toIntOrNull()
                                val month = backStackEntry.arguments?.getString("month")?.toIntOrNull()
                                CalendarScreen(
                                    navController = navController,
                                    initialYear = year,
                                    initialMonth = month
                                )
                            }
                            composable("profile") {
                                ProfileScreen(navController = navController)
                            }
                            composable(
                                "feedback",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                FeedbackScreen(navController = navController)
                            }
                            composable(
                                "changelog",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                ChangelogScreen(navController = navController)
                            }
                            composable(
                                "updater",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                UpdaterScreen(navController = navController)
                            }
                            composable(
                                "offlineManager",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                com.example.tobisoappnative.screens.OfflineManagerScreen(navController = navController)
                            }
                            composable(
                                "about",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                AboutScreen(navController = navController)
                            }
                            composable(
                                "favorites",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                FavoritesScreen(navController = navController)
                            }
                            composable(
                                "categoryList/{categoryName}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                                CategoryListScreen(
                                    parentCategoryName = categoryName,
                                    navController = navController
                                )
                            }
                            composable(
                                "postDetail/{postId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                                if (postId != null) {
                                    com.example.tobisoappnative.screens.PostDetailScreen(
                                        postId = postId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný postId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "exerciseTimeline/{exerciseId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toIntOrNull()
                                if (exerciseId != null) {
                                    com.example.tobisoappnative.screens.TimelineExerciseScreen(
                                        exerciseId = exerciseId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný exerciseId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "exerciseDragDrop/{exerciseId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toIntOrNull()
                                if (exerciseId != null) {
                                    com.example.tobisoappnative.screens.DragDropExerciseScreen(
                                        exerciseId = exerciseId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný exerciseId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "exerciseMatching/{exerciseId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toIntOrNull()
                                if (exerciseId != null) {
                                    com.example.tobisoappnative.screens.MatchingExerciseScreen(
                                        exerciseId = exerciseId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný exerciseId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "exerciseCircuit/{exerciseId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toIntOrNull()
                                if (exerciseId != null) {
                                    com.example.tobisoappnative.screens.CircuitExerciseScreen(
                                        exerciseId = exerciseId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný exerciseId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "plainText/{postId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                                if (postId != null) {
                                    com.example.tobisoappnative.screens.PlainTextScreen(
                                        postId = postId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný postId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "videoPlayer/{videoUrl}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                                com.example.tobisoappnative.screens.VideoPlayerScreen(
                                    videoUrl = videoUrl,
                                    navController = navController
                                )
                            }
                            composable(
                                "streak",
                                enterTransition = { slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400)) }
                            ) {
                                StreakScreen(navController = navController)
                            }
                            composable(
                                "questions/{postId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                                if (postId != null) {
                                    com.example.tobisoappnative.screens.QuestionsScreen(
                                        postId = postId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný postId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable("allQuestions") {
                                com.example.tobisoappnative.screens.AllQuestionsScreen(
                                    navController = navController
                                )
                            }
                            composable(
                                "mixedQuiz/{questionIds}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val questionIds = backStackEntry.arguments?.getString("questionIds") ?: ""
                                com.example.tobisoappnative.screens.MixedQuizScreen(
                                    questionIds = questionIds,
                                    navController = navController
                                )
                            }
                            composable(
                                "eventDetail/{eventId}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val eventId = backStackEntry.arguments?.getString("eventId")?.toIntOrNull()
                                if (eventId != null) {
                                    EventDetailScreen(
                                        eventId = eventId,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybné ID události", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            composable(
                                "shop",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                ShopScreen(navController = navController)
                            }
                            composable(
                                "backpack",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                BackpackScreen(navController = navController)
                            }
                            composable(
                                "aiChat/{postId}/{postTitle}/{firstUserMessage}",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                                val postTitle = backStackEntry.arguments?.getString("postTitle") ?: ""
                                val firstUserMessage = backStackEntry.arguments?.getString("firstUserMessage") ?: ""
                                if (postId != null) {
                                    com.example.tobisoappnative.screens.AiChatScreen(
                                        postId = postId,
                                        postTitle = postTitle,
                                        firstUserMessage = firstUserMessage,
                                        navController = navController
                                    )
                                } else {
                                    Text("Chybný postId", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    // Persistent TTS player shown above other content (bottom-aligned).
                    val ttsManagerInstance = ttsViewModel.ttsManager
                    val bottomBarVisible = (route == null ||
                        !(route.startsWith("postDetail") ||
                            route.startsWith("about") ||
                            route.startsWith("feedback") ||
                            route.startsWith("changelog") ||
                            route.startsWith("videoPlayer") ||
                            route.startsWith("streak") ||
                            route.startsWith("favorites") ||
                            route.startsWith("updater") ||
                            route.startsWith("questions") ||
                            route.startsWith("mixedQuiz") ||
                            route.startsWith("eventDetail") ||
                            route.startsWith("shop") ||
                            route.startsWith("backpack") ||
                            route.startsWith("plainText") ||
                            route.startsWith("exerciseTimeline") ||
                            route.startsWith("exerciseDragDrop") ||
                            route.startsWith("exerciseMatching") ||
                            route.startsWith("exerciseCircuit") ||
                            route.startsWith("aiChat")
                        )
                    )

                    if (ttsManagerInstance != null) {
                        val raiseBy = 63.dp
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

                    // Floating Search Bar
                    val currentRoute = navBackStackEntry?.destination?.route
                    val showFloatingSearch = currentRoute?.startsWith("home") == true ||
                            currentRoute?.startsWith("allQuestions") == true ||
                            currentRoute?.startsWith("calendar") == true ||
                            currentRoute?.startsWith("profile") == true ||
                            currentRoute?.startsWith("categoryList/") == true

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showFloatingSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        enter = androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ) + androidx.compose.animation.slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        ) + androidx.compose.animation.slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = androidx.compose.animation.core.tween(200)
                        )
                    ) {
                        FloatingSearchBar(
                            navController = navController,
                            viewModel = mainViewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (bottomBarVisible) 100.dp else 16.dp),
                            isOffline = !isConnected.value,
                            onAiSend = { post, message ->
                                navController.navigate(
                                    "aiChat/${post.id}/${android.net.Uri.encode(post.title)}/${android.net.Uri.encode(message)}"
                                )
                            }
                        )
                    }
                }

                // Overlays pro body, milníky a achievementy
                if (showMilestoneOverlay) {
                    FullScreenMilestoneOverlay(
                        points = overlayPoints,
                        totalPoints = totalPoints,
                        milestoneDay = milestoneDay
                    )
                } else if (showAchievementOverlay) {
                    FullScreenAchievementOverlay(
                        points = overlayPoints,
                        totalPoints = totalPoints,
                        achievementPoints = achievementPoints
                    )
                } else if (showOverlay) {
                    FullScreenPointsOverlay(
                        points = overlayPoints,
                        totalPoints = totalPoints
                    )
                } else if (showTotalOverlay) {
                    FullScreenTotalPointsOverlay(totalPoints = totalPoints)
                }
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
