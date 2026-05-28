package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.repository.ExerciseRepository
import timber.log.Timber

class GetExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(exerciseId: Int): Result<InteractiveExerciseResponse> {
        if (exerciseId <= 0) {
            Timber.w("GetExerciseUseCase: invalid exerciseId=$exerciseId")
            return Result.failure(IllegalArgumentException("Neplatné ID cvičení"))
        }
        return try {
            val result = repository.getExercise(exerciseId)
            result.fold(
                onSuccess = { exercise ->
                    if (exercise.isActive == false) {
                        Timber.w("GetExerciseUseCase: exercise $exerciseId is inactive")
                    }
                    Result.success(exercise)
                },
                onFailure = { e ->
                    Timber.e(e, "GetExerciseUseCase: repository failed for exercise $exerciseId")
                    Result.failure(Exception("Nepodařilo se načíst cvičení: ${e.message}", e))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "GetExerciseUseCase: unexpected error for exercise $exerciseId")
            Result.failure(Exception("Neočekávaná chyba: ${e.message}", e))
        }
    }
}
