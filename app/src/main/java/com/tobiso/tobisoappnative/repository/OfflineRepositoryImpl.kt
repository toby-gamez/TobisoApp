package com.tobiso.tobisoappnative.repository
import timber.log.Timber

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OfflineRepositoryImpl(
    private val context: Context,
    private val offlineDataManager: OfflineDataManager
) {
    companion object {
        private const val MAX_CONCURRENT_EXERCISE_DOWNLOADS = 10
    }

    // ── Public API ────────────────────────────────────────────────────────

    suspend fun downloadAllData(onProgress: (Float) -> Unit): Boolean {
        if (!NetworkUtils.isOnline(context)) {
            throw Exception("Zařízení není připojeno k internetu (NetworkUtils.isOnline = false)")
        }
        onProgress(0f)
        val categories = fetchCategories()
        onProgress(0.15f)
        val posts = fetchPosts()
        onProgress(0.3f)
        savePhaseA(categories, posts)
        onProgress(0.35f)
        return downloadAndSaveRemaining(categories, posts, onProgress, startProgress = 0.35f)
    }

    suspend fun downloadCategoriesAndPosts(): Pair<List<Category>, List<Post>>? {
        return try {
            if (!NetworkUtils.isOnline(context)) return null
            val categories = fetchCategories()
            val posts = fetchPosts()
            offlineDataManager.saveCategoriesAndPosts(categories, posts)
            Pair(categories, posts)
        } catch (e: Exception) {
            Timber.e(e, "downloadCategoriesAndPosts failed: ${e.message}")
            null
        }
    }

    suspend fun downloadRemainingData(categories: List<Category>, posts: List<Post>): Boolean {
        if (!NetworkUtils.isOnline(context)) return false
        return try {
            downloadAndSaveRemaining(categories, posts, onProgress = {}, startProgress = 0f)
        } catch (e: Exception) {
            Timber.e(e, "downloadRemainingData failed: ${e.message}")
            false
        }
    }

    fun isCacheFresh(minutes: Int): Boolean = offlineDataManager.isCacheFresh(minutes)

    // ── Shared orchestrator ───────────────────────────────────────────────

    private suspend fun downloadAndSaveRemaining(
        categories: List<Category>,
        posts: List<Post>,
        onProgress: (Float) -> Unit,
        startProgress: Float
    ): Boolean {
        return try {
            val p = ProgressTracker(startProgress)

            val questions = fetchQuestions()
            onProgress(p.fraction(0.25f))

            val questionsPosts = fetchQuestionsPosts()
            onProgress(p.fraction(0.45f))

            val relatedPosts = fetchRelatedPosts()
            onProgress(p.fraction(0.6f))

            val addendums = fetchAddendums()
            val grades = fetchGrades()
            if (grades.isNotEmpty()) offlineDataManager.saveGrades(grades)

            onProgress(p.fraction(0.75f))

            val exercises = fetchExercises(posts)
            onProgress(p.fraction(0.93f))

            val events = fetchEvents()
            onProgress(p.fraction(0.97f))

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

    // ── Per-type fetchers ─────────────────────────────────────────────────

    private suspend fun fetchCategories(): List<Category> {
        return try {
            val result = retryWithBackoff { ApiClient.apiService.getCategories().toList() }
            Timber.d("fetchCategories OK: ${result.size}")
            result
        } catch (e: Exception) {
            Timber.e(e, "fetchCategories failed")
            throw Exception("Stažení kategorií selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
    }

    private suspend fun fetchPosts(): List<Post> {
        return try {
            val result = retryWithBackoff { ApiClient.apiService.getPosts().toList() }
            Timber.d("fetchPosts OK: ${result.size}")
            result
        } catch (e: Exception) {
            Timber.e(e, "fetchPosts failed")
            throw Exception("Stažení článků selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
    }

    private suspend fun savePhaseA(categories: List<Category>, posts: List<Post>) {
        try {
            offlineDataManager.saveCategoriesAndPosts(categories, posts)
            Timber.d("savePhaseA OK")
        } catch (e: Exception) {
            Timber.e(e, "savePhaseA failed")
            throw Exception("Uložení kategorií/článků selhalo (${e.javaClass.simpleName}): ${e.message}", e)
        }
    }

    private suspend fun fetchQuestions(): List<com.tobiso.tobisoappnative.model.Question> {
        return try {
            retryWithBackoff { ApiClient.apiService.getAllQuestions().toList() }
        } catch (e: Exception) {
            Timber.w(e, "fetchQuestions failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchQuestionsPosts(): List<Post> {
        return try {
            retryWithBackoff { ApiClient.apiService.getPostsForQuestions().toList() }
        } catch (e: Exception) {
            Timber.w(e, "fetchQuestionsPosts failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchRelatedPosts(): List<com.tobiso.tobisoappnative.model.RelatedPost> {
        return try {
            retryWithBackoff { ApiClient.apiService.getAllRelatedPosts().toList() }
        } catch (e: Exception) {
            Timber.w(e, "fetchRelatedPosts failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchAddendums(): List<com.tobiso.tobisoappnative.model.Addendum> {
        return try {
            retryWithBackoff { ApiClient.apiService.getAddendums().toList() }
        } catch (e: Exception) {
            Timber.w(e, "fetchAddendums failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchGrades(): List<Grade> {
        return try {
            retryWithBackoff { ApiClient.apiService.getGrades().toList() }
        } catch (e: Exception) {
            Timber.w(e, "fetchGrades failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchExercises(posts: List<Post>): List<InteractiveExerciseResponse> {
        if (posts.isEmpty()) return emptyList()
        val exercises = mutableListOf<InteractiveExerciseResponse>()
        val postsCount = posts.size.coerceAtLeast(1)
        val channel = Channel<Pair<Int, List<InteractiveExerciseResponse>>>(MAX_CONCURRENT_EXERCISE_DOWNLOADS)
        coroutineScope {
            var submitted = 0
            repeat(MAX_CONCURRENT_EXERCISE_DOWNLOADS) {
                launch {
                    while (true) {
                        val idx = submitted++
                        if (idx >= posts.size) break
                        val post = posts[idx]
                        val list = try {
                            retryWithBackoff { ApiClient.apiService.getExercisesByPostId(post.id) }
                        } catch (e: Exception) {
                            Timber.w(e, "fetchExercisesByPostId failed for ${post.id}: ${e.message}")
                            emptyList()
                        }
                        channel.send(Pair(idx, list))
                    }
                }
            }
            val results = mutableMapOf<Int, List<InteractiveExerciseResponse>>()
            repeat(postsCount) {
                val (idx, list) = channel.receive()
                results[idx] = list
            }
            for (i in 0 until postsCount) {
                results[i]?.let { exercises.addAll(it) }
            }
        }
        return exercises
    }

    private suspend fun fetchEvents(): List<com.tobiso.tobisoappnative.model.Event> {
        return try {
            retryWithBackoff { ApiClient.apiService.getEvents().toList() }
        } catch (e: Exception) {
            Timber.w(e, "fetchEvents failed: ${e.message}")
            emptyList()
        }
    }

    // ── Retry helper ──────────────────────────────────────────────────────

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
        return block()
    }

    // ── Progress helper ───────────────────────────────────────────────────

    private class ProgressTracker(private val startProgress: Float) {
        private val span = 1f - startProgress
        fun fraction(f: Float) = startProgress + span * f
    }
}
