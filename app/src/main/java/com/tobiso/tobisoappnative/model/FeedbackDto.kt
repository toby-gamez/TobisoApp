package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

/** Matches the server's FeedbackType enum (serialized as a string). */
@Serializable
enum class FeedbackType { Bug, FeatureRequest }

@Serializable
data class FeedbackDto(
    val name: String,
    val email: String,
    val title: String,
    val message: String,
    val platform: String,
    val type: FeedbackType
)
