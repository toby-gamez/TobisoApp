package com.tobiso.tobisoappnative.model

data class AiChatMessageDto(
    val role: String,
    val content: String
)

data class AiChatRequest(
    val postId: Int,
    val question: String,
    val conversationHistory: List<AiChatMessageDto>
)

data class AiChatResponse(
    val answer: String,
    val remainingQuestions: Int
)
