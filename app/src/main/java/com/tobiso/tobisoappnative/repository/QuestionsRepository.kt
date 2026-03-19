package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question

interface QuestionsRepository {
    /** Returns all questions paired with the posts they belong to. */
    suspend fun getAllQuestions(): Result<Pair<List<Question>, List<Post>>>
    /** Returns questions scoped to a single post. */
    suspend fun getQuestionsForPost(postId: Int): Result<List<Question>>
}
