package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.Category
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
    private val offlineDataManager: OfflineDataManager
) : PostsRepository {

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            if (offlineDataManager.isCacheFresh(OfflineDataManager.CACHE_FRESHNESS_MINUTES)) {
                val cached = offlineDataManager.getCachedCategories()
                if (cached != null) return Result.success(cached)
            }
            if (NetworkUtils.isOnline(context)) {
                Result.success(ApiClient.apiService.getCategories().toList())
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
                Result.success(ApiClient.apiService.getPosts(categoryId, gradeId).toList())
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
                Result.success(ApiClient.apiService.getPost(postId, gradeId))
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
            Result.success(ApiClient.apiService.getPostSummaries().toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGrades(): Result<List<Grade>> {
        return try {
            Result.success(ApiClient.apiService.getGrades().toList())
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
                list.addAll(ApiClient.apiService.getRelatedPostsByPostId(postId).toList())
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
                val list = ApiClient.apiService.getAddendums().toList()
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
                Result.success(ApiClient.apiService.getQuestionsByPostId(postId).toList())
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
                Result.success(ApiClient.apiService.getExercisesByPostId(postId).toList())
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
        return ApiClient.apiService.generatePostPdf(postId)
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
