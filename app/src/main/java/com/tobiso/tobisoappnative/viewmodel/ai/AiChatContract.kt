package com.tobiso.tobisoappnative.viewmodel.ai

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

data class AiChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val remainingQuestions: Int? = null,
    val error: String? = null,
    val limitReached: Boolean = false
) : UiState

sealed interface AiChatIntent : UiIntent {
    data class SendMessage(val text: String) : AiChatIntent
}

sealed interface AiChatEffect : UiEffect
