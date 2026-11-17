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

// ── Exam summary / fun facts / notes ──────────────────────────────────────────
@Serializable
data class ExamSummaryResponse(val summary: String = "")

@Serializable
data class FunFactsResponse(val facts: List<String> = emptyList())

/** `GET ai/exam-questions/{postId}`: likely test questions with model answers. */
@Serializable
data class ExamQuestion(val question: String = "", val answer: String = "")

@Serializable
data class ExamQuestionsResponse(val questions: List<ExamQuestion> = emptyList())

/** `GET ai/key-terms/{postId}`: glossary of the article's key terms. */
@Serializable
data class KeyTerm(val term: String = "", val definition: String = "")

@Serializable
data class KeyTermsResponse(val terms: List<KeyTerm> = emptyList())

/** `POST ai/compare`: AI comparison of the article's topic with [compareTo], returned as a markdown table. */
@Serializable
data class CompareRequest(val postId: Int, val compareTo: String)

@Serializable
data class CompareResponse(val markdownTable: String = "")

/** `POST ai/why`: why a sentence from the article is true. */
@Serializable
data class WhyRequest(val postId: Int, val sentence: String)

@Serializable
data class WhyResponse(val explanation: String = "")

/** `POST ai/rewrite-grade` / `POST ai/rewrite-register`: the article rewritten for another grade or register. */
@Serializable
data class RewriteGradeRequest(val postId: Int, val targetGrade: Int)

/** [register] is "simple" | "student" | "expert". */
@Serializable
data class RewriteRegisterRequest(val postId: Int, val register: String)

@Serializable
data class RewriteResponse(val content: String = "")

/** `GET ai/formula-vars/{postId}`: formulas found in the article with their variables. */
@Serializable
data class FormulaVariable(
    val name: String = "",
    val label: String = "",
    val unit: String = "",
    val min: Double = 0.0,
    val max: Double = 0.0,
    val defaultVal: Double = 0.0,
    val step: Double = 1.0
)

@Serializable
data class FormulaEntry(
    val formula: String = "",
    val expression: String = "",
    val resultVar: String = "",
    val resultUnit: String = "",
    val variables: List<FormulaVariable> = emptyList()
)

@Serializable
data class FormulaVarsResponse(val formulas: List<FormulaEntry> = emptyList())

/** `GET ai/cross-connections/{postId}`: links to other subjects. */
@Serializable
data class CrossConnection(val targetPostTitle: String = "", val subject: String = "", val explanation: String = "")

@Serializable
data class CrossConnectionResponse(val connections: List<CrossConnection> = emptyList())

/** `GET ai/step-solver/{postId}`: a worked solution; [step] is the calculation (often LaTeX), [explanation] says why. */
@Serializable
data class SolverStep(val step: String = "", val explanation: String = "")

@Serializable
data class StepSolverResponse(val steps: List<SolverStep> = emptyList())

/** `GET ai/concept-map/{postId}`: nodes and labelled links of a mind map of the article. */
@Serializable
data class ConceptNode(val id: String = "", val label: String = "")

@Serializable
data class ConceptEdge(val source: String = "", val target: String = "", val label: String = "")

@Serializable
data class ConceptMapResponse(val nodes: List<ConceptNode> = emptyList(), val edges: List<ConceptEdge> = emptyList())

/** `GET/PUT user/notes/{postId}`: the signed-in student's private note on a post. */
@Serializable
data class UserNote(val content: String = "")

// ── Answer check ──────────────────────────────────────────────────────────────
@Serializable
data class EvaluateAnswerRequest(
    val postId: Int,
    val questionText: String,
    val studentAnswer: String,
    val correctAnswer: String
)

@Serializable
data class EvaluateAnswerResponse(val isCorrect: Boolean = false, val feedback: String = "")
