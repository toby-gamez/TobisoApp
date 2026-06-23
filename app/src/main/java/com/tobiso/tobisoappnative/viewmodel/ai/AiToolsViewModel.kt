package com.tobiso.tobisoappnative.viewmodel.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.AiCreditManager
import com.tobiso.tobisoappnative.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class AiTool {
    FLASHCARDS, REAL_WORLD, WHAT_IF, FEYNMAN, EXPLAIN, PRACTICE
}

data class AiToolsState(
    val activeTool: AiTool? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Flashcards
    val flashcards: List<FlashcardCard> = emptyList(),
    val flashcardIndex: Int = 0,
    val flashcardShowDefinition: Boolean = false,
    // Real World
    val realWorldApps: List<String> = emptyList(),
    // What-If
    val whatIfQuestion: String = "",
    val whatIfScenario: String = "",
    val whatIfExplanation: String = "",
    // Feynman
    val feynmanText: String = "",
    val feynmanScore: Int = -1,
    val feynmanFeedback: String = "",
    val feynmanStrong: List<String> = emptyList(),
    val feynmanMissing: List<String> = emptyList(),
    // Explain Sentence
    val explainInput: String = "",
    val explainResult: String = "",
    // Practice Problems
    val practiceProblems: List<PracticeProblem> = emptyList(),
    val expandedProblemIndex: Int? = null,
)

@HiltViewModel
class AiToolsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(AiToolsState())
    val state: StateFlow<AiToolsState> = _state

    private val clientId = "tobiso-android"
    private val deviceId get() = AiCreditManager.instance.deviceId

    fun openTool(tool: AiTool) {
        _state.update { AiToolsState(activeTool = tool) }
    }

    fun closeTool() {
        _state.update { AiToolsState() }
    }

    // ── Flashcards ────────────────────────────────────────────────────────────

    fun generateFlashcards(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, flashcards = emptyList(), flashcardIndex = 0) }
            try {
                val response = ApiClient.apiService.generateFlashcards(
                    clientId, deviceId, FlashcardRequest(postId)
                )
                _state.update { it.copy(isLoading = false, flashcards = response.cards) }
            } catch (e: Exception) {
                Timber.e(e, "generateFlashcards")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se vygenerovat kartičky.") }
            }
        }
    }

    fun nextFlashcard() = _state.update {
        it.copy(flashcardIndex = (it.flashcardIndex + 1).coerceAtMost(it.flashcards.lastIndex), flashcardShowDefinition = false)
    }

    fun prevFlashcard() = _state.update {
        it.copy(flashcardIndex = (it.flashcardIndex - 1).coerceAtLeast(0), flashcardShowDefinition = false)
    }

    fun flipFlashcard() = _state.update { it.copy(flashcardShowDefinition = !it.flashcardShowDefinition) }

    // ── Real World ────────────────────────────────────────────────────────────

    fun loadRealWorld(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, realWorldApps = emptyList()) }
            try {
                val response = ApiClient.apiService.getRealWorldApplications(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, realWorldApps = response.applications) }
            } catch (e: Exception) {
                Timber.e(e, "loadRealWorld")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se načíst reálné aplikace.") }
            }
        }
    }

    // ── What-If ───────────────────────────────────────────────────────────────

    fun setWhatIfQuestion(q: String) = _state.update { it.copy(whatIfQuestion = q) }

    fun submitWhatIf(postId: Int) {
        val q = _state.value.whatIfQuestion.trim()
        if (q.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, whatIfScenario = "", whatIfExplanation = "") }
            try {
                val response = ApiClient.apiService.getWhatIfScenario(clientId, deviceId, WhatIfRequest(postId, q))
                _state.update { it.copy(isLoading = false, whatIfScenario = response.scenario, whatIfExplanation = response.explanation) }
            } catch (e: Exception) {
                Timber.e(e, "submitWhatIf")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se vygenerovat scénář.") }
            }
        }
    }

    // ── Feynman ───────────────────────────────────────────────────────────────

    fun setFeynmanText(t: String) = _state.update { it.copy(feynmanText = t) }

    fun submitFeynman(postId: Int) {
        val text = _state.value.feynmanText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, feynmanScore = -1, feynmanFeedback = "") }
            try {
                val response = ApiClient.apiService.evaluateComprehension(
                    clientId, deviceId, EvaluateComprehensionRequest(postId, text)
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        feynmanScore = response.score,
                        feynmanFeedback = response.feedback,
                        feynmanStrong = response.strongPoints,
                        feynmanMissing = response.missingPoints
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "submitFeynman")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se vyhodnotit vysvětlení.") }
            }
        }
    }

    // ── Explain Sentence ──────────────────────────────────────────────────────

    fun setExplainInput(t: String) = _state.update { it.copy(explainInput = t) }

    fun clearExplain() = _state.update { it.copy(explainInput = "", explainResult = "", error = null, isLoading = false) }

    fun submitExplain(postId: Int) {
        val sentence = _state.value.explainInput.trim()
        if (sentence.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, explainResult = "") }
            try {
                val response = ApiClient.apiService.explainSentence(
                    clientId, deviceId, ExplainSentenceRequest(postId, sentence)
                )
                _state.update { it.copy(isLoading = false, explainResult = response.explanation) }
            } catch (e: Exception) {
                Timber.e(e, "submitExplain")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se vysvětlit větu.") }
            }
        }
    }

    // ── Practice Problems ─────────────────────────────────────────────────────

    fun generatePracticeProblems(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, practiceProblems = emptyList()) }
            try {
                val response = ApiClient.apiService.generatePracticeProblems(
                    clientId, deviceId, PracticeProblemRequest(postId)
                )
                _state.update { it.copy(isLoading = false, practiceProblems = response.problems) }
            } catch (e: Exception) {
                Timber.e(e, "generatePracticeProblems")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se vygenerovat úlohy.") }
            }
        }
    }

    fun toggleProblemExpanded(index: Int) = _state.update {
        it.copy(expandedProblemIndex = if (it.expandedProblemIndex == index) null else index)
    }
}
