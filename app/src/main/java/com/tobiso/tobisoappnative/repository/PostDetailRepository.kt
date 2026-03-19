package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.model.RelatedPost
import okhttp3.ResponseBody

interface PostDetailRepository {
    suspend fun getPostDetail(postId: Int): Result<Post>
    suspend fun getRelatedPosts(postId: Int, currentPost: Post?, allPosts: List<Post>): Result<List<RelatedPost>>
    suspend fun getAddendums(): Result<List<Addendum>>
    suspend fun getQuestionsForPost(postId: Int): Result<List<Question>>
    suspend fun getExercisesForPost(postId: Int, postCategoryId: Int?): Result<List<InteractiveExerciseResponse>>
    suspend fun downloadPdf(postId: Int): ResponseBody
}
