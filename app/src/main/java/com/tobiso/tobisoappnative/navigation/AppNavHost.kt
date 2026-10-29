package com.tobiso.tobisoappnative.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tobiso.tobisoappnative.navigation.AiChatHistoryRoute
import com.tobiso.tobisoappnative.navigation.AuthRoute
import com.tobiso.tobisoappnative.navigation.ClassesRoute
import com.tobiso.tobisoappnative.screens.ClassesScreen
import com.tobiso.tobisoappnative.screens.AuthScreen
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
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
import com.tobiso.tobisoappnative.navigation.ProfileRoute
import com.tobiso.tobisoappnative.navigation.QuestionsRoute
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute
import com.tobiso.tobisoappnative.navigation.UpdaterRoute
import com.tobiso.tobisoappnative.navigation.VideoPlayerRoute
import com.tobiso.tobisoappnative.navigation.YouTubePlayerRoute
import com.tobiso.tobisoappnative.navigation.AboutRoute
import com.tobiso.tobisoappnative.screens.HomeScreen
import com.tobiso.tobisoappnative.screens.ShopHubScreen
import com.tobiso.tobisoappnative.screens.CalendarScreen
import com.tobiso.tobisoappnative.screens.EventDetailScreen
import com.tobiso.tobisoappnative.screens.ProfileScreen
import com.tobiso.tobisoappnative.screens.BackpackScreen
import com.tobiso.tobisoappnative.screens.CategoryListScreen
import com.tobiso.tobisoappnative.screens.FeedbackScreen
import com.tobiso.tobisoappnative.screens.AboutScreen
import com.tobiso.tobisoappnative.screens.ChangelogScreen
import com.tobiso.tobisoappnative.screens.FavoritesScreen
import com.tobiso.tobisoappnative.screens.UpdaterScreen
import com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel
import com.tobiso.tobisoappnative.screens.StreakScreen
import com.tobiso.tobisoappnative.screens.ShopScreen
import androidx.navigation.NavHostController

/** All destinations of the app. Kept out of [com.tobiso.tobisoappnative.TobisoApp] so that file only holds scaffolding and overlays. */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any,
    ttsViewModel: com.tobiso.tobisoappnative.viewmodel.tts.TtsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(380, delayMillis = 60, easing = FastOutSlowInEasing)) +
                scaleIn(tween(420, delayMillis = 60, easing = FastOutSlowInEasing), initialScale = 0.96f) +
                slideInVertically(tween(420, delayMillis = 60, easing = FastOutSlowInEasing)) { it / 24 }
        },
        exitTransition = { fadeOut(tween(200, easing = FastOutSlowInEasing)) },
        popEnterTransition = { fadeIn(tween(380, delayMillis = 60, easing = FastOutSlowInEasing)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f) },
        popExitTransition = { fadeOut(tween(200, easing = FastOutSlowInEasing)) }
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
        composable<ClassesRoute>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
            ClassesScreen(navController = navController)
        }
        composable<AuthRoute>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
            AuthScreen(navController = navController)
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
                navController = navController,
                ttsViewModel = ttsViewModel
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
                navController = navController,
                ttsViewModel = ttsViewModel
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
                navController = navController,
                ttsViewModel = ttsViewModel
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
                navController = navController,
                ttsViewModel = ttsViewModel
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
                navController = navController,
                ttsViewModel = ttsViewModel
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
        composable<YouTubePlayerRoute> { backStackEntry ->
            val route: YouTubePlayerRoute = backStackEntry.toRoute()
            com.tobiso.tobisoappnative.screens.YouTubePlayerScreen(
                videoId = route.videoId,
                startSeconds = route.startSeconds,
                navController = navController
            )
        }
        composable<StreakRoute>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
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
            ShopHubScreen(navController = navController, initialTab = 0)
        }
        composable<BackpackRoute>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
            ShopHubScreen(navController = navController, initialTab = 1)
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
                sessionId = route.sessionId,
                navController = navController
            )
        }
        composable<AiChatHistoryRoute>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
            com.tobiso.tobisoappnative.screens.AiChatHistoryScreen(navController = navController)
        }
    }
}
