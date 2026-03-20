package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
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
    /**
     * Full download used by OfflineManagerScreen "Stáhnout vše" button.
     * Phase A: categories + posts saved immediately.
     * Phase B: questions, related, addendums, exercises – saved with complete timestamp.
     * Throws an Exception with a descriptive message on failure so the caller can display it.
     */
    suspend fun downloadAllData(onProgress: (Float) -> Unit): Boolean {
        val isOnline = NetworkUtils.isOnline(context)
        android.util.Log.d("OfflineRepo", "downloadAllData start: isOnline=$isOnline")
        if (!isOnline) {
            throw Exception("Zařízení není připojeno k internetu (NetworkUtils.isOnline = false)")
        }

        onProgress(0f)

        val categories: List<Category>
        try {
            categories = ApiClient.apiService.getCategories().toList()
            android.util.Log.d("OfflineRepo", "getCategories OK: ${categories.size}")
        } catch (e: Exception) {
            android.util.Log.e("OfflineRepo", "getCategories failed", e)
            throw Exception("Stažení kategorií selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
        onProgress(0.15f)

        val posts: List<Post>
        try {
            posts = ApiClient.apiService.getPosts().toList()
            android.util.Log.d("OfflineRepo", "getPosts OK: ${posts.size}")
        } catch (e: Exception) {
            android.util.Log.e("OfflineRepo", "getPosts failed", e)
            throw Exception("Stažení článků selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
        onProgress(0.3f)

        try {
            // Save phase A immediately so UI has data even if phase B fails
            offlineDataManager.saveCategoriesAndPosts(categories, posts)
            android.util.Log.d("OfflineRepo", "saveCategoriesAndPosts OK")
        } catch (e: Exception) {
            android.util.Log.e("OfflineRepo", "saveCategoriesAndPosts failed", e)
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
            val categories = ApiClient.apiService.getCategories().toList()
            val posts = ApiClient.apiService.getPosts().toList()
            offlineDataManager.saveCategoriesAndPosts(categories, posts)
            Pair(categories, posts)
        } catch (e: Exception) {
            android.util.Log.e("OfflineRepo", "downloadCategoriesAndPosts failed: ${e.message}", e)
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
            android.util.Log.e("OfflineRepo", "downloadRemainingData failed: ${e.message}", e)
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
                ApiClient.apiService.getAllQuestions().toList()
            } catch (e: Exception) {
                android.util.Log.w("OfflineRepo", "getAllQuestions failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.25f)

            val questionsPosts = try {
                ApiClient.apiService.getPostsForQuestions().toList()
            } catch (e: Exception) {
                android.util.Log.w("OfflineRepo", "getPostsForQuestions failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.45f)

            val relatedPosts = try {
                ApiClient.apiService.getAllRelatedPosts().toList()
            } catch (e: Exception) {
                android.util.Log.w("OfflineRepo", "getAllRelatedPosts failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.6f)

            val addendums = try {
                ApiClient.apiService.getAddendums().toList()
            } catch (e: Exception) {
                android.util.Log.w("OfflineRepo", "getAddendums failed: ${e.message}")
                emptyList()
            }
            onProgress(startProgress + span * 0.75f)

            val exercises = mutableListOf<InteractiveExerciseResponse>()
            posts.forEachIndexed { idx, post ->
                try {
                    exercises.addAll(ApiClient.apiService.getExercisesByPostId(post.id).toList())
                } catch (_: Exception) { /* one post failing is OK */ }
                if (idx % 10 == 0) {
                    onProgress(startProgress + span * (0.75f + 0.2f * (idx.toFloat() / posts.size)))
                }
            }
            onProgress(startProgress + span * 0.95f)

            offlineDataManager.saveRemainingData(
                categories, posts,
                questions, questionsPosts,
                relatedPosts, addendums, exercises
            )
            onProgress(1f)
            true
        } catch (e: Exception) {
            android.util.Log.e("OfflineRepo", "downloadAndSaveRemaining failed", e)
            throw Exception("Uložení dat selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
    }

    fun isCacheFresh(minutes: Int): Boolean = offlineDataManager.isCacheFresh(minutes)
}
