package com.example.tobisoappnative.domain.usecase

import com.example.tobisoappnative.model.InteractiveExerciseResponse
import com.example.tobisoappnative.repository.ExerciseRepository

class GetExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(exerciseId: Int): Result<InteractiveExerciseResponse> =
        repository.getExercise(exerciseId)
}
