package com.example.tobisoappnative.domain.usecase

import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.repository.QuestionsRepository

class GetAllQuestionsUseCase(private val repository: QuestionsRepository) {
    suspend operator fun invoke(): Result<Pair<List<Question>, List<Post>>> =
        repository.getAllQuestions()
}
