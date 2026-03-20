package com.tobiso.tobisoappnative.model

data class RelatedPost(
    val id: Int,
    val postId: Int,
    val relatedPostId: Int,
    val text: String?,
    val postTitle: String?,
    val relatedPostTitle: String?
)