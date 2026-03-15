package com.example.tobisoappnative.repository

import com.example.tobisoappnative.model.ExerciseValidationResult
import com.example.tobisoappnative.model.InteractiveExerciseResponse

interface ExerciseRepository {
    /** Load a single exercise by its ID (network with offline fallback). */
    suspend fun getExercise(exerciseId: Int): Result<InteractiveExerciseResponse>

    /** Validate user solution JSON for the given exercise (network only). */
    suspend fun validateExercise(
        exerciseId: Int,
        userSolutionJson: String
    ): Result<ExerciseValidationResult>
}
