package com.example.tobisoappnative.viewmodel.plaintext

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Snippet
import com.example.tobisoappnative.repository.FavoritesRepositoryImpl
import com.example.tobisoappnative.repository.PostDetailRepository
import com.example.tobisoappnative.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlainTextViewModel @Inject constructor(
    application: Application,
    private val repo: PostDetailRepository,
    private val favoritesRepo: FavoritesRepositoryImpl
) : AndroidViewModel(application) {

    private val ttsManager = TtsManager(application)

    private val _postDetail = MutableStateFlow<Post?>(null)
    val postDetail: StateFlow<Post?> = _postDetail

    private val _postDetailError = MutableStateFlow<String?>(null)
    val postDetailError: StateFlow<String?> = _postDetailError

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _lastHandledClipboard = MutableStateFlow<String?>(null)
    val lastHandledClipboard: StateFlow<String?> = _lastHandledClipboard

    fun loadPostDetail(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getPostDetail(postId).fold(
                onSuccess = { post ->
                    _postDetail.value = post
                    _postDetailError.value = null
                    _isOffline.value = false
                },
                onFailure = { e ->
                    _postDetailError.value = e.message
                    _isOffline.value = e is IllegalStateException
                }
            )
        }
    }

    fun getTtsManager(): TtsManager = ttsManager

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun addSnippet(snippet: Snippet) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.addSnippet(snippet) }
    }

    fun markClipboardHandled(text: String) {
        _lastHandledClipboard.value = text
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
