package com.example.tobisoappnative.repository

import com.example.tobisoappnative.model.Addendum
import com.example.tobisoappnative.model.InteractiveExerciseResponse
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.model.RelatedPost
import okhttp3.ResponseBody

interface PostDetailRepository {
    suspend fun getPostDetail(postId: Int): Result<Post>
    suspend fun getRelatedPosts(postId: Int, currentPost: Post?, allPosts: List<Post>): Result<List<RelatedPost>>
    suspend fun getAddendums(): Result<List<Addendum>>
    suspend fun getQuestionsForPost(postId: Int): Result<List<Question>>
    suspend fun getExercisesForPost(postId: Int, postCategoryId: Int?): Result<List<InteractiveExerciseResponse>>
    suspend fun downloadPdf(postId: Int): ResponseBody
}
