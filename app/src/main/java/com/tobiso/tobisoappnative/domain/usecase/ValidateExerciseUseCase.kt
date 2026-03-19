package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.ExerciseValidationResult
import com.tobiso.tobisoappnative.repository.ExerciseRepository

class ValidateExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(
        exerciseId: Int,
        userSolutionJson: String
    ): Result<ExerciseValidationResult> =
        repository.validateExercise(exerciseId, userSolutionJson)
}
