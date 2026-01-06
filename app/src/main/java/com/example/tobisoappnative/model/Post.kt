package com.example.tobisoappnative.model

data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val filePath: String,
    val createdAt: String?,
    // Server no longer provides `updatedAt`. New fields:
    val lastFix: String?,
    val lastEdit: String?,
    val categoryId: Int?,
)
