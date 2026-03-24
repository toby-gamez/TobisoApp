package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class RelatedPost(
    val id: Int,
    val postId: Int,
    val relatedPostId: Int,
    val text: String? = null,
    val postTitle: String? = null,
    val relatedPostTitle: String? = null
)