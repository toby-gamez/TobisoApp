package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.tobiso.tobisoappnative.model.Category

private val gson = Gson()

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val slug: String? = null,
    val parentId: Int? = null,
    val parentJson: String? = null,     // Gson-serialized Category?
    val childrenJson: String? = null,   // Gson-serialized List<Category>?
    val fullPath: String? = null
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    slug = slug,
    parentId = parentId,
    parent = parentJson?.let { gson.fromJson(it, Category::class.java) },
    children = childrenJson?.let { gson.fromJson(it, Array<Category>::class.java)?.toList() },
    fullPath = fullPath
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    slug = slug,
    parentId = parentId,
    parentJson = parent?.let { gson.toJson(it) },
    childrenJson = children?.let { gson.toJson(it) },
    fullPath = fullPath
)
