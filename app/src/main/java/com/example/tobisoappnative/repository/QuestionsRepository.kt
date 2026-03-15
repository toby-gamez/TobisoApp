package com.example.tobisoappnative.repository

import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question

interface QuestionsRepository {
    /** Returns all questions paired with the posts they belong to. */
    suspend fun getAllQuestions(): Result<Pair<List<Question>, List<Post>>>
    /** Returns questions scoped to a single post. */
    suspend fun getQuestionsForPost(postId: Int): Result<List<Question>>
}
