package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.DifficultyRatingRequest
import com.tobiso.tobisoappnative.model.PostDifficultyResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.PostSummaryResponse
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.model.RelatedPost
import com.tobiso.tobisoappnative.utils.NetworkUtils
import com.tobiso.tobisoappnative.utils.filterVersionForGrade
import okhttp3.ResponseBody

class PostsRepositoryImpl(
    private val context: Context,
    private val offlineDataManager: OfflineDataManager,
    private val api: ApiService
) : PostsRepository {

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            if (offlineDataManager.isCacheFresh(OfflineDataManager.CACHE_FRESHNESS_MINUTES)) {
                val cached = offlineDataManager.getCachedCategories()
                if (cached != null) return Result.success(cached)
            }
            if (NetworkUtils.isOnline(context)) {
                Result.success(api.getCategories().toList())
            } else {
                val cached = offlineDataManager.getCachedCategories()
                if (cached != null) Result.success(cached)
                else Result.failure(IllegalStateException("Kategorie nejsou dostupné v offline režimu"))
            }
        } catch (e: Exception) {
            val cached = offlineDataManager.getCachedCategories()
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    override suspend fun getPostsByCategory(categoryId: Int?, gradeId: Int?): Result<List<Post>> {
        return try {
            if (gradeId == null && offlineDataManager.isCacheFresh(OfflineDataManager.CACHE_FRESHNESS_MINUTES)) {
                val cached = if (categoryId != null)
                    offlineDataManager.getCachedPostsByCategory(categoryId)
                else
                    offlineDataManager.getCachedPosts()
                if (cached != null) return Result.success(cached)
            }
            if (NetworkUtils.isOnline(context)) {
                Result.success(api.getPosts(categoryId, gradeId).toList())
            } else {
                val cached = if (categoryId != null)
                    offlineDataManager.getCachedPostsByCategory(categoryId)
                else
                    offlineDataManager.getCachedPosts()
                if (cached != null) {
                    val posts = if (gradeId != null) {
                        val grades = offlineDataManager.getCachedGrades()
                        cached.map { it.filterVersionForGrade(gradeId, grades) }
                    } else cached
                    Result.success(posts)
                } else Result.failure(IllegalStateException("Články nejsou dostupné v offline režimu"))
            }
        } catch (e: Exception) {
            val cached = if (categoryId != null)
                offlineDataManager.getCachedPostsByCategory(categoryId)
            else
                offlineDataManager.getCachedPosts()
            if (cached != null) {
                val posts = if (gradeId != null) {
                    val grades = offlineDataManager.getCachedGrades()
                    cached.map { it.filterVersionForGrade(gradeId, grades) }
                } else cached
                Result.success(posts)
            } else Result.failure(e)
        }
    }

    override suspend fun getPost(postId: Int, gradeId: Int?): Result<Post> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                Result.success(api.getPost(postId, gradeId))
            } else {
                val cached = offlineDataManager.getCachedPost(postId)
                if (cached != null) {
                    val post = if (gradeId != null) {
                        cached.filterVersionForGrade(gradeId, offlineDataManager.getCachedGrades())
                    } else cached
                    Result.success(post)
                } else Result.failure(IllegalStateException("Článek není dostupný v offline režimu"))
            }
        } catch (e: Exception) {
            val cached = offlineDataManager.getCachedPost(postId)
            if (cached != null) {
                val post = if (gradeId != null) {
                    cached.filterVersionForGrade(gradeId, offlineDataManager.getCachedGrades())
                } else cached
                Result.success(post)
            } else Result.failure(e)
        }
    }

    override suspend fun getPostSummaries(): Result<List<PostSummaryResponse>> {
        return try {
            Result.success(api.getPostSummaries().toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGrades(): Result<List<Grade>> {
        return try {
            Result.success(api.getGrades().toList())
        } catch (e: Exception) {
            val cached = offlineDataManager.getCachedGrades()
            if (cached.isNotEmpty()) Result.success(cached)
            else Result.failure(e)
        }
    }

    override fun isCacheFresh(minutes: Int): Boolean = offlineDataManager.isCacheFresh(minutes)

    override suspend fun getRelatedPosts(
        postId: Int,
        currentPost: Post?,
        allPosts: List<Post>
    ): Result<List<RelatedPost>> {
        return try {
            val list = mutableListOf<RelatedPost>()
            if (NetworkUtils.isOnline(context)) {
                list.addAll(api.getRelatedPostsByPostId(postId).toList())
            } else {
                val cached = offlineDataManager.getCachedRelatedPostsByPostId(postId)
                if (cached != null) list.addAll(cached)
            }
            if (list.size < 5 && currentPost?.categoryId != null) {
                val existingIds = list.map { it.relatedPostId }.toSet()
                val postsPool = if (allPosts.isNotEmpty()) allPosts
                    else offlineDataManager.getCachedPosts() ?: emptyList()
                postsPool
                    .filter { p -> p.categoryId == currentPost.categoryId && p.id != currentPost.id }
                    .take(5 - list.size)
                    .filter { p -> p.id !in existingIds }
                    .map { p ->
                        RelatedPost(
                            id = 0,
                            postId = currentPost.id,
                            relatedPostId = p.id,
                            text = "souvisí s tématem",
                            postTitle = currentPost.title,
                            relatedPostTitle = p.title
                        )
                    }
                    .forEach { list.add(it) }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAddendums(): Result<List<Addendum>> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val list = api.getAddendums().toList()
                offlineDataManager.saveAddendums(list)
                Result.success(list)
            } else {
                val cached = offlineDataManager.getCachedAddendums() ?: emptyList()
                Result.success(cached)
            }
        } catch (e: Exception) {
            val cached = offlineDataManager.getCachedAddendums() ?: emptyList()
            Result.success(cached)
        }
    }

    override suspend fun getQuestionsForPost(postId: Int): Result<List<Question>> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                Result.success(api.getQuestionsByPostId(postId).toList())
            } else {
                val cached = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
                Result.success(cached)
            }
        } catch (e: Exception) {
            val cached = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
            Result.success(cached)
        }
    }

    override suspend fun getExercisesForPost(
        postId: Int,
        postCategoryId: Int?
    ): Result<List<InteractiveExerciseResponse>> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                Result.success(api.getExercisesByPostId(postId).toList())
            } else {
                val cached = getCachedExercisesForPost(postId, postCategoryId)
                Result.success(cached)
            }
        } catch (e: Exception) {
            val cached = getCachedExercisesForPost(postId, postCategoryId)
            Result.success(cached)
        }
    }

    override suspend fun downloadPdf(postId: Int): ResponseBody {
        return api.generatePostPdf(postId)
    }

    private suspend fun getCachedExercisesForPost(
        postId: Int,
        postCategoryId: Int?
    ): List<InteractiveExerciseResponse> {
        val categories = offlineDataManager.getCachedCategories() ?: emptyList()
        val relevantCategoryIds = if (postCategoryId != null)
            getAllAncestorCategoryIds(postCategoryId, categories)
        else emptySet()
        return (offlineDataManager.getCachedExercises() ?: emptyList())
            .asSequence()
            .filter { it.isActive != false }
            .filter { ex ->
                (ex.postIds?.contains(postId) == true) ||
                    (relevantCategoryIds.isNotEmpty() && ex.categoryIds?.any { it in relevantCategoryIds } == true)
            }
            .distinctBy { it.id }
            .toList()
    }

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences("post_extras", Context.MODE_PRIVATE)
    }
    private val json = Json { ignoreUnknownKeys = true }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override suspend fun getArticleOfDay(): Result<Post> {
        val day = today()
        fun cached(anyDay: Boolean): Post? {
            if (!anyDay && prefs.getString(KEY_AOD_DAY, null) != day) return null
            return prefs.getString(KEY_AOD_POST, null)?.let { runCatching { json.decodeFromString<Post>(it) }.getOrNull() }
        }
        cached(anyDay = false)?.let { return Result.success(it) }
        return try {
            if (!NetworkUtils.isOnline(context)) {
                return cached(anyDay = true)?.let { Result.success(it) }
                    ?: Result.failure(IllegalStateException("Článek dne není dostupný offline"))
            }
            val post = api.getArticleOfDay()
            prefs.edit().putString(KEY_AOD_DAY, day).putString(KEY_AOD_POST, json.encodeToString(post.copy(content = null, versions = null))).apply()
            Result.success(post)
        } catch (e: Exception) {
            cached(anyDay = true)?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    override suspend fun getRandomPost(): Result<Post> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                Result.success(api.getRandomPost())
            } else {
                offlineDataManager.getCachedPosts()?.randomOrNull()?.let { Result.success(it) }
                    ?: Result.failure(IllegalStateException("Články nejsou dostupné v offline režimu"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableGradeIds(postId: Int): Result<Set<Int>> = try {
        Result.success(api.getPostVersions(postId).mapNotNull { it.gradeId }.toSet())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getDifficulty(postId: Int): Result<PostDifficultyResponse> {
        return try {
            Result.success(api.getPostDifficulty(postId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rateDifficulty(postId: Int, rating: Int, deviceId: String): Result<String> {
        return try {
            val message = api.ratePostDifficulty(postId, DifficultyRatingRequest(rating, deviceId)).message
            // "already_rated" means a vote exists server-side; keep our local copy so results stay visible.
            if (prefs.getInt(voteKey(postId), 0) == 0) prefs.edit().putInt(voteKey(postId), rating).apply()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMyDifficultyVote(postId: Int): Int? =
        prefs.getInt(voteKey(postId), 0).takeIf { it in 1..3 }

    private fun voteKey(postId: Int) = "difficulty_vote_$postId"

    private companion object {
        const val KEY_AOD_DAY = "aod_day"
        const val KEY_AOD_POST = "aod_post"
    }

    private fun getAllAncestorCategoryIds(
        categoryId: Int,
        categories: List<Category>
    ): Set<Int> {
        val result = mutableSetOf<Int>()
        var currentId: Int? = categoryId
        val seen = mutableSetOf<Int>()
        while (currentId != null && seen.add(currentId)) {
            result.add(currentId)
            currentId = categories.firstOrNull { it.id == currentId }?.parentId
        }
        return result
    }
}
