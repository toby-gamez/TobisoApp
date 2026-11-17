package com.tobiso.tobisoappnative.viewmodel.flashcards

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.PracticeAttemptRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val DECK_SIZE = 20

@HiltViewModel
class FlashcardDeckViewModel @Inject constructor(
    private val api: ApiService
) : BaseViewModel<FlashcardDeckState, FlashcardDeckIntent, FlashcardDeckEffect>(FlashcardDeckState()) {

    override fun onIntent(intent: FlashcardDeckIntent) {
        when (intent) {
            is FlashcardDeckIntent.Load -> load(intent.categoryId)
            FlashcardDeckIntent.Flip -> setState { copy(flipped = !flipped) }
            is FlashcardDeckIntent.Answer -> answer(intent.knew)
            FlashcardDeckIntent.RepeatUnknown -> repeatUnknown()
        }
    }

    private fun load(categoryId: Int?) {
        viewModelScope.launch {
            setState { FlashcardDeckState(isLoading = true, categoryId = categoryId) }
            try {
                val cards = api.getFlashcardDeck(categoryId, DECK_SIZE)
                setState { copy(isLoading = false, cards = cards) }
            } catch (e: Exception) {
                Timber.e(e, "flashcard deck")
                setState { copy(isLoading = false, error = "Kartičky se nepodařilo načíst. Zkontroluj připojení.") }
            }
        }
    }

    private fun answer(knew: Boolean) {
        val card = currentState.current ?: return
        setState {
            copy(
                flipped = false,
                index = index + 1,
                known = if (knew) known + card.id else known - card.id,
                unknown = if (knew) unknown - card.id else unknown + card.id
            )
        }
        // Signed-in students feed the account-wide "recommended" practice; failures are not worth interrupting the deck.
        if (AuthSession.isSignedIn) {
            viewModelScope.launch {
                runCatching { api.recordPracticeAttempt(PracticeAttemptRequest(card.id, knew)) }
                    .onFailure { Timber.w(it, "record flashcard attempt") }
            }
        }
    }

    private fun repeatUnknown() {
        val again = currentState.cards.filter { it.id in currentState.unknown }
        if (again.isEmpty()) return
        setState { copy(cards = again, index = 0, flipped = false, known = emptySet(), unknown = emptySet()) }
    }
}
