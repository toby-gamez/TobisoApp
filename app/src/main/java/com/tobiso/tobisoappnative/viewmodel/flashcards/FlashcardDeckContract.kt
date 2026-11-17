package com.tobiso.tobisoappnative.viewmodel.flashcards

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.Question

data class FlashcardDeckState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val categoryId: Int? = null,
    val cards: List<Question> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    /** Ids of cards answered "Umím" / "Musím zopakovat" in the current round. */
    val known: Set<Int> = emptySet(),
    val unknown: Set<Int> = emptySet()
) : UiState {
    val current: Question? get() = cards.getOrNull(index)
    val finished: Boolean get() = cards.isNotEmpty() && index >= cards.size
    val progress: Float get() = if (cards.isEmpty()) 0f else index.coerceAtMost(cards.size).toFloat() / cards.size
}

sealed interface FlashcardDeckIntent : UiIntent {
    /** Loads a fresh deck (the whole category subtree when [categoryId] is set). */
    data class Load(val categoryId: Int?) : FlashcardDeckIntent
    object Flip : FlashcardDeckIntent
    data class Answer(val knew: Boolean) : FlashcardDeckIntent
    /** New round with only the cards that were not known. */
    object RepeatUnknown : FlashcardDeckIntent
}

sealed interface FlashcardDeckEffect : UiEffect
