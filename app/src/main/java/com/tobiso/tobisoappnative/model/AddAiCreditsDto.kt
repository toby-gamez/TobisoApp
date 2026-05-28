package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class AddAiCreditsRequest(
    val deviceId: String,
    val count: Int,
    val validUntilUtc: Long,
    val signature: String
)

@Serializable
data class AddAiCreditsResponse(
    val success: Boolean,
    val totalRemainingToday: Int
)
