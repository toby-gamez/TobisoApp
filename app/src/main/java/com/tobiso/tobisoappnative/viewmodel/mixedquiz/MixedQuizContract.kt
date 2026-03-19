package com.tobiso.tobisoappnative.viewmodel.mixedquiz

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question

data class MixedQuizState(
    val allQuestions: List<Question> = emptyList(),
    val mixedQuestions: List<Question> = emptyList(),
    val questionsPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    // Quiz progress
    val quizStarted: Boolean = false,
    val shuffledIndices: List<Int> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val textAnswers: Map<Int, String> = emptyMap(),
    val showResults: Boolean = false,
    val pointsAwarded: Boolean = false,
    val showPointsOverlay: Boolean = false,
    val awardedPoints: Int = 0
) : UiState

sealed interface MixedQuizIntent : UiIntent {
    data class Load(val questionIdsCsv: String) : MixedQuizIntent
    object Refresh : MixedQuizIntent
    object StartQuiz : MixedQuizIntent
    data class SelectAnswer(val displayIndex: Int, val answerIndex: Int) : MixedQuizIntent
    data class SetTextAnswer(val displayIndex: Int, val text: String) : MixedQuizIntent
    object NextQuestion : MixedQuizIntent
    object PreviousQuestion : MixedQuizIntent
    object FinishQuiz : MixedQuizIntent
    object RestartQuiz : MixedQuizIntent
    object DismissPointsOverlay : MixedQuizIntent
}

sealed interface MixedQuizEffect : UiEffect {
    object NavigateBack : MixedQuizEffect
}
