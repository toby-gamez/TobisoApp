package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.ExerciseValidationResult
import com.tobiso.tobisoappnative.repository.ExerciseRepository
import timber.log.Timber

class ValidateExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(
        exerciseId: Int,
        userSolutionJson: String
    ): Result<ExerciseValidationResult> {
        if (exerciseId <= 0) {
            Timber.w("ValidateExerciseUseCase: invalid exerciseId=$exerciseId")
            return Result.failure(IllegalArgumentException("Neplatné ID cvičení"))
        }
        if (userSolutionJson.isBlank()) {
            Timber.w("ValidateExerciseUseCase: empty solution for exercise $exerciseId")
            return Result.failure(IllegalArgumentException("Prázdné řešení nelze odeslat"))
        }
        if (userSolutionJson.length > 100_000) {
            Timber.w("ValidateExerciseUseCase: solution too large for exercise $exerciseId (${userSolutionJson.length} chars)")
            return Result.failure(IllegalArgumentException("Řešení je příliš velké"))
        }
        return try {
            val result = repository.validateExercise(exerciseId, userSolutionJson)
            result.fold(
                onSuccess = { Result.success(it) },
                onFailure = { e ->
                    Timber.e(e, "ValidateExerciseUseCase: validation failed for exercise $exerciseId")
                    Result.failure(Exception("Validace selhala: ${e.message}", e))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "ValidateExerciseUseCase: unexpected error for exercise $exerciseId")
            Result.failure(Exception("Neočekávaná chyba: ${e.message}", e))
        }
    }
}
