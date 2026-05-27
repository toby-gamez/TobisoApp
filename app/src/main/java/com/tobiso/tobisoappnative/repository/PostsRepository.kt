package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.PostSummaryResponse

interface PostsRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getPostsByCategory(categoryId: Int? = null, gradeId: Int? = null): Result<List<Post>>
    suspend fun getPost(postId: Int, gradeId: Int? = null): Result<Post>
    suspend fun getPostSummaries(): Result<List<PostSummaryResponse>>
    suspend fun getGrades(): Result<List<Grade>>
    fun isCacheFresh(minutes: Int): Boolean
}
