package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class PostVersion(
    val id: Int,
    val postId: Int,
    val gradeId: Int,
    val gradeName: String? = null,
    val gradeLevel: Int? = null,
    val content: String,
    val lastFix: String? = null,
    val lastEdit: String? = null
)
