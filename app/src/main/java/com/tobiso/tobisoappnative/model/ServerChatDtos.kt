package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** `GET ai/history`: a signed-in student's chat sessions stored on the server (shared with the website). */
@Serializable
data class ServerChatSession(
    val id: Int,
    val postId: Int? = null,
    val postTitle: String? = null,
    val title: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val attachedPostIds: List<Int> = emptyList()
)

/** `GET ai/history/{id}` */
@Serializable
data class ServerChatMessage(
    val id: Int = 0,
    val role: String,
    val content: String,
    val createdAt: String? = null
)

/** Server timestamps are UTC without an offset suffix. */
fun parseServerInstant(raw: String?): Long = raw?.let {
    runCatching { Instant.parse(it) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(it.take(26)).toInstant(ZoneOffset.UTC) }.getOrNull()
}?.toEpochMilli() ?: 0L
