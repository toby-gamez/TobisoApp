package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_sessions")
data class AiChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Int,
    val postTitle: String,
    val startedAt: Long,
    val lastMessageAt: Long,
    val lastMessagePreview: String = "",
    val messageCount: Int = 0
)
