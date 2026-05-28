package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.ExerciseValidationResult
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse

interface ExerciseRepository {
    /** Load a single exercise by its ID (network with offline fallback). */
    suspend fun getExercise(exerciseId: Int): Result<InteractiveExerciseResponse>

    /** Load all exercises. Online: fetches per-post and caches. Offline: returns cache. */
    suspend fun getAllExercises(postIds: List<Int> = emptyList()): List<InteractiveExerciseResponse>

    /** Validate user solution JSON for the given exercise (network only). */
    suspend fun validateExercise(
        exerciseId: Int,
        userSolutionJson: String
    ): Result<ExerciseValidationResult>
}
