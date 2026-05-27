package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable
@Serializable
data class Post(
    val id: Int,
    val title: String,
    val content: String? = null,
    val filePath: String,
    val createdAt: String? = null,
    val lastFix: String? = null,
    val lastEdit: String? = null,
    val categoryId: Int? = null,
    val versions: List<PostVersion>? = null
) {
    val activeVersion: PostVersion? get() = versions?.firstOrNull()
    val activeContent: String? get() = activeVersion?.content ?: content
    val activeLastFix: String? get() = activeVersion?.lastFix ?: lastFix
    val activeLastEdit: String? get() = activeVersion?.lastEdit ?: lastEdit
}
