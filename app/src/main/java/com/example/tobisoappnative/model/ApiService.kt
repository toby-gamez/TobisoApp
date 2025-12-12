package com.example.tobisoappnative.model

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.ResponseBody
import retrofit2.http.Streaming

interface ApiService {
    @GET("categories")
    suspend fun getCategories(): Array<Category>

    @GET("pages")
    suspend fun getPosts(@Query("categoryId") categoryId: Int? = null): Array<Post>

    @GET("pages/links")
    suspend fun getPostLinks(): Array<PostLink>

    @GET("pages/{id}")
    suspend fun getPost(@Path("id") id: Int): Post

    @GET("Questions/post/{postId}")
    suspend fun getQuestionsByPostId(@Path("postId") postId: Int): Array<Question>

    @GET("Questions")
    suspend fun getAllQuestions(): Array<Question>

    @GET("posts/links")
    suspend fun getPostsForQuestions(): Array<Post>

    @GET("RelatedPosts/by-post/{postId}")
    suspend fun getRelatedPostsByPostId(@Path("postId") postId: Int): Array<RelatedPost>

    @GET("RelatedPosts")
    suspend fun getAllRelatedPosts(): Array<RelatedPost>

    // Event endpoints
    @GET("Events")
    suspend fun getEvents(): Array<Event>

    @GET("Events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Event

    @GET("Events/range")
    suspend fun getEventsInRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Array<Event>

    @GET("Events/search")
    suspend fun searchEvents(@Query("query") query: String): Array<Event>

    // Addendum endpoints
    @GET("Addendums")
    suspend fun getAddendums(): Array<Addendum>

    @GET("Addendums/{id}")
    suspend fun getAddendum(@Path("id") id: Int): Addendum

    // PDF generation
    @Streaming
    @GET("Pdf/generate/post/{id}")
    suspend fun generatePostPdf(@Path("id") id: Int): ResponseBody
}
