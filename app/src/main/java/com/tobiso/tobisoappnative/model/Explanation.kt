package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class Explanation(
    val id: Int,
    val text: String,
    val questionId: Int
)