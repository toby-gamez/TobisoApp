package com.example.tobisoappnative.repository

import android.content.Context
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.utils.NetworkUtils

/**
 * Handles the "download all offline data" bulk operation used by
 * HomeScreen, AllQuestionsScreen, ProfileScreen, OfflineManagerScreen, CalendarScreen.
 */
class OfflineRepositoryImpl(
    private val context: Context,
    private val offlineDataManager: OfflineDataManager
) {
    /**
     * Downloads all data categories from the API and stores them in the offline cache.
     * Returns true on success, false on failure.
     */
    suspend fun downloadAllData(onProgress: (Float) -> Unit): Boolean {
        return try {
            if (!NetworkUtils.isOnline(context)) return false

            onProgress(0f)
            val categoriesArray = ApiClient.apiService.getCategories()
            onProgress(0.2f)
            val postsArray = ApiClient.apiService.getPosts()
            onProgress(0.4f)
            val questionsArray = ApiClient.apiService.getAllQuestions()
            onProgress(0.5f)
            val questionsPostsArray = ApiClient.apiService.getPostsForQuestions()
            onProgress(0.65f)
            val relatedPostsArray = ApiClient.apiService.getAllRelatedPosts()
            onProgress(0.8f)
            val addendumsArray = ApiClient.apiService.getAddendums()
            onProgress(0.9f)

            val allExercises = mutableListOf<com.example.tobisoappnative.model.InteractiveExerciseResponse>()
            postsArray.forEach { post ->
                try {
                    allExercises.addAll(ApiClient.apiService.getExercisesByPostId(post.id))
                } catch (e: Exception) { /* continue */ }
            }
            onProgress(0.95f)

            offlineDataManager.saveCategoriesPostsAndQuestions(
                categoriesArray.toList(),
                postsArray.toList(),
                questionsArray.toList(),
                questionsPostsArray.toList(),
                relatedPostsArray.toList(),
                addendumsArray.toList(),
                allExercises
            )
            onProgress(1f)
            true
        } catch (e: Exception) {
            android.util.Log.w("OfflineRepo", "downloadAllData failed: ${e.message}")
            false
        }
    }

    fun isCacheFresh(minutes: Int): Boolean = offlineDataManager.isCacheFresh(minutes)
}
