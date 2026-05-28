package com.tobiso.tobisoappnative.viewmodel.questions

import android.app.Application
import com.tobiso.tobisoappnative.model.Answer
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.repository.PostsRepository
import com.tobiso.tobisoappnative.utils.NetworkUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionsViewModelTest {

    private lateinit var repo: PostsRepository
    private lateinit var app: Application
    private lateinit var viewModel: QuestionsViewModel

    private val samplePost = Post(id = 5, title = "Slovesa", filePath = "slovesa.md", categoryId = 2)
    private val sampleQuestion = Question(
        id = 1,
        questionText = "Co je to sloveso?",
        postId = 5,
        answers = listOf(
            Answer(id = 1, answerText = "Část řeči vyjadřující děj", correct = 1, questionId = 1),
            Answer(id = 2, answerText = "Jméno věci", correct = 0, questionId = 1),
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        repo = mockk()
        app = mockk(relaxed = true)
        mockkObject(NetworkUtils)
        every { NetworkUtils.isOnline(any()) } returns true
        viewModel = QuestionsViewModel(repo, app)
    }

    @After
    fun tearDown() {
        unmockkObject(NetworkUtils)
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun `loadQuestions populates questions on success`() = runTest {
        coEvery { repo.getQuestionsForPost(5) } returns Result.success(listOf(sampleQuestion))

        viewModel.loadQuestions(5)
        delay(200)

        assertEquals(1, viewModel.questions.value.size)
        assertEquals("Co je to sloveso?", viewModel.questions.value[0].questionText)
        assertNull(viewModel.questionsError.value)
        assertFalse(viewModel.questionsLoading.value)
    }

    @Test
    fun `loadQuestions sets error on failure`() = runTest {
        coEvery { repo.getQuestionsForPost(5) } returns Result.failure(RuntimeException("Chyba sítě"))

        viewModel.loadQuestions(5)
        delay(200)

        assertTrue(viewModel.questions.value.isEmpty())
        assertEquals("Chyba sítě", viewModel.questionsError.value)
        assertFalse(viewModel.questionsLoading.value)
    }

    @Test
    fun `loadQuestions sets offline error message when empty and offline`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        coEvery { repo.getQuestionsForPost(5) } returns Result.success(emptyList())

        viewModel.loadQuestions(5)
        delay(200)

        assertTrue(viewModel.questions.value.isEmpty())
        assertTrue(viewModel.questionsError.value!!.contains("offline"))
        assertTrue(viewModel.isOffline.value)
    }

    @Test
    fun `loadQuestions with non-empty result does not set offline error even when offline`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        coEvery { repo.getQuestionsForPost(5) } returns Result.success(listOf(sampleQuestion))

        viewModel.loadQuestions(5)
        delay(200)

        assertEquals(1, viewModel.questions.value.size)
        assertNull(viewModel.questionsError.value)
    }

    @Test
    fun `loadPostDetail populates postDetail on success`() = runTest {
        coEvery { repo.getPost(5) } returns Result.success(samplePost)

        viewModel.loadPostDetail(5)
        delay(200)

        assertEquals(samplePost, viewModel.postDetail.value)
    }

    @Test
    fun `loadPostDetail tolerates failure without crashing`() = runTest {
        coEvery { repo.getPost(5) } returns Result.failure(RuntimeException("Not found"))

        viewModel.loadPostDetail(5)
        delay(200)

        assertNull(viewModel.postDetail.value)
    }

    @Test
    fun `clearQuestions resets questions and error state`() = runTest {
        coEvery { repo.getQuestionsForPost(5) } returns Result.success(listOf(sampleQuestion))
        viewModel.loadQuestions(5)
        delay(200)

        viewModel.clearQuestions()

        assertTrue(viewModel.questions.value.isEmpty())
        assertNull(viewModel.questionsError.value)
    }

    @Test
    fun `isOffline reflects online status during load`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        coEvery { repo.getQuestionsForPost(5) } returns Result.success(listOf(sampleQuestion))

        viewModel.loadQuestions(5)
        delay(200)

        assertTrue(viewModel.isOffline.value)
    }
}
