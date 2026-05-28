package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.repository.QuestionsRepository
import timber.log.Timber

class GetAllQuestionsUseCase(private val repository: QuestionsRepository) {
    suspend operator fun invoke(): Result<Pair<List<Question>, List<Post>>> {
        return try {
            val result = repository.getAllQuestions()
            result.fold(
                onSuccess = { (questions, posts) ->
                    if (questions.isEmpty()) {
                        Timber.w("GetAllQuestionsUseCase: received empty question list")
                    }
                    Result.success(questions to posts)
                },
                onFailure = { e ->
                    Timber.e(e, "GetAllQuestionsUseCase: repository failed")
                    Result.failure(Exception("Nepodařilo se načíst otázky: ${e.message}", e))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "GetAllQuestionsUseCase: unexpected error")
            Result.failure(Exception("Neočekávaná chyba: ${e.message}", e))
        }
    }
}
