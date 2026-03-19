package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.repository.ExerciseRepository

class GetExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(exerciseId: Int): Result<InteractiveExerciseResponse> =
        repository.getExercise(exerciseId)
}
