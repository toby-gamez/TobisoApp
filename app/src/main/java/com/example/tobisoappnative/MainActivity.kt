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
import com.example.tobisoappnative.screens.CalendarScreen
import com.example.tobisoappnative.screens.EventDetailScreen
import com.example.tobisoappnative.screens.MoreScreen
import com.example.tobisoappnative.navigation.BottomBar
import com.example.tobisoappnative.screens.CategoryListScreen
import com.example.tobisoappnative.screens.FeedbackScreen
import com.example.tobisoappnative.screens.AboutScreen
import com.example.tobisoappnative.screens.ChangelogScreen
import com.example.tobisoappnative.screens.FavoritesScreen
import com.example.tobisoappnative.screens.NoInternetScreen
import com.example.tobisoappnative.screens.UpdaterScreen
import com.example.tobisoappnative.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import androidx.work.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.util.concurrent.TimeUnit
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.ShopManager
import com.example.tobisoappnative.components.FullScreenPointsOverlay
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.FullScreenMilestoneOverlay
import androidx.compose.runtime.rememberCoroutineScope
import android.app.AlarmManager
import com.example.tobisoappnative.screens.StreakScreen
import com.example.tobisoappnative.screens.ShopScreen
import com.example.tobisoappnative.screens.addTodayToStreak
import com.example.tobisoappnative.screens.getStreakDays
import com.example.tobisoappnative.screens.calculateStreaks
import java.util.*

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Zaznamenat otevření aplikace
        recordAppOpen()
        
        // Přidat dnešní den do řady (pokud už tam není)
        addTodayToStreak(this)
        
        // Inicializace StreakFreezeManager a kontrola automatického použití
        StreakFreezeManager.init(this)
        StreakFreezeManager.checkAndAutoUseFreeze(this)
        
        // DOČASNÉ: Vymazat dosažené milníky pro testování (odkomentujte pokud potřebujete)
        // resetMilestones(this)
        
        // POZOR: Milníky se nyní kontrolují v MyApp() po inicializaci UI
        
        setContent {
            MyApp()
        }
        // Naplánování notifikací při startu aplikace
        scheduleNotification(this, 17, 0, false) // běžná notifikace v 17:00
        scheduleNotification(this, 20, 0, true)  // kritická notifikace ve 20:00
        
        // Nové notifikace pro události
        scheduleEventNotification(this, 18, 0, "tomorrow_events") // notifikace v 18:00 pro zítřejší události
        scheduleEventNotification(this, 6, 30, "today_events")     // notifikace v 6:30 pro dnešní události

        // Denní kontrola aktualizace
        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "update_check_work",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )
    }

    fun scheduleNotification(context: Context, hour: Int, minute: Int, isCritical: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("hour", hour)
            putExtra("critical", isCritical)
        }
        val requestCode = if (isCritical) 2001 else 2000
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleEventNotification(context: Context, hour: Int, minute: Int, notificationType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("notification_type", notificationType)
        }
        val requestCode = when (notificationType) {
            "tomorrow_events" -> 3000
            "today_events" -> 3001
            else -> 3999
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
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
        val isOffline by mainViewModel.isOffline.collectAsState()
        val hasUserDismissedNoInternet by mainViewModel.hasUserDismissedNoInternet.collectAsState()
        val lastAddedPoints by PointsManager.lastAddedPoints.collectAsState()
        val lastMilestone by PointsManager.lastMilestone.collectAsState()
        var showOverlay by remember { mutableStateOf(false) }
        var overlayPoints by remember { mutableStateOf(0) }
        var showTotalOverlay by remember { mutableStateOf(false) }
        var showMilestoneOverlay by remember { mutableStateOf(false) }
        var milestoneDay by remember { mutableStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        // Kontrola, zda byla aplikace otevřena z notifikace
        val navigateTo = intent?.getStringExtra("navigate_to")

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
            if (isConnected.value) {
                mainViewModel.resetNoInternetDismiss()
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
            mainViewModel.loadCategories()
        }

        // Inicializace bodů a obchodu při startu aplikace
        LaunchedEffect(context) {
            PointsManager.init(context)
            ShopManager.init(context)
            // Kontrola milníků až po inicializaci PointsManager a UI
            checkStreakMilestones(context)
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

        // LaunchedEffect pro běžný overlay (body s přičítáním)
        LaunchedEffect(lastAddedPoints) {
            if (lastAddedPoints != 0 && lastMilestone == null) {
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
                    if (!isConnected.value && !hasUserDismissedNoInternet) {
                        NoInternetScreen(
                            onRetry = onRetry,
                            onOfflineMode = { 
                                mainViewModel.enableOfflineMode()
                                mainViewModel.confirmOfflineModeTransition()
                            }
                        )
                    } else if (categories.isEmpty() && categoryError == null && !isOffline) {
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
                                                    route.startsWith("favorites") ||
                                                    route.startsWith("updater") ||
                                                    route.startsWith("questions") ||
                                                    route.startsWith("mixedQuiz") ||
                                                    route.startsWith("eventDetail") ||
                                                    route.startsWith("shop"))
                                            ),
                                    enter = slideInVertically(
                                        initialOffsetY = { it }, // přichází zdola
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            delayMillis = 100 // mírné zpoždění pro hladší přechod
                                        )
                                    ),
                                    exit = slideOutVertically(
                                        targetOffsetY = { it }, // odchází dolů
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            delayMillis = 0 // okamžité schování
                                        )
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
                                composable("search") {
                                    SearchScreen(
                                        navController = navController,
                                        searchRequestFocus = searchRequestFocus,
                                        viewModel = mainViewModel
                                    )
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
                                    "updater",
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
                                    UpdaterScreen(navController = navController)
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
                                            initialOffsetY = { -it }, // přichází z hora
                                            animationSpec = tween(400)
                                        )
                                    },
                                    exitTransition = {
                                        slideOutVertically(
                                            targetOffsetY = { -it }, // odchází nahoru
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popEnterTransition = {
                                        slideInVertically(
                                            initialOffsetY = { -it }, // při návratu přichází z hora
                                            animationSpec = tween(400)
                                        )
                                    },
                                    popExitTransition = {
                                        slideOutVertically(
                                            targetOffsetY = { -it }, // při návratu odchází nahoru
                                            animationSpec = tween(400)
                                        )
                                    }
                                ) {
                                    StreakScreen(navController = navController)
                                }
                                composable(
                                    "questions/{postId}",
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
                                    val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                                    if (postId != null) {
                                        com.example.tobisoappnative.screens.QuestionsScreen(
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
                                composable("allQuestions") {
                                    com.example.tobisoappnative.screens.AllQuestionsScreen(
                                        navController = navController
                                    )
                                }
                                composable(
                                    "mixedQuiz/{questionIds}",
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
                                    val questionIds = backStackEntry.arguments?.getString("questionIds") ?: ""
                                    com.example.tobisoappnative.screens.MixedQuizScreen(
                                        questionIds = questionIds,
                                        navController = navController
                                    )
                                }
                                composable(
                                    "eventDetail/{eventId}",
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
                                    val eventId = backStackEntry.arguments?.getString("eventId")?.toIntOrNull()
                                    if (eventId != null) {
                                        EventDetailScreen(
                                            eventId = eventId,
                                            navController = navController
                                        )
                                    } else {
                                        Text(
                                            "Chybné ID události",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                composable(
                                    "shop",
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
                                    ShopScreen(navController = navController)
                                }
                            }
                        }
                    }
                    
                    // Overlays pro body a milníky
                    if (showMilestoneOverlay) {
                        FullScreenMilestoneOverlay(
                            points = overlayPoints,
                            totalPoints = totalPoints,
                            milestoneDay = milestoneDay
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
    
    private fun recordAppOpen() {
        val prefs = getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        prefs.edit().putString("last_opened_date", today).apply()
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkStreakMilestones(context: Context) {
        val streakDays = getStreakDays(context)
        val streakInfo = calculateStreaks(streakDays)
        val currentStreak = streakInfo.currentStreak
        
        println("=== STREAK MILESTONES DEBUG ===")
        println("Current streak: $currentStreak days")
        println("Total streak days: ${streakDays.size}")
        
        // Generování všech milníků dynamicky
        val allMilestones = generateStreakMilestones(currentStreak)
        
        // SharedPreferences pro sledování dosažených milníků
        val milestonesPrefs = context.getSharedPreferences("streak_milestones", Context.MODE_PRIVATE)
        
        // Debug: zobrazit všechny dosažené milníky
        val achievedMilestones = milestonesPrefs.all.keys.filter { it.startsWith("milestone_") }
        println("Already achieved milestones: $achievedMilestones")
        
        // Zkontrolovat každý milník
        var newMilestonesFound = false
        allMilestones.forEach { (days, points) ->
            if (currentStreak >= days) {
                val milestoneKey = "milestone_$days"
                val isAlreadyAchieved = milestonesPrefs.getBoolean(milestoneKey, false)
                
                println("Checking milestone $days days: already achieved = $isAlreadyAchieved")
                
                if (!isAlreadyAchieved) {
                    newMilestonesFound = true
                    println("🎉 NEW MILESTONE ACHIEVED: $days days - awarding $points points")
                    
                    // Milník dosažen poprvé - přidat body s informací o milníku
                    PointsManager.addPointsForMilestone(context, points, days)
                    
                    // Označit milník jako dosažený
                    milestonesPrefs.edit().putBoolean(milestoneKey, true).apply()
                    
                    println("Points added and milestone marked as achieved")
                } else {
                    println("Milestone $days already achieved, skipping")
                }
            }
        }
        
        if (!newMilestonesFound) {
            println("No new milestones found for current streak: $currentStreak")
        }
        println("=== END STREAK MILESTONES DEBUG ===")
    }
    
    private fun generateStreakMilestones(maxStreak: Int): Map<Int, Int> {
        val milestones = mutableMapOf<Int, Int>()
        
        // Základní významné milníky
        val specialMilestones = mapOf(
            7 to 15,    // 1 týden
            14 to 15,   // 2 týdny  
            30 to 15,   // 1 měsíc
            60 to 15,   // 2 měsíce
            100 to 15,  // 100 dní
            183 to 30,  // půl roku (6 měsíců)
            365 to 50,  // 1 rok
            548 to 30,  // 1.5 roku
            730 to 100, // 2 roky
            913 to 75,  // 2.5 roku
            1095 to 150, // 3 roky
            1460 to 200, // 4 roky
            1826 to 250  // 5 let
        )
        
        // Přidat speciální milníky
        milestones.putAll(specialMilestones)
        
        // Každých 25 dní od 25 do nekonečna (kromě speciálních milníků)
        var current = 25
        while (current <= maxStreak + 100) { // +100 pro rezervu
            if (!specialMilestones.containsKey(current)) {
                milestones[current] = 15
            }
            current += 25
        }
        
        return milestones.toSortedMap()
    }
    
    // DOČASNÁ funkce pro resetování milníků při testování
    private fun resetMilestones(context: Context) {
        val milestonesPrefs = context.getSharedPreferences("streak_milestones", Context.MODE_PRIVATE)
        milestonesPrefs.edit().clear().apply()
        println("All milestones reset for testing")
    }
}