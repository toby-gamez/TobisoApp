package com.tobiso.tobisoappnative.model
import kotlinx.serialization.Serializable

@Serializable
data class AiChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class AiChatRequest(
    val postId: Int,
    val question: String,
    val conversationHistory: List<AiChatMessageDto>
)

@Serializable
data class AiChatResponse(
    val answer: String,
    val remainingQuestions: Int
)
