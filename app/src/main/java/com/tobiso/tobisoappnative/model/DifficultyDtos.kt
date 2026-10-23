package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class PostDifficultyResponse(
    val easy: Int = 0,
    val ok: Int = 0,
    val hard: Int = 0,
    val total: Int = 0
)

@Serializable
data class DifficultyRatingRequest(val rating: Int, val deviceId: String)

/** `message` is `"ok"` or `"already_rated"`. */
@Serializable
data class DifficultyRatingResult(val message: String)
