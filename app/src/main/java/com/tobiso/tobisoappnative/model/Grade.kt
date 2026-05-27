package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class Grade(
    val id: Int,
    val name: String,
    val level: Int
)
