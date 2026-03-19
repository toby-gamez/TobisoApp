package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post

interface PostsRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getPostsByCategory(categoryId: Int? = null): Result<List<Post>>
    suspend fun getPost(postId: Int): Result<Post>
    fun isCacheFresh(minutes: Int): Boolean
}
