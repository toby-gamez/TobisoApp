package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.utils.NetworkUtils

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

    override suspend fun getPostsByCategory(categoryId: Int?): Result<List<Post>> {
        return try {
            if (offlineDataManager.isCacheFresh(OfflineDataManager.CACHE_FRESHNESS_MINUTES)) {
                val cached = if (categoryId != null)
                    offlineDataManager.getCachedPostsByCategory(categoryId)
                else
                    offlineDataManager.getCachedPosts()
                if (cached != null) return Result.success(cached)
            }
            if (NetworkUtils.isOnline(context)) {
                Result.success(ApiClient.apiService.getPosts(categoryId).toList())
            } else {
                val cached = if (categoryId != null)
                    offlineDataManager.getCachedPostsByCategory(categoryId)
                else
                    offlineDataManager.getCachedPosts()
                if (cached != null) Result.success(cached)
                else Result.failure(IllegalStateException("Články nejsou dostupné v offline režimu"))
            }
        } catch (e: Exception) {
            val cached = if (categoryId != null)
                offlineDataManager.getCachedPostsByCategory(categoryId)
            else
                offlineDataManager.getCachedPosts()
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    override suspend fun getPost(postId: Int): Result<Post> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                Result.success(ApiClient.apiService.getPost(postId))
            } else {
                val cached = offlineDataManager.getCachedPost(postId)
                if (cached != null) Result.success(cached)
                else Result.failure(IllegalStateException("Článek není dostupný v offline režimu"))
            }
        } catch (e: Exception) {
            val cached = offlineDataManager.getCachedPost(postId)
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    override fun isCacheFresh(minutes: Int): Boolean = offlineDataManager.isCacheFresh(minutes)
}
