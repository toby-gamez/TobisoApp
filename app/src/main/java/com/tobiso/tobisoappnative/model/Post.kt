package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int,
    val title: String,
    val content: String? = null,
    val filePath: String,
    val createdAt: String? = null,
    // Server no longer provides `updatedAt`. New fields:
    val lastFix: String? = null,
    val lastEdit: String? = null,
    val categoryId: Int? = null,
)
