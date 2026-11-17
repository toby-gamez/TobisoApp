package com.tobiso.tobisoappnative.viewmodel.ai

import com.tobiso.tobisoappnative.model.ApiService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.AiCreditManager
import com.tobiso.tobisoappnative.model.*
import com.tobiso.tobisoappnative.model.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class AiTool {
    FLASHCARDS, REAL_WORLD, WHAT_IF, FEYNMAN, EXPLAIN, PRACTICE, EXAM_SUMMARY, EXAM_QUESTIONS, KEY_TERMS, COMPARE, WHY, REWRITE, FORMULAS, CROSS_CONNECTIONS, STEP_SOLVER, CONCEPT_MAP, FUN_FACTS, NOTES, POMODORO
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
    val problemAnswers: Map<Int, String> = emptyMap(),
    val problemFeedback: Map<Int, EvaluateAnswerResponse> = emptyMap(),
    val checkingProblem: Int? = null,
    // Exam summary / fun facts
    val examSummary: String = "",
    val funFacts: List<String> = emptyList(),
    // Exam questions / key terms / compare
    val examQuestions: List<ExamQuestion> = emptyList(),
    val keyTerms: List<KeyTerm> = emptyList(),
    val compareTo: String = "",
    val compareTable: String = "",
    // Why / rewrite / formulas / cross connections
    val whySentence: String = "",
    val whyResult: String = "",
    val grades: List<Grade> = emptyList(),
    val rewriteSelected: String = "",
    val rewriteContent: String = "",
    val formulas: List<FormulaEntry> = emptyList(),
    val crossConnections: List<CrossConnection> = emptyList(),
    // Step solver / concept map
    val solverSteps: List<SolverStep> = emptyList(),
    val conceptNodes: List<ConceptNode> = emptyList(),
    val conceptEdges: List<ConceptEdge> = emptyList(),
    // Notes (signed-in students)
    val noteText: String = "",
    val noteSaved: Boolean = false,
    val noteSaving: Boolean = false,
)

@HiltViewModel
class AiToolsViewModel @Inject constructor(private val api: ApiService) : ViewModel() {

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
                val response = api.generateFlashcards(
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
                val response = api.getRealWorldApplications(clientId, deviceId, postId)
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
                val response = api.getWhatIfScenario(clientId, deviceId, WhatIfRequest(postId, q))
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
                val response = api.evaluateComprehension(
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
                val response = api.explainSentence(
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
                val response = api.generatePracticeProblems(
                    clientId, deviceId, PracticeProblemRequest(postId)
                )
                _state.update { it.copy(isLoading = false, practiceProblems = response.problems) }
            } catch (e: Exception) {
                Timber.e(e, "generatePracticeProblems")
                _state.update { it.copy(isLoading = false, error = "Nepodařilo se vygenerovat úlohy.") }
            }
        }
    }

    fun setProblemAnswer(index: Int, text: String) = _state.update {
        it.copy(problemAnswers = it.problemAnswers + (index to text), problemFeedback = it.problemFeedback - index)
    }

