package com.tobiso.tobisoappnative.model

import java.util.UUID

// Datová třída pro útržek
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val postId: Int,
    val content: String,
    val createdAt: Long
)

