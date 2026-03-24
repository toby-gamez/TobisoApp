package com.tobiso.tobisoappnative.model

import java.util.UUID
import kotlinx.serialization.Serializable

// Datová třída pro útržek
@Serializable
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val postId: Int,
    val content: String,
    val createdAt: Long
)

