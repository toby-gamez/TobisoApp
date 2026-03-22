package com.tobiso.tobisoappnative
import timber.log.Timber

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import com.tobiso.tobisoappnative.navigation.AiChatRoute
import com.tobiso.tobisoappnative.navigation.AllQuestionsRoute
import com.tobiso.tobisoappnative.navigation.BackpackRoute
import com.tobiso.tobisoappnative.navigation.CalendarRoute
import com.tobiso.tobisoappnative.navigation.CalendarWithDateRoute
import com.tobiso.tobisoappnative.navigation.CategoryListRoute
import com.tobiso.tobisoappnative.navigation.ChangelogRoute
import com.tobiso.tobisoappnative.navigation.EventDetailRoute
import com.tobiso.tobisoappnative.navigation.ExerciseCircuitRoute
import com.tobiso.tobisoappnative.navigation.ExerciseDragDropRoute
import com.tobiso.tobisoappnative.navigation.ExerciseMatchingRoute
import com.tobiso.tobisoappnative.navigation.ExerciseTimelineRoute
import com.tobiso.tobisoappnative.navigation.FavoritesRoute
import com.tobiso.tobisoappnative.navigation.FeedbackRoute
import com.tobiso.tobisoappnative.navigation.HomeRoute
import com.tobiso.tobisoappnative.navigation.MixedQuizRoute
import com.tobiso.tobisoappnative.navigation.OfflineManagerRoute
import com.tobiso.tobisoappnative.navigation.PlainTextRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
import com.tobiso.tobisoappnative.navigation.ProfileRoute
import com.tobiso.tobisoappnative.navigation.QuestionsRoute
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute
import com.tobiso.tobisoappnative.navigation.UpdaterRoute
import com.tobiso.tobisoappnative.navigation.VideoPlayerRoute
import com.tobiso.tobisoappnative.navigation.AboutRoute
import com.tobiso.tobisoappnative.ui.theme.TobisoAppNativeTheme
import com.tobiso.tobisoappnative.screens.HomeScreen
import com.tobiso.tobisoappnative.screens.CalendarScreen
import com.tobiso.tobisoappnative.screens.EventDetailScreen
import com.tobiso.tobisoappnative.screens.ProfileScreen
import com.tobiso.tobisoappnative.screens.BackpackScreen
import com.tobiso.tobisoappnative.navigation.BottomBar
import com.tobiso.tobisoappnative.screens.CategoryListScreen
import com.tobiso.tobisoappnative.screens.FeedbackScreen
import com.tobiso.tobisoappnative.screens.AboutScreen
import com.tobiso.tobisoappnative.screens.ChangelogScreen
import com.tobiso.tobisoappnative.screens.FavoritesScreen
import com.tobiso.tobisoappnative.screens.NoInternetScreen
import com.tobiso.tobisoappnative.screens.UpdaterScreen
import com.tobiso.tobisoappnative.viewmodel.MainViewModel
import com.tobiso.tobisoappnative.viewmodel.MainIntent
import com.tobiso.tobisoappnative.viewmodel.MainEffect
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import kotlinx.coroutines.delay
import com.tobiso.tobisoappnative.components.FullScreenPointsOverlay
import com.tobiso.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.tobiso.tobisoappnative.components.FullScreenMilestoneOverlay
import com.tobiso.tobisoappnative.components.FullScreenAchievementOverlay
import androidx.compose.runtime.rememberCoroutineScope
import com.tobiso.tobisoappnative.components.TtsPlayer
import com.tobiso.tobisoappnative.screens.StreakScreen
import com.tobiso.tobisoappnative.screens.ShopScreen
import com.tobiso.tobisoappnative.utils.checkPointsAchievements
import com.tobiso.tobisoappnative.utils.NetworkUtils
import com.tobiso.tobisoappnative.components.FloatingSearchBar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TobisoApp(navigateTo: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isConnected by NetworkUtils.observeConnectivityAsFlow(context)
        .collectAsState(initial = NetworkUtils.isOnline(context))
    val mainViewModel: MainViewModel = hiltViewModel()
    val ttsViewModel: TtsViewModel = hiltViewModel()
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

    // callback pro ruční obnovu
    val onRetry = {
        if (NetworkUtils.isOnline(context)) {
            mainViewModel.onIntent(MainIntent.ResetNoInternetDismiss)
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
            Timber.d("=== MILESTONE OVERLAY DEBUG ===")
            Timber.d("Milestone detected: $lastMilestone days")
            Timber.d("Points to show: $lastAddedPoints")
            Timber.d("Total points: $totalPoints")

            overlayPoints = lastAddedPoints
            milestoneDay = lastMilestone!!
            showMilestoneOverlay = true
            delay(3000) // Delší zobrazení pro milník
            showMilestoneOverlay = false
            PointsManager.resetLastAddedPoints()

            Timber.d("Milestone overlay finished and reset")
            Timber.d("=== END MILESTONE OVERLAY DEBUG ===")
        }
    }

    // LaunchedEffect pro achievement overlay (priorita)
    LaunchedEffect(lastAchievement) {
        if (lastAchievement != null) {
            Timber.d("=== ACHIEVEMENT OVERLAY DEBUG ===")
            Timber.d("Achievement detected: $lastAchievement points")
            Timber.d("Points to show: $lastAddedPoints")
            Timber.d("Total points: $totalPoints")

            overlayPoints = lastAddedPoints
            achievementPoints = lastAchievement!!
            showAchievementOverlay = true
            delay(3000) // Delší zobrazení pro achievement
            showAchievementOverlay = false
            PointsManager.resetLastAddedPoints()

            Timber.d("Achievement overlay finished and reset")
            Timber.d("=== END ACHIEVEMENT OVERLAY DEBUG ===")
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
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                val searchRequestFocus = remember { mutableStateOf(false) }
                if (!isConnected && !hasUserDismissedNoInternet) {
                    NoInternetScreen(
                        onRetry = onRetry,
                        onOfflineMode = {
                            mainViewModel.onIntent(MainIntent.EnableOfflineMode)
                            mainViewModel.onIntent(MainIntent.ConfirmOfflineModeTransition)
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val destination = navBackStackEntry?.destination
                    val showBottomBar = destination == null ||
                        destination.hasRoute(HomeRoute::class) ||
                        destination.hasRoute(AllQuestionsRoute::class) ||
                        destination.hasRoute(CalendarRoute::class) ||
                        destination.hasRoute(CalendarWithDateRoute::class) ||
                        destination.hasRoute(ProfileRoute::class) ||
                        destination.hasRoute(CategoryListRoute::class)
                    Scaffold(
                        bottomBar = {
                            AnimatedVisibility(
                                visible = showBottomBar,
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
                            startDestination = if (navigateTo == "calendar") CalendarRoute else HomeRoute,
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(navController = navController)
                            }
                            composable<CalendarRoute> {
                                CalendarScreen(navController = navController)
                            }
                            composable<CalendarWithDateRoute> { backStackEntry ->
                                val route: CalendarWithDateRoute = backStackEntry.toRoute()
                                CalendarScreen(
                                    navController = navController,
                                    initialYear = route.year,
                                    initialMonth = route.month
                                )
                            }
                            composable<ProfileRoute> {
                                ProfileScreen(navController = navController)
                            }
                            composable<FeedbackRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                FeedbackScreen(navController = navController)
                            }
                            composable<ChangelogRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                ChangelogScreen(navController = navController)
                            }
                            composable<UpdaterRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                UpdaterScreen(navController = navController)
                            }
                            composable<OfflineManagerRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                com.tobiso.tobisoappnative.screens.OfflineManagerScreen(navController = navController)
                            }
                            composable<AboutRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                AboutScreen(navController = navController)
                            }
                            composable<FavoritesRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                FavoritesScreen(navController = navController)
                            }
                            composable<CategoryListRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: CategoryListRoute = backStackEntry.toRoute()
                                CategoryListScreen(
                                    parentCategoryName = route.categoryName,
                                    navController = navController
                                )
                            }
                            composable<PostDetailRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: PostDetailRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.PostDetailScreen(
                                    postId = route.postId,
                                    navController = navController
                                )
                            }
                            composable<ExerciseTimelineRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: ExerciseTimelineRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.TimelineExerciseScreen(
                                    exerciseId = route.exerciseId,
                                    navController = navController
                                )
                            }
                            composable<ExerciseDragDropRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: ExerciseDragDropRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.DragDropExerciseScreen(
                                    exerciseId = route.exerciseId,
                                    navController = navController
                                )
                            }
                            composable<ExerciseMatchingRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: ExerciseMatchingRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.MatchingExerciseScreen(
                                    exerciseId = route.exerciseId,
                                    navController = navController
                                )
                            }
                            composable<ExerciseCircuitRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: ExerciseCircuitRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.CircuitExerciseScreen(
                                    exerciseId = route.exerciseId,
                                    navController = navController
                                )
                            }
                            composable<PlainTextRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: PlainTextRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.PlainTextScreen(
                                    postId = route.postId,
                                    navController = navController
                                )
                            }
                            composable<VideoPlayerRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: VideoPlayerRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.VideoPlayerScreen(
                                    videoUrl = route.videoUrl,
                                    navController = navController
                                )
                            }
                            composable<StreakRoute>(
                                enterTransition = { slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400)) }
                            ) {
                                StreakScreen(navController = navController)
                            }
                            composable<QuestionsRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: QuestionsRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.QuestionsScreen(
                                    postId = route.postId,
                                    navController = navController
                                )
                            }
                            composable<AllQuestionsRoute> {
                                com.tobiso.tobisoappnative.screens.AllQuestionsScreen(
                                    navController = navController
                                )
                            }
                            composable<MixedQuizRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: MixedQuizRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.MixedQuizScreen(
                                    questionIds = route.questionIds,
                                    navController = navController
                                )
                            }
                            composable<EventDetailRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: EventDetailRoute = backStackEntry.toRoute()
                                EventDetailScreen(
                                    eventId = route.eventId,
                                    navController = navController
                                )
                            }
                            composable<ShopRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                ShopScreen(navController = navController)
                            }
                            composable<BackpackRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) {
                                BackpackScreen(navController = navController)
                            }
                            composable<AiChatRoute>(
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val route: AiChatRoute = backStackEntry.toRoute()
                                com.tobiso.tobisoappnative.screens.AiChatScreen(
                                    postId = route.postId,
                                    postTitle = route.postTitle,
                                    firstUserMessage = route.firstUserMessage,
                                    navController = navController
                                )
                            }
                        }
                    }
                    // Persistent TTS player shown above other content (bottom-aligned).
                    val ttsManagerInstance = ttsViewModel.ttsManager
                    val bottomBarVisible = showBottomBar

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
                    val showFloatingSearch = destination?.hasRoute(HomeRoute::class) == true ||
                            destination?.hasRoute(AllQuestionsRoute::class) == true ||
                            destination?.hasRoute(CalendarRoute::class) == true ||
                            destination?.hasRoute(CalendarWithDateRoute::class) == true ||
                            destination?.hasRoute(ProfileRoute::class) == true ||
                            destination?.hasRoute(CategoryListRoute::class) == true

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
                            isOffline = !isConnected,
                            onAiSend = { post, message ->
                                navController.navigate(
                                    AiChatRoute(
                                        postId = post.id,
                                        postTitle = post.title,
                                        firstUserMessage = message
                                    )
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
