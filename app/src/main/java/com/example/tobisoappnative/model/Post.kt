package com.example.tobisoappnative.model

data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val filePath: String,
    val createdAt: String?,
    val updatedAt: String?,
    val categoryId: Int?,
)
