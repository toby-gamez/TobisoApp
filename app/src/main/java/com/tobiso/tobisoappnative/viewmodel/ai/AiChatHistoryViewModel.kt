package com.tobiso.tobisoappnative.viewmodel.ai

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.db.dao.AiChatDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AiChatHistoryViewModel @Inject constructor(
    private val aiChatDao: AiChatDao
) : BaseViewModel<AiChatHistoryState, AiChatHistoryIntent, AiChatHistoryEffect>(AiChatHistoryState()) {

    init {
        aiChatDao.getSessions()
            .onEach { sessions -> setState { copy(sessions = sessions) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: AiChatHistoryIntent) {
        when (intent) {
            is AiChatHistoryIntent.DeleteSession -> deleteSession(intent.id)
        }
    }

    private fun deleteSession(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                aiChatDao.deleteSession(id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete AI chat session $id")
            }
        }
    }
}
