package com.example.tobisoappnative.model

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("posts")
    suspend fun getPosts(@Query("categoryId") categoryId: Int? = null): List<Post>

    @GET("posts/links")
    suspend fun getPostLinks(): List<PostLink>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Post

    @GET("Questions/post/{postId}")
    suspend fun getQuestionsByPostId(@Path("postId") postId: Int): List<Question>
}
