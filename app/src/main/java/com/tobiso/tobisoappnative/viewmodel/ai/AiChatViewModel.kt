package com.tobiso.tobisoappnative.viewmodel.ai
import timber.log.Timber

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.model.AiChatMessageDto
import com.tobiso.tobisoappnative.model.AiChatRequest
import com.tobiso.tobisoappnative.model.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AiChatViewModel(
    private val postId: Int,
    private val firstUserMessage: String
) : BaseViewModel<AiChatState, AiChatIntent, AiChatEffect>(AiChatState()) {

    init {
        if (firstUserMessage.isNotBlank()) {
            onIntent(AiChatIntent.SendMessage(firstUserMessage))
        }
    }

    override fun onIntent(intent: AiChatIntent) {
        when (intent) {
            is AiChatIntent.SendMessage -> sendMessage(intent.text)
        }
    }

    private fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || currentState.isLoading) return

        setState { copy(messages = messages + ChatMessage("user", trimmed), error = null, isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = currentState.messages.dropLast(1).map {
                    AiChatMessageDto(it.role, it.content)
                }
                val request = AiChatRequest(
                    postId = postId,
                    question = trimmed,
                    conversationHistory = history
                )
                val response = ApiClient.apiService.askAi("tobiso-android", request)
                setState {
                    copy(
                        messages = messages + ChatMessage("assistant", response.answer),
                        remainingQuestions = response.remainingQuestions,
                        isLoading = false
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    setState { copy(limitReached = true, error = "Dosáhl jsi denního limitu dotazů. Zkus to zítra.", isLoading = false) }
                } else {
                    setState { copy(error = "Chyba serveru (${e.code()}). Zkus to znovu.", isLoading = false) }
                }
                Timber.e(e, "HTTP error ${e.code()}")
            } catch (e: Exception) {
                setState { copy(error = "Nepodařilo se spojit se serverem.", isLoading = false) }
                Timber.e(e, "Error asking AI")
            }
        }
    }

    class Factory(
        private val postId: Int,
        private val firstUserMessage: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AiChatViewModel(postId, firstUserMessage) as T
    }
}
