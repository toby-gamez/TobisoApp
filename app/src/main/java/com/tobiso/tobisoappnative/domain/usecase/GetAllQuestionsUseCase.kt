package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.repository.QuestionsRepository

class GetAllQuestionsUseCase(private val repository: QuestionsRepository) {
    suspend operator fun invoke(): Result<Pair<List<Question>, List<Post>>> =
        repository.getAllQuestions()
}
