package com.tobiso.tobisoappnative.viewmodel.ai

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.db.dao.AiChatDao
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.parseServerInstant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Signed-in students see the history stored on the server (shared with the website); everyone
 * else sees the chats kept on this device.
 */
@HiltViewModel
class AiChatHistoryViewModel @Inject constructor(
    private val aiChatDao: AiChatDao,
    private val api: ApiService
) : BaseViewModel<AiChatHistoryState, AiChatHistoryIntent, AiChatHistoryEffect>(AiChatHistoryState()) {

    private var localJob: Job? = null

    init {
        load()
    }

    override fun onIntent(intent: AiChatHistoryIntent) {
        when (intent) {
            is AiChatHistoryIntent.DeleteSession -> deleteSession(intent.id)
            AiChatHistoryIntent.Reload -> load()
        }
    }

    private fun load() {
        localJob?.cancel()
        if (AuthSession.isSignedIn) loadRemote() else loadLocal()
    }

    private fun loadLocal() {
        setState { copy(remote = false, isLoading = false, error = null) }
        localJob = aiChatDao.getSessions()
            .onEach { sessions ->
                setState {
                    copy(sessions = sessions.map {
                        ChatSessionItem(it.id, it.postId, it.postTitle, it.lastMessagePreview, it.lastMessageAt, it.messageCount)
                    })
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadRemote() {
        setState { copy(remote = true, isLoading = true, error = null, sessions = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = api.getServerChatHistory().map {
                    ChatSessionItem(
                        id = it.id.toLong(),
                        postId = it.postId ?: 0,
                        postTitle = it.postTitle ?: it.title ?: "AI konverzace",
                        lastMessagePreview = "",
                        lastMessageAt = parseServerInstant(it.updatedAt ?: it.createdAt),
                        messageCount = 0
                    )
                }
                setState { copy(sessions = items, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load server AI chat history")
                setState { copy(isLoading = false, error = "Historii se nepodařilo načíst.") }
            }
        }
    }

    private fun deleteSession(id: Long) {
        if (currentState.remote) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                aiChatDao.deleteSession(id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete AI chat session $id")
            }
        }
    }
}
