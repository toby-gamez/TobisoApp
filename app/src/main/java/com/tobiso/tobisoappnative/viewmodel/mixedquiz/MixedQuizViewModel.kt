package com.tobiso.tobisoappnative.viewmodel.mixedquiz

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.base.BaseAndroidViewModel
import com.tobiso.tobisoappnative.domain.usecase.GetAllQuestionsUseCase
import com.tobiso.tobisoappnative.utils.NetworkUtils
import com.tobiso.tobisoappnative.utils.normalizeText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MixedQuizViewModel @Inject constructor(
    application: Application,
    private val getAllQuestions: GetAllQuestionsUseCase
) : BaseAndroidViewModel<MixedQuizState, MixedQuizIntent, MixedQuizEffect>(
    application, MixedQuizState()
) {

    override fun onIntent(intent: MixedQuizIntent) = when (intent) {
        is MixedQuizIntent.Load -> load(intent.questionIdsCsv)
        MixedQuizIntent.Refresh -> refresh()
        MixedQuizIntent.StartQuiz -> startQuiz()
        is MixedQuizIntent.SelectAnswer -> selectAnswer(intent.displayIndex, intent.answerIndex)
        is MixedQuizIntent.SetTextAnswer -> setTextAnswer(intent.displayIndex, intent.text)
        MixedQuizIntent.NextQuestion -> nextQuestion()
        MixedQuizIntent.PreviousQuestion -> previousQuestion()
        MixedQuizIntent.FinishQuiz -> finishQuiz()
        MixedQuizIntent.RestartQuiz -> restartQuiz()
        MixedQuizIntent.DismissPointsOverlay -> setState { copy(showPointsOverlay = false) }
    }

    private fun load(questionIdsCsv: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isOffline = !NetworkUtils.isOnline(getApplication())
            setState { copy(isLoading = true, isOffline = isOffline) }
            getAllQuestions()
                .onSuccess { (questions, posts) ->
                    val ids = questionIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
                    val filtered = questions.filter { it.id in ids }
                    setState {
                        copy(
                            isLoading = false,
                            allQuestions = questions,
                            questionsPosts = posts,
                            mixedQuestions = filtered,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    setState { copy(isLoading = false, error = e.message ?: "Chyba při načítání otázek") }
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (NetworkUtils.isOnline(getApplication())) {
                setState { copy(isRefreshing = true, isOffline = false) }
                getAllQuestions()
                    .onSuccess { (questions, posts) ->
                        val ids = currentState.mixedQuestions.map { it.id }.toSet()
                        val filtered = if (ids.isEmpty()) questions else questions.filter { it.id in ids }
                        setState {
                            copy(isRefreshing = false, allQuestions = questions, questionsPosts = posts, mixedQuestions = filtered)
                        }
                    }
                    .onFailure { setState { copy(isRefreshing = false) } }
            } else {
                setState { copy(isRefreshing = false, isOffline = true) }
            }
        }
    }

    private fun startQuiz() {
        val questions = currentState.mixedQuestions
        if (questions.isEmpty()) return
        setState {
            copy(
                quizStarted = true,
                shuffledIndices = questions.indices.shuffled(),
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                textAnswers = emptyMap(),
                showResults = false,
                pointsAwarded = false,
                showPointsOverlay = false,
                awardedPoints = 0
            )
        }
    }

    private fun selectAnswer(displayIndex: Int, answerIndex: Int) {
        setState { copy(selectedAnswers = selectedAnswers + (displayIndex to answerIndex)) }
    }

    private fun setTextAnswer(displayIndex: Int, text: String) {
        setState { copy(textAnswers = textAnswers + (displayIndex to text)) }
    }

    private fun nextQuestion() {
        val s = currentState
        if (s.currentQuestionIndex < s.shuffledIndices.size - 1) {
            setState { copy(currentQuestionIndex = currentQuestionIndex + 1) }
        }
    }

    private fun previousQuestion() {
        if (currentState.currentQuestionIndex > 0) {
            setState { copy(currentQuestionIndex = currentQuestionIndex - 1) }
        }
    }

    private fun finishQuiz() {
        val s = currentState
        if (s.pointsAwarded) { setState { copy(showResults = true) }; return }

        val correctAnswers = s.shuffledIndices.mapIndexed { displayIndex, questionIndex ->
            if (questionIndex >= s.mixedQuestions.size) return@mapIndexed false
            val q = s.mixedQuestions[questionIndex]
            if (q.isTextQuestion) {
                normalizeText(s.textAnswers[displayIndex]?.trim() ?: "") ==
                        normalizeText(q.correctTextAnswer?.trim() ?: "")
            } else {
                val sel = s.selectedAnswers[displayIndex]
                sel != null && sel >= 0 && sel < q.options.size && sel == q.correctAnswer
            }
        }.count { it }

        val points = correctAnswers * 2
        if (points > 0) {
            PointsManager.addPoints(points)
        }
        setState { copy(showResults = true, pointsAwarded = true, awardedPoints = points, showPointsOverlay = points > 0) }
    }

    private fun restartQuiz() {
        setState {
            copy(
                quizStarted = false,
                shuffledIndices = emptyList(),
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                textAnswers = emptyMap(),
                showResults = false,
                pointsAwarded = false,
                showPointsOverlay = false,
                awardedPoints = 0
            )
        }
    }
}
