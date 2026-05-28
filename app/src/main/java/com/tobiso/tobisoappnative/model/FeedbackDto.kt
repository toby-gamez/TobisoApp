package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackDto(
    val name: String,
    val email: String,
    val message: String,
    val platform: String
)
