package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.ExerciseValidationResult
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.model.ValidateSolutionRequest
import com.tobiso.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ExerciseRepositoryImpl(
    private val context: Context,
    private val offlineDataManager: OfflineDataManager
) : ExerciseRepository {

    override suspend fun getExercise(exerciseId: Int): Result<InteractiveExerciseResponse> {
        val isOnline = NetworkUtils.isOnline(context)
        return if (isOnline) {
            try {
                Result.success(ApiClient.apiService.getExercise(exerciseId))
            } catch (e: Exception) {
                // Network failed – fall back to cache
                val cached = offlineDataManager.getCachedExercise(exerciseId)
                if (cached != null) Result.success(cached)
                else Result.failure(e)
            }
        } else {
            val cached = offlineDataManager.getCachedExercise(exerciseId)
            if (cached != null) Result.success(cached)
            else Result.failure(IllegalStateException("Cvičení není dostupné v offline režimu"))
        }
    }

    override suspend fun getAllExercises(postIds: List<Int>): List<InteractiveExerciseResponse> {
        if (NetworkUtils.isOnline(context) && postIds.isNotEmpty()) {
            return try {
                val fetched = coroutineScope {
                    postIds.map { postId ->
                        async {
                            runCatching { ApiClient.apiService.getExercisesByPostId(postId) }
                                .getOrElse { emptyList() }
                        }
                    }.awaitAll()
                }.flatten().distinctBy { it.id }
                if (fetched.isNotEmpty()) offlineDataManager.upsertExercises(fetched)
                fetched
            } catch (e: Exception) {
                offlineDataManager.getCachedExercises() ?: emptyList()
            }
        }
        return offlineDataManager.getCachedExercises() ?: emptyList()
    }

    override suspend fun validateExercise(
        exerciseId: Int,
        userSolutionJson: String
    ): Result<ExerciseValidationResult> {
        val isOnline = NetworkUtils.isOnline(context)
        if (!isOnline) {
            return Result.failure(IllegalStateException("Validace vyžaduje internetové připojení"))
        }
        return try {
            val result = ApiClient.apiService.validateExercise(
                exerciseId,
                ValidateSolutionRequest(userSolutionJson)
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
