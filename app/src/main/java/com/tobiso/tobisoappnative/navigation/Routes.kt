package com.tobiso.tobisoappnative.navigation

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeRoute

@Serializable
object CalendarRoute

@Serializable
data class CalendarWithDateRoute(val year: Int, val month: Int)

@Serializable
object ProfileRoute

@Serializable
object FeedbackRoute

@Serializable
object ChangelogRoute

@Serializable
object UpdaterRoute

@Serializable
object OfflineManagerRoute

@Serializable
object AboutRoute

@Serializable
object FavoritesRoute

@Serializable
data class CategoryListRoute(val categoryName: String)

@Serializable
data class PostDetailRoute(val postId: Int)

@Serializable
data class ExerciseTimelineRoute(val exerciseId: Int)

@Serializable
data class ExerciseDragDropRoute(val exerciseId: Int)

@Serializable
data class ExerciseMatchingRoute(val exerciseId: Int)

@Serializable
data class ExerciseCircuitRoute(val exerciseId: Int)
@Serializable
data class VideoPlayerRoute(val videoUrl: String)

@Serializable
object StreakRoute

@Serializable
data class QuestionsRoute(val postId: Int)

@Serializable
object AllQuestionsRoute

@Serializable
data class MixedQuizRoute(val questionIds: String)

@Serializable
data class EventDetailRoute(val eventId: Int)

@Serializable
object ShopRoute

@Serializable
object BackpackRoute

@Serializable
data class AiChatRoute(val postId: Int, val postTitle: String, val firstUserMessage: String)

/**
 * Routes where the bottom navigation bar should be visible.
 * Add a route here when the screen should display the bottom bar.
 * All other routes automatically hide it — no changes needed elsewhere.
 */
val BOTTOM_BAR_ROUTES: Set<KClass<*>> = setOf(
    HomeRoute::class,
    AllQuestionsRoute::class,
    CalendarRoute::class,
    CalendarWithDateRoute::class,
    ProfileRoute::class,
    CategoryListRoute::class,
)
