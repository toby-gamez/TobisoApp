package com.tobiso.tobisoappnative.viewmodel.flashcards

import com.tobiso.tobisoappnative.model.Answer
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.PracticeAttemptRequest
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.screens.flashcardAnswerText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FlashcardDeckViewModelTest {

    private lateinit var api: ApiService
    private lateinit var vm: FlashcardDeckViewModel

    private fun card(id: Int) = Question(id, "Otázka $id", 1, listOf(Answer(id * 10, "Odpověď $id", 1, id)))

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        api = mockk()
        coEvery { api.recordPracticeAttempt(any()) } returns Response.success(Unit)
        vm = FlashcardDeckViewModel(api)
    }

    @After
    fun tearDown() {
        AuthSession.signOut()
        Dispatchers.setMain(Dispatchers.Default)
    }

    private fun loaded(vararg ids: Int, categoryId: Int? = null): FlashcardDeckViewModel {
        coEvery { api.getFlashcardDeck(categoryId, 20) } returns ids.map(::card)
        vm.onIntent(FlashcardDeckIntent.Load(categoryId))
        return vm
    }

    @Test
    fun `load fills the deck`() {
        loaded(1, 2, 3, categoryId = 7)

        val s = vm.uiState.value
        assertEquals(3, s.cards.size)
        assertEquals(7, s.categoryId)
        assertFalse(s.isLoading)
        assertNull(s.error)
        assertEquals(1, s.current?.id)
    }

    @Test
    fun `load failure reports an error and no cards`() {
        coEvery { api.getFlashcardDeck(any(), any()) } throws RuntimeException("offline")

        vm.onIntent(FlashcardDeckIntent.Load(null))

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.cards.isEmpty())
    }

    @Test
    fun `flip toggles and answering resets it and advances`() {
        loaded(1, 2)

        vm.onIntent(FlashcardDeckIntent.Flip)
        assertTrue(vm.uiState.value.flipped)

        vm.onIntent(FlashcardDeckIntent.Answer(knew = true))

        val s = vm.uiState.value
        assertFalse(s.flipped)
        assertEquals(1, s.index)
        assertEquals(setOf(1), s.known)
        assertEquals(2, s.current?.id)
    }

    @Test
    fun `deck is finished after the last answer and splits known and unknown`() {
        loaded(1, 2, 3)

        vm.onIntent(FlashcardDeckIntent.Answer(true))
        vm.onIntent(FlashcardDeckIntent.Answer(false))
        vm.onIntent(FlashcardDeckIntent.Answer(false))

        val s = vm.uiState.value
        assertTrue(s.finished)
        assertNull(s.current)
        assertEquals(setOf(1), s.known)
        assertEquals(setOf(2, 3), s.unknown)
        assertEquals(1f, s.progress, 0.0001f)
    }

    @Test
    fun `answering with no card does nothing`() {
        vm.onIntent(FlashcardDeckIntent.Answer(true))

        assertEquals(0, vm.uiState.value.index)
        assertTrue(vm.uiState.value.known.isEmpty())
    }

    @Test
    fun `repeat unknown starts a round with only the missed cards`() {
        loaded(1, 2, 3)
        vm.onIntent(FlashcardDeckIntent.Answer(true))
        vm.onIntent(FlashcardDeckIntent.Answer(false))
        vm.onIntent(FlashcardDeckIntent.Answer(false))

        vm.onIntent(FlashcardDeckIntent.RepeatUnknown)

        val s = vm.uiState.value
        assertEquals(listOf(2, 3), s.cards.map { it.id })
        assertEquals(0, s.index)
        assertTrue(s.known.isEmpty() && s.unknown.isEmpty())
        assertFalse(s.finished)
    }

    @Test
    fun `repeat unknown is a no op when everything was known`() {
        loaded(1)
        vm.onIntent(FlashcardDeckIntent.Answer(true))

        vm.onIntent(FlashcardDeckIntent.RepeatUnknown)

        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun `signed in answers are recorded on the server`() {
        AuthSession.signIn("t", AuthUser(displayName = "Ája"))
        loaded(5, 6)

        vm.onIntent(FlashcardDeckIntent.Answer(true))
        vm.onIntent(FlashcardDeckIntent.Answer(false))

        coVerify(exactly = 1) { api.recordPracticeAttempt(PracticeAttemptRequest(5, true)) }
        coVerify(exactly = 1) { api.recordPracticeAttempt(PracticeAttemptRequest(6, false)) }
    }

    @Test
    fun `anonymous answers are not sent anywhere`() {
        loaded(5)

        vm.onIntent(FlashcardDeckIntent.Answer(true))

        coVerify(exactly = 0) { api.recordPracticeAttempt(any()) }
    }

    @Test
    fun `a failing attempt upload does not break the deck`() {
        AuthSession.signIn("t", AuthUser(displayName = "Ája"))
        coEvery { api.recordPracticeAttempt(any()) } throws RuntimeException("offline")
        loaded(5, 6)

        vm.onIntent(FlashcardDeckIntent.Answer(true))

        assertEquals(1, vm.uiState.value.index)
        assertEquals(6, vm.uiState.value.current?.id)
    }

    @Test
    fun `text answers show the answer text and choice questions list the correct options`() {
        assertEquals("Odpověď 1", flashcardAnswerText(card(1)))
        val choice = Question(2, "Která?", 1, listOf(Answer(1, "A", 1, 2), Answer(2, "B", 0, 2), Answer(3, "C", 1, 2)))
        assertEquals("• A\n• C", flashcardAnswerText(choice))
    }
}
