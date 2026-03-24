package com.tobiso.tobisoappnative.model

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.ResponseBody
import retrofit2.http.Streaming
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {
    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("pages")
    suspend fun getPosts(@Query("categoryId") categoryId: Int? = null): List<Post>

    @GET("pages/links")
    suspend fun getPostLinks(): List<PostLink>

    @GET("pages/{id}")
    suspend fun getPost(@Path("id") id: Int): Post

    @GET("Questions/post/{postId}")
    suspend fun getQuestionsByPostId(@Path("postId") postId: Int): List<Question>

    @GET("Questions")
    suspend fun getAllQuestions(): List<Question>

    @GET("posts/links")
    suspend fun getPostsForQuestions(): List<Post>

    @GET("RelatedPosts/by-post/{postId}")
    suspend fun getRelatedPostsByPostId(@Path("postId") postId: Int): List<RelatedPost>

    @GET("RelatedPosts")
    suspend fun getAllRelatedPosts(): List<RelatedPost>

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

    // Addendum endpoints
    @GET("Addendums")
    suspend fun getAddendums(): List<Addendum>

    @GET("Addendums/{id}")
    suspend fun getAddendum(@Path("id") id: Int): Addendum

    // PDF generation
    @Streaming
    @GET("Pdf/generate/post/{id}")
    suspend fun generatePostPdf(@Path("id") id: Int): ResponseBody

    // Feedback endpoint
    @POST("Feedback")
    suspend fun sendFeedback(@Body feedback: FeedbackDto)

    // Interactive Exercise endpoints
    @GET("InteractiveExercises/post/{postId}")
    suspend fun getExercisesByPostId(@Path("postId") postId: Int): List<InteractiveExerciseResponse>

    @GET("InteractiveExercises/{id}")
    suspend fun getExercise(@Path("id") id: Int): InteractiveExerciseResponse

    @POST("InteractiveExercises/{id}/validate")
    suspend fun validateExercise(
        @Path("id") id: Int,
        @Body request: ValidateSolutionRequest
    ): ExerciseValidationResult

    // AI chat endpoint
    @POST("ai/ask")
    suspend fun askAi(
        @retrofit2.http.Header("X-Client-Id") clientId: String,
        @Body request: AiChatRequest
    ): AiChatResponse
}
