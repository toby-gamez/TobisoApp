package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class Addendum(
    val id: Int,
    val name: String? = null,
    val content: String? = null,
    val updatedAt: String? = null
)
