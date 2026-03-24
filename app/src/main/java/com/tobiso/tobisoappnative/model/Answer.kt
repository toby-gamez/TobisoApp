package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class Answer(
    val id: Int,
    val answerText: String,
    val correct: Int, // 0 = nesprávná, 1 = správná
    val questionId: Int
)