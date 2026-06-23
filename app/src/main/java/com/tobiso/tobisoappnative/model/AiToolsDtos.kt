package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

// ── Flashcards ────────────────────────────────────────────────────────────────
@Serializable
data class FlashcardRequest(val postId: Int)

@Serializable
data class FlashcardCard(val term: String, val definition: String)

@Serializable
data class FlashcardResponse(val cards: List<FlashcardCard>)

// ── Real-World Applications ───────────────────────────────────────────────────
@Serializable
data class RealWorldResponse(val applications: List<String>)

// ── What-If Scenarios ─────────────────────────────────────────────────────────
@Serializable
data class WhatIfRequest(val postId: Int, val userQuestion: String)

@Serializable
data class WhatIfResponse(val scenario: String, val explanation: String)

// ── Feynman Technique ─────────────────────────────────────────────────────────
@Serializable
data class EvaluateComprehensionRequest(val postId: Int, val studentExplanation: String)

@Serializable
data class EvaluateComprehensionResponse(
    val feedback: String,
    val score: Int,
    val strongPoints: List<String>,
    val missingPoints: List<String>
)

// ── Explain Sentence ──────────────────────────────────────────────────────────
@Serializable
data class ExplainSentenceRequest(val postId: Int, val sentence: String)

@Serializable
data class ExplainSentenceResponse(val explanation: String)

// ── Practice Problems ─────────────────────────────────────────────────────────
@Serializable
data class PracticeProblemRequest(val postId: Int, val count: Int = 3)

@Serializable
data class PracticeProblem(
    val problemText: String,
    val solution: String,
    val difficulty: String
)

@Serializable
data class PracticeProblemResponse(val problems: List<PracticeProblem>)
