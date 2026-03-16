package com.example.tobisoappnative.viewmodel.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.AiChatMessageDto
import com.example.tobisoappnative.model.AiChatRequest
import com.example.tobisoappnative.model.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ChatMessage(
    val role: String, // "user" nebo "assistant"
    val content: String
)

class AiChatViewModel(
    private val postId: Int,
    private val firstUserMessage: String
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _remainingQuestions = MutableStateFlow<Int?>(null)
    val remainingQuestions: StateFlow<Int?> = _remainingQuestions

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _limitReached = MutableStateFlow(false)
    val limitReached: StateFlow<Boolean> = _limitReached

    init {
        if (firstUserMessage.isNotBlank()) {
            sendMessage(firstUserMessage)
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        _messages.value = _messages.value + ChatMessage("user", trimmed)
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = _messages.value.dropLast(1).map {
                    AiChatMessageDto(it.role, it.content)
                }
                val request = AiChatRequest(
                    postId = postId,
                    question = trimmed,
                    conversationHistory = history
                )
                val response = ApiClient.apiService.askAi("tobiso-android", request)
                _messages.value = _messages.value + ChatMessage("assistant", response.answer)
                _remainingQuestions.value = response.remainingQuestions
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    _limitReached.value = true
                    _error.value = "Dosáhl jsi denního limitu dotazů. Zkus to zítra."
                } else {
                    _error.value = "Chyba serveru (${e.code()}). Zkus to znovu."
                }
                android.util.Log.e("AiChatViewModel", "HTTP error ${e.code()}", e)
            } catch (e: Exception) {
                _error.value = "Nepodařilo se spojit se serverem."
                android.util.Log.e("AiChatViewModel", "Error asking AI", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(
        private val postId: Int,
        private val firstUserMessage: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AiChatViewModel(postId, firstUserMessage) as T
    }
}
