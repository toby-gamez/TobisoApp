package com.tobiso.tobisoappnative.repository
import timber.log.Timber

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.utils.NetworkUtils

/**
 * Handles the "download all offline data" bulk operation used by
 * HomeScreen, AllQuestionsScreen, ProfileScreen, OfflineManagerScreen, CalendarScreen.
 */
class OfflineRepositoryImpl(
    private val context: Context,
    private val offlineDataManager: OfflineDataManager
) {
    // Generic retry with exponential backoff for transient network errors.
    private suspend fun <T> retryWithBackoff(
        attempts: Int = 3,
        initialDelayMillis: Long = 500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        var lastError: Throwable? = null
        repeat(attempts - 1) {
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                Timber.w(e, "Transient error, retrying in %d ms", currentDelay)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        // final attempt - let exception propagate if it fails
        return block()
    }

    /**
     * Full download used by OfflineManagerScreen "Stáhnout vše" button.
     * Phase A: categories + posts saved immediately.
     * Phase B: questions, related, addendums, exercises – saved with complete timestamp.
     * Throws an Exception with a descriptive message on failure so the caller can display it.
     */
    suspend fun downloadAllData(onProgress: (Float) -> Unit): Boolean {
        val isOnline = NetworkUtils.isOnline(context)
        Timber.d("downloadAllData start: isOnline=$isOnline")
        if (!isOnline) {
            throw Exception("Zařízení není připojeno k internetu (NetworkUtils.isOnline = false)")
        }

        onProgress(0f)

        val categories: List<Category>
        try {
            categories = retryWithBackoff { ApiClient.apiService.getCategories().toList() }
            Timber.d("getCategories OK: ${categories.size}")
        } catch (e: Exception) {
            Timber.e(e, "getCategories failed")
            throw Exception("Stažení kategorií selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
        onProgress(0.15f)

        val posts: List<Post>
        try {
            posts = retryWithBackoff { ApiClient.apiService.getPosts().toList() }
            Timber.d("getPosts OK: ${posts.size}")
        } catch (e: Exception) {
            Timber.e(e, "getPosts failed")
            throw Exception("Stažení článků selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
        onProgress(0.3f)

        try {
            // Save phase A immediately so UI has data even if phase B fails
            offlineDataManager.saveCategoriesAndPosts(categories, posts)
            Timber.d("saveCategoriesAndPosts OK")
        } catch (e: Exception) {
            Timber.e(e, "saveCategoriesAndPosts failed")
            throw Exception("Uložení kategorií/článků selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
        onProgress(0.35f)

        return downloadAndSaveRemaining(categories, posts, onProgress, startProgress = 0.35f)
    }

    /**
     * Fast Phase 1 on app startup – downloads only categories + posts to unblock the UI.
     * Returns the data directly so Phase 2 doesn't need a DB re-read.
     */
    suspend fun downloadCategoriesAndPosts(): Pair<List<Category>, List<Post>>? {
        return try {
            if (!NetworkUtils.isOnline(context)) return null
            val categories = retryWithBackoff { ApiClient.apiService.getCategories().toList() }
            val posts = retryWithBackoff { ApiClient.apiService.getPosts().toList() }
            offlineDataManager.saveCategoriesAndPosts(categories, posts)
            Pair(categories, posts)
        } catch (e: Exception) {
            Timber.e(e, "downloadCategoriesAndPosts failed: ${e.message}")
            null
        }
    }

    /**
     * Background Phase 2 – downloads questions, related posts, addendums, exercises
     * and saves everything (including passed-in categories+posts) with the complete timestamp.
     * Never throws – returns false on any failure so the calling coroutine stays alive.
     */
    suspend fun downloadRemainingData(categories: List<Category>, posts: List<Post>): Boolean {
        if (!NetworkUtils.isOnline(context)) return false
        return try {
            downloadAndSaveRemaining(categories, posts, onProgress = {}, startProgress = 0f)
        } catch (e: Exception) {
            Timber.e(e, "downloadRemainingData failed: ${e.message}")
            false
        }
    }

    // ── Shared helper ──────────────────────────────────────────────────────────

    private suspend fun downloadAndSaveRemaining(
        categories: List<Category>,
        posts: List<Post>,
        onProgress: (Float) -> Unit,
        startProgress: Float
    ): Boolean {
        return try {
            val span = 1f - startProgress

            val questions = try {
                retryWithBackoff { ApiClient.apiService.getAllQuestions().toList() }
            } catch (e: Exception) {
                Timber.w(e, "getAllQuestions failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.25f)

            val questionsPosts = try {
                retryWithBackoff { ApiClient.apiService.getPostsForQuestions().toList() }
            } catch (e: Exception) {
                Timber.w(e, "getPostsForQuestions failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.45f)

            val relatedPosts = try {
                retryWithBackoff { ApiClient.apiService.getAllRelatedPosts().toList() }
            } catch (e: Exception) {
                Timber.w(e, "getAllRelatedPosts failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.6f)

            val addendums = try {
                retryWithBackoff { ApiClient.apiService.getAddendums().toList() }
            } catch (e: Exception) {
                Timber.w(e, "getAddendums failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.75f)

            val exercises = mutableListOf<InteractiveExerciseResponse>()
            // Fetch exercises concurrently but limit concurrency to avoid overwhelming the server/device.
            coroutineScope {
                val semaphore = Semaphore(10)
                val completed = AtomicInteger(0)
                val deferreds = posts.map { post ->
                    async {
                        semaphore.withPermit {
                            val list = try {
                                retryWithBackoff { ApiClient.apiService.getExercisesByPostId(post.id) }
                            } catch (e: Exception) {
                                Timber.w(e, "getExercisesByPostId failed for ${post.id}: ${e.message}")
                                emptyList()
                            }
                            val done = completed.incrementAndGet()
                            if (done % 10 == 0) {
                                onProgress(startProgress + span * (0.75f + 0.18f * (done.toFloat() / posts.size)))
                            }
                            list
                        }
                    }
                }
                val results = deferreds.awaitAll()
                results.forEach { exercises.addAll(it) }
            }
            onProgress(startProgress + span * 0.93f)

            val events = try {
                retryWithBackoff { ApiClient.apiService.getEvents().toList() }
            } catch (e: Exception) {
                Timber.w(e, "getEvents failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.97f)

            offlineDataManager.saveRemainingData(
                categories, posts,
                questions, questionsPosts,
                relatedPosts, addendums, exercises
            )
            if (events.isNotEmpty()) offlineDataManager.saveEvents(events)
            onProgress(1f)
            true
        } catch (e: Exception) {
            Timber.e(e, "downloadAndSaveRemaining failed")
            throw Exception("Uložení dat selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
    }

    fun isCacheFresh(minutes: Int): Boolean = offlineDataManager.isCacheFresh(minutes)
}
