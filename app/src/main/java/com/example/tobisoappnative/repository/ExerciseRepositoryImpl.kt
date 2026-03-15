package com.example.tobisoappnative.repository

import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.ExerciseValidationResult
import com.example.tobisoappnative.model.InteractiveExerciseResponse
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.ValidateSolutionRequest
import com.example.tobisoappnative.utils.NetworkUtils
import android.content.Context

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
