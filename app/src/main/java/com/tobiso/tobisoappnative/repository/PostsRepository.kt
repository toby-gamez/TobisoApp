package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.PostSummaryResponse
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.model.RelatedPost
import okhttp3.ResponseBody

interface PostsRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getPostsByCategory(categoryId: Int? = null, gradeId: Int? = null): Result<List<Post>>
    suspend fun getPost(postId: Int, gradeId: Int? = null): Result<Post>
    suspend fun getPostSummaries(): Result<List<PostSummaryResponse>>
    suspend fun getGrades(): Result<List<Grade>>
    fun isCacheFresh(minutes: Int): Boolean
    suspend fun getRelatedPosts(postId: Int, currentPost: Post?, allPosts: List<Post>): Result<List<RelatedPost>>
    suspend fun getAddendums(): Result<List<Addendum>>
    suspend fun getQuestionsForPost(postId: Int): Result<List<Question>>
    suspend fun getExercisesForPost(postId: Int, postCategoryId: Int?): Result<List<InteractiveExerciseResponse>>
    suspend fun downloadPdf(postId: Int): ResponseBody
}
