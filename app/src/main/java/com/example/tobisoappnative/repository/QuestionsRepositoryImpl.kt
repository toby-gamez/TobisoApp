package com.example.tobisoappnative.repository

import android.content.Context
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.utils.NetworkUtils

class QuestionsRepositoryImpl(
    private val context: Context,
    private val offlineDataManager: OfflineDataManager
) : QuestionsRepository {

    override suspend fun getAllQuestions(): Result<Pair<List<Question>, List<Post>>> {
        val isOnline = NetworkUtils.isOnline(context)
        return if (isOnline) {
            try {
                val questions = ApiClient.apiService.getAllQuestions().toList()
                val posts = ApiClient.apiService.getPosts().toList()
                Result.success(Pair(questions, posts))
            } catch (e: Exception) {
                // Fallback to cache on network error
                val cachedQ = offlineDataManager.getCachedQuestions()
                val cachedP = offlineDataManager.getCachedQuestionsPosts()
                if (cachedQ != null && cachedP != null) Result.success(Pair(cachedQ, cachedP))
                else Result.failure(e)
            }
        } else {
            val cachedQ = offlineDataManager.getCachedQuestions()
            val cachedP = offlineDataManager.getCachedQuestionsPosts()
            if (cachedQ != null && cachedP != null) Result.success(Pair(cachedQ, cachedP))
            else Result.failure(IllegalStateException("Otázky nejsou dostupné v offline režimu"))
        }
    }

    override suspend fun getQuestionsForPost(postId: Int): Result<List<Question>> {
        val isOnline = NetworkUtils.isOnline(context)
        return if (isOnline) {
            try {
                Result.success(ApiClient.apiService.getQuestionsByPostId(postId).toList())
            } catch (e: Exception) {
                val cached = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
                Result.success(cached)
            }
        } else {
            val cached = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
            Result.success(cached)
        }
    }
}
