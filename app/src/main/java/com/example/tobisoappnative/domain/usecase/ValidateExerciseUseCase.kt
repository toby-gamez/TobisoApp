package com.example.tobisoappnative.domain.usecase

import com.example.tobisoappnative.model.ExerciseValidationResult
import com.example.tobisoappnative.repository.ExerciseRepository

class ValidateExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(
        exerciseId: Int,
        userSolutionJson: String
    ): Result<ExerciseValidationResult> =
        repository.validateExercise(exerciseId, userSolutionJson)
}
