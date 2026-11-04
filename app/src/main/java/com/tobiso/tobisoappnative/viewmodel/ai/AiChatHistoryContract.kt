package com.tobiso.tobisoappnative.viewmodel.ai

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState

/** A chat in the history list, whether it lives on the device (not signed in) or on the server (signed in). */
data class ChatSessionItem(
    val id: Long,
    val postId: Int,
    val postTitle: String,
    val lastMessagePreview: String,
    val lastMessageAt: Long,
    val messageCount: Int
)

data class AiChatHistoryState(
    val sessions: List<ChatSessionItem> = emptyList(),
    /** True when [sessions] come from the server: shared with the website, cannot be deleted here. */
    val remote: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState

sealed interface AiChatHistoryIntent : UiIntent {
    data class DeleteSession(val id: Long) : AiChatHistoryIntent
    object Reload : AiChatHistoryIntent
}

sealed interface AiChatHistoryEffect : UiEffect {
    data class NavigateToChat(
        val postId: Int,
        val postTitle: String,
        val sessionId: Long
    ) : AiChatHistoryEffect
}
