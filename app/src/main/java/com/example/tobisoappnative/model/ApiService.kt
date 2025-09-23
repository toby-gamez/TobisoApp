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

    // Event endpoints
    @GET("Events")
    suspend fun getEvents(): List<Event>

    @GET("Events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Event

    @GET("Events/range")
    suspend fun getEventsInRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): List<Event>

    @GET("Events/search")
    suspend fun searchEvents(@Query("query") query: String): List<Event>
}
