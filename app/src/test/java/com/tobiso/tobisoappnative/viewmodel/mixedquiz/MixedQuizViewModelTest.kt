package com.tobiso.tobisoappnative.viewmodel.mixedquiz

import android.app.Application
import com.tobiso.tobisoappnative.QuestionProgressManager
import com.tobiso.tobisoappnative.domain.usecase.GetAllQuestionsUseCase
import com.tobiso.tobisoappnative.fake.FakePointsManager
import com.tobiso.tobisoappnative.model.Answer
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.utils.NetworkUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MixedQuizViewModelTest {

    private lateinit var getAllQuestions: GetAllQuestionsUseCase
    private lateinit var pointsManager: FakePointsManager
    private lateinit var app: Application
    private lateinit var viewModel: MixedQuizViewModel

    private fun makeQuestion(id: Int, correctIdx: Int = 0): Question {
        val answers = listOf(
            Answer(id = id * 10 + 1, answerText = "Správná odpověď", correct = if (correctIdx == 0) 1 else 0, questionId = id),
            Answer(id = id * 10 + 2, answerText = "Špatná odpověď", correct = if (correctIdx == 1) 1 else 0, questionId = id),
        )
        return Question(id = id, questionText = "Otázka $id", postId = 1, answers = answers)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        getAllQuestions = mockk()
        pointsManager = FakePointsManager()
        app = mockk(relaxed = true)
        mockkObject(NetworkUtils)
        every { NetworkUtils.isOnline(any()) } returns true
        QuestionProgressManager.initialize(app)
        viewModel = MixedQuizViewModel(app, getAllQuestions, pointsManager)
    }

    @After
    fun tearDown() {
        unmockkObject(NetworkUtils)
        Dispatchers.setMain(Dispatchers.Default)
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    @Test
    fun `load populates mixedQuestions filtered by ids`() = runBlocking {
        val q1 = makeQuestion(1)
        val q2 = makeQuestion(2)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1, q2) to emptyList())

        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)

        assertEquals(1, viewModel.uiState.value.mixedQuestions.size)
        assertEquals(1, viewModel.uiState.value.mixedQuestions[0].id)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `load with multiple ids keeps all matching questions`() = runBlocking {
        val q1 = makeQuestion(1)
        val q2 = makeQuestion(2)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1, q2) to emptyList())

        viewModel.onIntent(MixedQuizIntent.Load("1,2"))
        delay(100)

        assertEquals(2, viewModel.uiState.value.mixedQuestions.size)
    }

    @Test
    fun `load sets error on use case failure`() = runBlocking {
        coEvery { getAllQuestions() } returns Result.failure(RuntimeException("Nepodařilo se načíst otázky"))

        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.error != null)
    }

    @Test
    fun `load sets isOffline when network unavailable`() = runBlocking {
        every { NetworkUtils.isOnline(any()) } returns false
        coEvery { getAllQuestions() } returns Result.success(emptyList<Question>() to emptyList())

        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)

        assertTrue(viewModel.uiState.value.isOffline)
    }

    // ── StartQuiz ─────────────────────────────────────────────────────────────

    @Test
    fun `startQuiz sets quizStarted and initialises shuffled indices`() = runBlocking {
        val q1 = makeQuestion(1)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)

        viewModel.onIntent(MixedQuizIntent.StartQuiz)

        val state = viewModel.uiState.value
        assertTrue(state.quizStarted)
        assertEquals(1, state.shuffledIndices.size)
        assertEquals(0, state.currentQuestionIndex)
        assertTrue(state.selectedAnswers.isEmpty())
    }

    @Test
    fun `startQuiz with empty questions does nothing`() = runBlocking {
        coEvery { getAllQuestions() } returns Result.success(emptyList<Question>() to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load(""))
        delay(100)

        viewModel.onIntent(MixedQuizIntent.StartQuiz)

        assertFalse(viewModel.uiState.value.quizStarted)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    fun `nextQuestion increments index when not at end`() = runBlocking {
        val q1 = makeQuestion(1)
        val q2 = makeQuestion(2)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1, q2) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1,2"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)

        viewModel.onIntent(MixedQuizIntent.NextQuestion)

        assertEquals(1, viewModel.uiState.value.currentQuestionIndex)
    }

    @Test
    fun `nextQuestion does not exceed last index`() = runBlocking {
        val q1 = makeQuestion(1)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)

        viewModel.onIntent(MixedQuizIntent.NextQuestion)

        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
    }

    @Test
    fun `previousQuestion decrements index when past first`() = runBlocking {
        val q1 = makeQuestion(1)
        val q2 = makeQuestion(2)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1, q2) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1,2"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)
        viewModel.onIntent(MixedQuizIntent.NextQuestion)

        viewModel.onIntent(MixedQuizIntent.PreviousQuestion)

        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
    }

    @Test
    fun `previousQuestion does not go below zero`() = runBlocking {
        val q1 = makeQuestion(1)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)

        viewModel.onIntent(MixedQuizIntent.PreviousQuestion)

        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
    }

    // ── SelectAnswer ──────────────────────────────────────────────────────────

    @Test
    fun `selectAnswer stores answer for display index`() = runBlocking {
        val q1 = makeQuestion(1)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)

        viewModel.onIntent(MixedQuizIntent.SelectAnswer(displayIndex = 0, answerIndex = 1))

        assertEquals(1, viewModel.uiState.value.selectedAnswers[0])
    }

    // ── FinishQuiz ────────────────────────────────────────────────────────────

    @Test
    fun `finishQuiz awards 2 points per correct answer`() = runBlocking {
        val q1 = makeQuestion(1, correctIdx = 0)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)
        viewModel.onIntent(MixedQuizIntent.SelectAnswer(0, 0))

        viewModel.onIntent(MixedQuizIntent.FinishQuiz)

        assertEquals(2, viewModel.uiState.value.awardedPoints)
        assertEquals(2, pointsManager.getPoints())
        assertTrue(viewModel.uiState.value.showResults)
        assertTrue(viewModel.uiState.value.showPointsOverlay)
        assertTrue(viewModel.uiState.value.pointsAwarded)
    }

    @Test
    fun `finishQuiz with all wrong answers awards zero points`() = runBlocking {
        val q1 = makeQuestion(1, correctIdx = 0)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)
        viewModel.onIntent(MixedQuizIntent.SelectAnswer(0, 1))

        viewModel.onIntent(MixedQuizIntent.FinishQuiz)

        assertEquals(0, viewModel.uiState.value.awardedPoints)
        assertEquals(0, pointsManager.getPoints())
        assertFalse(viewModel.uiState.value.showPointsOverlay)
        assertTrue(viewModel.uiState.value.showResults)
    }

    @Test
    fun `finishQuiz second call skips re-awarding points`() = runBlocking {
        val q1 = makeQuestion(1, correctIdx = 0)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)
        viewModel.onIntent(MixedQuizIntent.SelectAnswer(0, 0))
        viewModel.onIntent(MixedQuizIntent.FinishQuiz)

        viewModel.onIntent(MixedQuizIntent.FinishQuiz)

        assertEquals(2, pointsManager.getPoints())
    }

    // ── RestartQuiz ───────────────────────────────────────────────────────────

    @Test
    fun `restartQuiz resets quiz progress state`() = runBlocking {
        val q1 = makeQuestion(1, correctIdx = 0)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)
        viewModel.onIntent(MixedQuizIntent.SelectAnswer(0, 0))
        viewModel.onIntent(MixedQuizIntent.FinishQuiz)

        viewModel.onIntent(MixedQuizIntent.RestartQuiz)

        val state = viewModel.uiState.value
        assertFalse(state.quizStarted)
        assertFalse(state.showResults)
        assertFalse(state.pointsAwarded)
        assertTrue(state.selectedAnswers.isEmpty())
        assertEquals(0, state.awardedPoints)
    }

    // ── DismissPointsOverlay ──────────────────────────────────────────────────

    @Test
    fun `dismissPointsOverlay hides points overlay`() = runBlocking {
        val q1 = makeQuestion(1, correctIdx = 0)
        coEvery { getAllQuestions() } returns Result.success(listOf(q1) to emptyList())
        viewModel.onIntent(MixedQuizIntent.Load("1"))
        delay(100)
        viewModel.onIntent(MixedQuizIntent.StartQuiz)
        viewModel.onIntent(MixedQuizIntent.SelectAnswer(0, 0))
        viewModel.onIntent(MixedQuizIntent.FinishQuiz)
        assertTrue(viewModel.uiState.value.showPointsOverlay)

        viewModel.onIntent(MixedQuizIntent.DismissPointsOverlay)

        assertFalse(viewModel.uiState.value.showPointsOverlay)
    }
}
