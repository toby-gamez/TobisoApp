package com.tobiso.tobisoappnative.viewmodel.ai

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.db.dao.AiChatDao
import com.tobiso.tobisoappnative.db.entity.AiChatMessageEntity
import com.tobiso.tobisoappnative.db.entity.AiChatSessionEntity
import com.tobiso.tobisoappnative.model.AiChatMessageDto
import com.tobiso.tobisoappnative.model.AiChatRequest
import com.tobiso.tobisoappnative.model.ApiClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

@HiltViewModel(assistedFactory = AiChatViewModel.Factory::class)
class AiChatViewModel @AssistedInject constructor(
    @Assisted val postId: Int,
    @Assisted("postTitle") val postTitle: String,
    @Assisted("firstUserMessage") val firstUserMessage: String,
    @Assisted val sessionId: Long,
    private val aiChatDao: AiChatDao
) : BaseViewModel<AiChatState, AiChatIntent, AiChatEffect>(AiChatState()) {

    @AssistedFactory
    interface Factory {
        fun create(
            postId: Int,
            @Assisted("postTitle") postTitle: String,
            @Assisted("firstUserMessage") firstUserMessage: String,
            sessionId: Long
        ): AiChatViewModel
    }

    private val resolvedSessionId = CompletableDeferred<Long>()

    init {
        if (sessionId >= 0L) {
            resolvedSessionId.complete(sessionId)
            viewModelScope.launch(Dispatchers.IO) {
                val msgs = aiChatDao.getMessages(sessionId).map { ChatMessage(it.role, it.content) }
                setState { copy(messages = msgs, currentSessionId = sessionId) }
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val sid = aiChatDao.insertSession(
                    AiChatSessionEntity(
                        postId = postId,
                        postTitle = postTitle,
                        startedAt = now,
                        lastMessageAt = now
                    )
                )
                resolvedSessionId.complete(sid)
                setState { copy(currentSessionId = sid) }
            }
            if (firstUserMessage.isNotBlank()) {
                onIntent(AiChatIntent.SendMessage(firstUserMessage))
            }
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

                val sid = resolvedSessionId.await()
                val now = System.currentTimeMillis()
                aiChatDao.insertMessage(AiChatMessageEntity(sessionId = sid, role = "user", content = trimmed, timestamp = now))
                aiChatDao.insertMessage(AiChatMessageEntity(sessionId = sid, role = "assistant", content = response.answer, timestamp = now + 1))
                aiChatDao.updateSession(sid, now, response.answer.take(80), currentState.messages.size)
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
}
