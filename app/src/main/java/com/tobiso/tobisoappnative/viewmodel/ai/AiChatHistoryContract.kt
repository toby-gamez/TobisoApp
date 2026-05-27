package com.tobiso.tobisoappnative.viewmodel.ai

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.db.entity.AiChatSessionEntity

data class AiChatHistoryState(
    val sessions: List<AiChatSessionEntity> = emptyList()
) : UiState

sealed interface AiChatHistoryIntent : UiIntent {
    data class DeleteSession(val id: Long) : AiChatHistoryIntent
}

sealed interface AiChatHistoryEffect : UiEffect {
    data class NavigateToChat(
        val postId: Int,
        val postTitle: String,
        val sessionId: Long
    ) : AiChatHistoryEffect
}
