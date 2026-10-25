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

/** `POST user/points/claim`: the server owns the price list; [ref] is the shop item id as text. Spends exchange points. */
@Serializable
data class PointsClaimRequest(val reason: String, val ref: String)

@Serializable
data class PointsClaimResponse(val credits: Int = 0, val points: Int = 0)

/** One answered quiz question for server-side grading: picked answer ids, or [text] for free-text questions. */
@Serializable
data class QuizAnswerSubmission(val questionId: Int, val answerIds: List<Int>? = null, val text: String? = null)

/** `POST user/points/earn`: the server grades the answers and decides the amount; the app never sends points. */
@Serializable
data class PointsEarnRequest(val answers: List<QuizAnswerSubmission>)

@Serializable
data class PointsEarnResponse(
    val correct: Int = 0, val awarded: Int = 0, val balance: Int = 0,
    val earnedToday: Int = 0, val dailyEarnCap: Int = 0
)

/** `GET user/points`: server-owned exchange points (separate from the local shop points). */
@Serializable
data class PointsWalletResponse(val balance: Int = 0, val earnedToday: Int = 0, val dailyEarnCap: Int = 0)

/** `GET posts/{id}/versions`: which grade versions of an article exist (no content). */
@Serializable
data class PostVersionInfo(
    val id: Int = 0,
    val gradeId: Int? = null,
    val gradeName: String? = null,
    val gradeLevel: Int? = null
)
