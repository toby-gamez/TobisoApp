package com.tobiso.tobisoappnative.model

data class Category(
    val id: Int,
    val name: String,
    val slug: String? = null,
    val parentId: Int? = null,
    val parent: Category? = null,
    val children: List<Category>? = null,
    val fullPath: String? = null
)
