package com.tobiso.tobisoappnative.viewmodel.ai

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.AiCreditManager
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.db.dao.AiChatDao
import com.tobiso.tobisoappnative.db.entity.AiChatMessageEntity
import com.tobiso.tobisoappnative.db.entity.AiChatSessionEntity
import com.tobiso.tobisoappnative.model.AiChatMessageDto
import com.tobiso.tobisoappnative.model.AiChatRequest
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
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
    private val aiChatDao: AiChatDao,
    private val api: ApiService
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

    /**
     * Signed-in students: the server saves the chat to their account (shared with the website), so
     * nothing is written to the device. [sessionId] is then a server session id. Otherwise the
     * chat lives only in the local database.
     */
    private val remote = AuthSession.isSignedIn

    init {
        if (remote) {
            if (sessionId >= 0L) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val msgs = api.getServerChatMessages(sessionId.toInt()).map { ChatMessage(it.role, it.content) }
                        setState { copy(messages = msgs, currentSessionId = sessionId) }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load server chat $sessionId")
                        setState { copy(error = "Konverzaci se nepodařilo načíst.") }
                    }
                }
            }
            if (sessionId < 0L && firstUserMessage.isNotBlank()) {
                onIntent(AiChatIntent.SendMessage(firstUserMessage))
            }
        } else if (sessionId >= 0L) {
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

        // When the user purchases bonus credits, re-enable the chat input
        viewModelScope.launch {
            AiCreditManager.instance.bonusRemaining.collect { bonus ->
                if (bonus > 0 && currentState.limitReached) {
                    setState { copy(limitReached = false, error = null) }
                }
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
                val deviceId = AiCreditManager.instance.deviceId
                val httpResponse = api.askAiStream("tobiso-android", deviceId, request)
                if (!httpResponse.isSuccessful) throw HttpException(httpResponse)
                httpResponse.headers()["X-Remaining-Questions"]?.toIntOrNull()?.let { remaining ->
                    AiCreditManager.instance.updateBonusRemaining(remaining)
                    setState { copy(remainingQuestions = remaining) }
                }

                // The answer streams in; show it growing in a placeholder bubble.
                val bubble = ChatMessage("assistant", "")
                val answer = StringBuilder()
                setState { copy(messages = messages + bubble) }
                httpResponse.body()!!.use { body ->
                    val reader = body.charStream().buffered()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").removePrefix(" ")
                        if (payload == "[DONE]") break
                        answer.append(payload.replace("\\n", "\n"))
                        val text = answer.toString()
                        setState { copy(messages = messages.map { if (it.id == bubble.id) it.copy(content = text) else it }) }
                    }
                }
                val finalAnswer = answer.toString()
                if (finalAnswer.isBlank()) throw java.io.IOException("Empty AI answer")
                setState { copy(isLoading = false) }
                val response = com.tobiso.tobisoappnative.model.AiChatResponse(finalAnswer, currentState.remainingQuestions ?: 0)

                if (remote) return@launch
                val sid = resolvedSessionId.await()
                val now = System.currentTimeMillis()
                aiChatDao.insertMessage(AiChatMessageEntity(sessionId = sid, role = "user", content = trimmed, timestamp = now))
                aiChatDao.insertMessage(AiChatMessageEntity(sessionId = sid, role = "assistant", content = response.answer, timestamp = now + 1))
                aiChatDao.updateSession(sid, now, response.answer.take(80), currentState.messages.size)
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    setState { copy(limitReached = true, error = "Dosáhl jsi denního limitu dotazů. Zkus to zítra nebo si dokup otázky v obchodě.", isLoading = false) }
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
