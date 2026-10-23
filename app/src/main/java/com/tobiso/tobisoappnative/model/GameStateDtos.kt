package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `GET user/game-state` (204 when the account never stored anything). */
@Serializable
data class GameStateResponse(
    val revision: Int = 0,
    val updatedAt: String? = null,
    val state: JsonObject = JsonObject(emptyMap())
)

/** `PUT user/game-state`; [baseRevision] is 0 for the very first write. */
@Serializable
data class GameStatePutRequest(val baseRevision: Int, val state: JsonObject)

@Serializable
data class GameStatePutResponse(val revision: Int = 0, val updatedAt: String? = null)

/** `POST user/points/claim`: the server owns the price list; [ref] is the shop item id as text. */
@Serializable
data class PointsClaimRequest(val reason: String, val ref: String)

@Serializable
data class PointsClaimResponse(val credits: Int = 0, val points: Float = 0f, val revision: Int = 0)

/** `GET posts/{id}/versions`: which grade versions of an article exist (no content). */
@Serializable
data class PostVersionInfo(
    val id: Int = 0,
    val gradeId: Int? = null,
    val gradeName: String? = null,
    val gradeLevel: Int? = null
)
