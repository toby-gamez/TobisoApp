package com.example.tobisoappnative.repository

import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post

interface PostsRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getPostsByCategory(categoryId: Int? = null): Result<List<Post>>
    suspend fun getPost(postId: Int): Result<Post>
    fun isCacheFresh(minutes: Int): Boolean
}
