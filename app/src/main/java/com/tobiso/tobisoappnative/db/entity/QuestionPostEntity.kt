package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.PostVersion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Separate table for posts fetched via getPostsForQuestions() endpoint.
 * Kept separate from [PostEntity] to preserve the distinct data sets.
 */
@Entity(tableName = "questions_posts")
data class QuestionPostEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val content: String?,
    val filePath: String,
    val createdAt: String? = null,
    val lastFix: String? = null,
    val lastEdit: String? = null,
    val categoryId: Int? = null,
    val versionsJson: String? = null
)

fun QuestionPostEntity.toDomain(): Post = Post(
    id = id,
    title = title,
    content = content,
    filePath = filePath,
    createdAt = createdAt,
    lastFix = lastFix,
    lastEdit = lastEdit,
    categoryId = categoryId,
    versions = versionsJson?.let { runCatching { Json.decodeFromString<List<PostVersion>>(it) }.getOrNull() }
)

fun Post.toQuestionPostEntity(): QuestionPostEntity = QuestionPostEntity(
    id = id,
    title = title,
    content = content,
    filePath = filePath,
    createdAt = createdAt,
    lastFix = lastFix,
    lastEdit = lastEdit,
    categoryId = categoryId,
    versionsJson = versions?.let { runCatching { Json.encodeToString(it) }.getOrNull() }
)