    /** AI-checks the student's own answer to a practice problem against the generated solution. */
    fun checkProblemAnswer(postId: Int, index: Int) {
        val problem = _state.value.practiceProblems.getOrNull(index) ?: return
        val answer = _state.value.problemAnswers[index]?.trim().orEmpty()
        if (answer.isEmpty() || _state.value.checkingProblem != null) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(checkingProblem = index, error = null) }
            try {
                val r = api.evaluateAnswer(clientId, deviceId, EvaluateAnswerRequest(postId, problem.problemText, answer, problem.solution))
                _state.update { it.copy(checkingProblem = null, problemFeedback = it.problemFeedback + (index to r)) }
            } catch (e: Exception) {
                Timber.e(e, "checkProblemAnswer")
                _state.update {
                    it.copy(
                        checkingProblem = null,
                        problemFeedback = it.problemFeedback + (index to EvaluateAnswerResponse(false, "Odpověď se nepodařilo zkontrolovat."))
                    )
                }
            }
        }
    }

    fun toggleProblemExpanded(index: Int) = _state.update {
        it.copy(expandedProblemIndex = if (it.expandedProblemIndex == index) null else index)
    }

    // ── Exam summary ──────────────────────────────────────────────────────────

    fun loadExamSummary(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, examSummary = "") }
            try {
                val response = api.getExamSummary(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, examSummary = response.summary) }
            } catch (e: Exception) {
                Timber.e(e, "loadExamSummary")
                _state.update { it.copy(isLoading = false, error = "Shrnutí se nepodařilo vygenerovat.") }
            }
        }
    }

    // ── Exam questions ────────────────────────────────────────────────────────

    fun loadExamQuestions(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, examQuestions = emptyList()) }
            try {
                val response = api.getExamQuestions(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, examQuestions = response.questions) }
            } catch (e: Exception) {
                Timber.e(e, "loadExamQuestions")
                _state.update { it.copy(isLoading = false, error = "Otázky se nepodařilo vygenerovat.") }
            }
        }
    }

    // ── Key terms ─────────────────────────────────────────────────────────────

    fun loadKeyTerms(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, keyTerms = emptyList()) }
            try {
                val response = api.getKeyTerms(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, keyTerms = response.terms) }
            } catch (e: Exception) {
                Timber.e(e, "loadKeyTerms")
                _state.update { it.copy(isLoading = false, error = "Pojmy se nepodařilo načíst.") }
            }
        }
    }

    // ── Compare ───────────────────────────────────────────────────────────────

    fun setCompareTo(t: String) = _state.update { it.copy(compareTo = t) }

    fun submitCompare(postId: Int) {
        val target = _state.value.compareTo.trim()
        if (target.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, compareTable = "") }
            try {
                val response = api.compareWith(clientId, deviceId, CompareRequest(postId, target))
                _state.update { it.copy(isLoading = false, compareTable = response.markdownTable) }
            } catch (e: Exception) {
                Timber.e(e, "submitCompare")
                _state.update { it.copy(isLoading = false, error = "Srovnání se nepodařilo vytvořit.") }
            }
        }
    }

    // ── Why ───────────────────────────────────────────────────────────────────

    fun setWhySentence(t: String) = _state.update { it.copy(whySentence = t) }

    fun submitWhy(postId: Int) {
        val sentence = _state.value.whySentence.trim()
        if (sentence.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, whyResult = "") }
            try {
                val response = api.explainWhy(clientId, deviceId, WhyRequest(postId, sentence))
                _state.update { it.copy(isLoading = false, whyResult = response.explanation) }
            } catch (e: Exception) {
                Timber.e(e, "submitWhy")
                _state.update { it.copy(isLoading = false, error = "Vysvětlení se nepodařilo vygenerovat.") }
            }
        }
    }

    // ── Rewrite (register / grade) ────────────────────────────────────────────

    fun loadGrades() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val grades = api.getGrades().sortedBy { it.level }
                _state.update { it.copy(grades = grades) }
            } catch (e: Exception) {
                Timber.e(e, "loadGrades")
            }
        }
    }

    /** [key] is "register:<simple|student|expert>" or "grade:<level>"; it also marks the selected chip. */
    fun rewrite(postId: Int, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, rewriteSelected = key, rewriteContent = "") }
            try {
                val kind = key.substringBefore(':')
                val value = key.substringAfter(':')
                val response = if (kind == "grade") {
                    api.rewriteForGrade(clientId, deviceId, RewriteGradeRequest(postId, value.toInt()))
                } else {
                    api.rewriteForRegister(clientId, deviceId, RewriteRegisterRequest(postId, value))
                }
                _state.update { it.copy(isLoading = false, rewriteContent = response.content) }
            } catch (e: Exception) {
                Timber.e(e, "rewrite")
                _state.update { it.copy(isLoading = false, error = "Článek se nepodařilo přepsat.") }
            }
        }
    }

    // ── Formulas ──────────────────────────────────────────────────────────────

    fun loadFormulas(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, formulas = emptyList()) }
            try {
                val response = api.getFormulaVars(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, formulas = response.formulas) }
            } catch (e: Exception) {
                Timber.e(e, "loadFormulas")
                _state.update { it.copy(isLoading = false, error = "Vzorce se nepodařilo načíst.") }
            }
        }
    }

    // ── Cross connections ─────────────────────────────────────────────────────

    fun loadCrossConnections(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, crossConnections = emptyList()) }
            try {
                val response = api.getCrossConnections(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, crossConnections = response.connections) }
            } catch (e: Exception) {
                Timber.e(e, "loadCrossConnections")
                _state.update { it.copy(isLoading = false, error = "Souvislosti se nepodařilo načíst.") }
            }
        }
    }

    // ── Step solver ───────────────────────────────────────────────────────────

    fun loadStepSolver(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, solverSteps = emptyList()) }
            try {
                val response = api.getStepSolver(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, solverSteps = response.steps) }
            } catch (e: Exception) {
                Timber.e(e, "loadStepSolver")
                _state.update { it.copy(isLoading = false, error = "Postup se nepodařilo vygenerovat.") }
            }
        }
    }

    // ── Concept map ───────────────────────────────────────────────────────────

    fun loadConceptMap(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, conceptNodes = emptyList(), conceptEdges = emptyList()) }
            try {
                val response = api.getConceptMap(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, conceptNodes = response.nodes, conceptEdges = response.edges) }
            } catch (e: Exception) {
                Timber.e(e, "loadConceptMap")
                _state.update { it.copy(isLoading = false, error = "Myšlenkovou mapu se nepodařilo vytvořit.") }
            }
        }
    }

    // ── Fun facts ─────────────────────────────────────────────────────────────

    fun loadFunFacts(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, funFacts = emptyList()) }
            try {
                val response = api.getFunFacts(clientId, deviceId, postId)
                _state.update { it.copy(isLoading = false, funFacts = response.facts) }
            } catch (e: Exception) {
                Timber.e(e, "loadFunFacts")
                _state.update { it.copy(isLoading = false, error = "Zajímavosti se nepodařilo načíst.") }
            }
        }
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    fun loadNote(postId: Int) {
        if (!AuthSession.isSignedIn) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, noteSaved = false) }
            try {
                val note = api.getNote(postId)
                _state.update { it.copy(isLoading = false, noteText = note.content) }
            } catch (e: Exception) {
                Timber.e(e, "loadNote")
                _state.update { it.copy(isLoading = false, error = "Poznámku se nepodařilo načíst.") }
            }
        }
    }

    fun setNoteText(t: String) = _state.update { it.copy(noteText = t, noteSaved = false) }

    fun saveNote(postId: Int) {
        val text = _state.value.noteText
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(noteSaving = true, error = null) }
            try {
                val r = api.saveNote(postId, UserNote(text.trim()))
                if (!r.isSuccessful) throw retrofit2.HttpException(r)
                _state.update { it.copy(noteSaving = false, noteSaved = true) }
            } catch (e: Exception) {
                Timber.e(e, "saveNote")
                _state.update { it.copy(noteSaving = false, error = "Poznámku se nepodařilo uložit.") }
            }
        }
    }
}
