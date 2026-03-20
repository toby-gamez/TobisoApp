package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.Answer
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.repository.QuestionsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetAllQuestionsUseCaseTest {

    private lateinit var repository: QuestionsRepository
    private lateinit var useCase: GetAllQuestionsUseCase

    private val samplePost = Post(
        id = 1,
        title = "Druhá světová válka",
        content = null,
        filePath = "wwii.md",
        createdAt = "2024-01-01",
        lastFix = null,
        lastEdit = null,
        categoryId = 2
    )

    private val sampleQuestion = Question(
        id = 10,
        questionText = "Ve kterém roce skončila druhá světová válka?",
        postId = 1,
        answers = listOf(
            Answer(id = 1, answerText = "1943", correct = 0, questionId = 10),
            Answer(id = 2, answerText = "1945", correct = 1, questionId = 10),
            Answer(id = 3, answerText = "1948", correct = 0, questionId = 10),
        )
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetAllQuestionsUseCase(repository)
    }

    @Test
    fun `returns questions and posts on success`() = runTest {
        coEvery { repository.getAllQuestions() } returns
            Result.success(Pair(listOf(sampleQuestion), listOf(samplePost)))

        val result = useCase()

        assertTrue(result.isSuccess)
        val (questions, posts) = result.getOrNull()!!
        assertEquals(1, questions.size)
        assertEquals(1, posts.size)
        assertEquals("Ve kterém roce skončila druhá světová válka?", questions.first().questionText)
    }

    @Test
    fun `returns empty lists when data source is empty`() = runTest {
        coEvery { repository.getAllQuestions() } returns
            Result.success(Pair(emptyList(), emptyList()))

        val result = useCase()

        assertTrue(result.isSuccess)
        val (questions, posts) = result.getOrNull()!!
        assertTrue(questions.isEmpty())
        assertTrue(posts.isEmpty())
    }

    @Test
    fun `returns failure when repository fails`() = runTest {
        val exception = RuntimeException("Server error")
        coEvery { repository.getAllQuestions() } returns Result.failure(exception)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `returns failure when offline without cache`() = runTest {
        val offlineError = IllegalStateException("Otázky nejsou dostupné v offline režimu")
        coEvery { repository.getAllQuestions() } returns Result.failure(offlineError)

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `delegates to repository without extra calls`() = runTest {
        coEvery { repository.getAllQuestions() } returns
            Result.success(Pair(emptyList(), emptyList()))

        useCase()

        coVerify(exactly = 1) { repository.getAllQuestions() }
    }

    @Test
    fun `question helper properties return correct values`() {
        val correctIndex = sampleQuestion.correctAnswer
        assertEquals(1, correctIndex) // "1945" is at index 1

        assertEquals("Ve kterém roce skončila druhá světová válka?", sampleQuestion.text)
        assertEquals(3, sampleQuestion.options.size)
        assertEquals("1945", sampleQuestion.options[correctIndex])
    }

    @Test
    fun `question isTextQuestion is false for multiple choice`() {
        assertTrue(!sampleQuestion.isTextQuestion)
    }

    @Test
    fun `question isTextQuestion is true for single correct answer`() {
        val textQuestion = Question(
            id = 20,
            questionText = "Napiš rok konce druhé světové války",
            postId = 1,
            answers = listOf(Answer(id = 5, answerText = "1945", correct = 1, questionId = 20))
        )
        assertTrue(textQuestion.isTextQuestion)
        assertEquals("1945", textQuestion.correctTextAnswer)
    }
}
