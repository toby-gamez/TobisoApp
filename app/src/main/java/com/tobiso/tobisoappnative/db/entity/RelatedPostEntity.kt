package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.RelatedPost

@Entity(
    tableName = "related_posts",
    indices = [Index(value = ["postId"])]
)
data class RelatedPostEntity(
    @PrimaryKey val id: Int,
    val postId: Int,
    val relatedPostId: Int,
    val text: String?,
    val postTitle: String? = null,
    val relatedPostTitle: String? = null
)

fun RelatedPostEntity.toDomain(): RelatedPost = RelatedPost(
    id = id,
    postId = postId,
    relatedPostId = relatedPostId,
    text = text,
    postTitle = postTitle,
    relatedPostTitle = relatedPostTitle
)

fun RelatedPost.toEntity(): RelatedPostEntity = RelatedPostEntity(
    id = id,
    postId = postId,
    relatedPostId = relatedPostId,
    text = text,
    postTitle = postTitle,
    relatedPostTitle = relatedPostTitle
)
