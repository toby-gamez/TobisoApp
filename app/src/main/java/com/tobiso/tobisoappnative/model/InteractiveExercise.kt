package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNames

// Hlavní DTO pro interaktivní cvičení
@Serializable
data class InteractiveExerciseResponse(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String, // "circuit", "timeline", "drag-drop", "matching"
    @SerialName("configJson") val configJson: String,
    @SerialName("instructionsMarkdown") val instructionsMarkdown: String?,
    @SerialName("postIds") val postIds: List<Int>? = null,
    @SerialName("categoryIds") val categoryIds: List<Int>? = null,
    @SerialName("isActive") val isActive: Boolean? = true
)

// Request pro validaci řešení
@Serializable
data class ValidateSolutionRequest(
    @SerialName("userSolutionJson") val userSolutionJson: String
)

// Výsledek validace
@Serializable
data class ExerciseValidationResult(
    @SerialName("isCorrect") val isCorrect: Boolean,
    @SerialName("score") val score: Int,
    @SerialName("feedback") val feedback: String,
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("detailedResults") val detailedResults: Map<String, Boolean>? = null
)

// Config modely pro jednotlivé typy cvičení

// Timeline Exercise Config
@Serializable
data class TimelineConfig(
    @SerialName("timeRange") val timeRange: TimeRange,
    @SerialName("events") val events: List<TimelineEvent>
)

@Serializable
data class TimeRange(
    @SerialName("start") val start: Int,
    @SerialName("end") val end: Int
)

@Serializable
data class TimelineEvent(
    @SerialName("id") val id: String,
    @JsonNames("label", "text", "name")
    @SerialName("label") val label: String,
    @SerialName("year") val year: Int? = null
)

// Timeline User Solution
@Serializable
data class TimelineSolution(
    @SerialName("order") val order: List<String>
)

// Drag-Drop Exercise Config
@Serializable
data class DragDropConfig(
    @SerialName("categories") val categories: List<DragDropCategory>,
    @SerialName("items") val items: List<DragDropItem>
)

@Serializable
data class DragDropCategory(
    @SerialName("id") val id: String,
    @JsonNames("label", "name")
    @SerialName("label") val label: String
)

@Serializable
data class DragDropItem(
    @SerialName("id") val id: String,
    @JsonNames("text", "label", "name")
    @SerialName("text") val text: String
)

// Drag-Drop User Solution
@Serializable
data class DragDropSolution(
    @SerialName("placements") val placements: Map<String, String>
)

// Matching Exercise Config
@Serializable
data class MatchingConfig(
    @SerialName("left") val left: List<MatchingItem>,
    @SerialName("right") val right: List<MatchingItem>
)

@Serializable
data class MatchingItem(
    @SerialName("id") val id: String,
    @JsonNames("text", "label", "name")
    @SerialName("text") val text: String
)

// Matching User Solution
@Serializable
data class MatchingSolution(
    @SerialName("pairs") val pairs: List<MatchingPair>
)

@Serializable
data class MatchingPair(
    @SerialName("leftId") val leftId: String,
    @SerialName("rightId") val rightId: String
)

// Circuit Exercise - user solution
@Serializable
data class CircuitSolution(
    @SerialName("connections") val connections: List<CircuitConnection>
)

@Serializable
data class CircuitConnection(
    @SerialName("from") val from: String,
    @SerialName("to") val to: String
)

// Optional config shapes for prebuilt circuits
@Serializable
data class CircuitConfig(
    @SerialName("components") val components: List<CircuitComponentInstance> = emptyList(),
    @SerialName("connections") val connections: List<CircuitConnectionInstance> = emptyList()
)

@Serializable
data class CircuitComponentInstance(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("label") val label: String,
    @SerialName("value") val value: Double = 0.0,
    @SerialName("x") val x: Double = 0.0,
    @SerialName("y") val y: Double = 0.0
)

@Serializable
data class CircuitConnectionInstance(
    @SerialName("id") val id: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String
)
