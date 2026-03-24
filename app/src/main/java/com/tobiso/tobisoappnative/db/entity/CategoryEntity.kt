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
    val parentJson: String? = null,     // kotlinx.serialization-serialized Category?
    val childrenJson: String? = null,   // kotlinx.serialization-serialized List<Category>?
    val fullPath: String? = null
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    slug = slug,
    parentId = parentId,
    parent = parentJson?.let { try { json.decodeFromString<Category>(it) } catch (e: Exception) { null } },
    children = childrenJson?.let { try { json.decodeFromString<List<Category>>(it) } catch (e: Exception) { null } },
    fullPath = fullPath
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    slug = slug,
    parentId = parentId,
    parentJson = parent?.let { json.encodeToString(Category.serializer(), it) },
    childrenJson = children?.let { json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Category.serializer()), it) },
    fullPath = fullPath
)
