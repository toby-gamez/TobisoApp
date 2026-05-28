package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.Category
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val slug: String? = null,
    val parentId: Int? = null,
    val parentJson: String? = null,
    val childrenJson: String? = null,
    val fullPath: String? = null
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    slug = slug,
    parentId = parentId,
    parent = null,
    children = null,
    fullPath = fullPath
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    slug = slug,
    parentId = parentId,
    parentJson = null,
    childrenJson = null,
    fullPath = fullPath
)
