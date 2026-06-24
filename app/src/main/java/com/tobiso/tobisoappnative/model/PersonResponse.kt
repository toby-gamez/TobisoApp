package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class PersonResponse(
    val name: String,
    val slug: String? = null,
    val bio: String? = null,
    val role: String? = null,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val externalLink: String? = null,
    val photoUrl: String? = null,
    val aiGenerated: Boolean = false
)
