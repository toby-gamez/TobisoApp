package com.example.tobisoappnative.viewmodel.questions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.repository.PostDetailRepositoryImpl
import com.example.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuestionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PostDetailRepositoryImpl(application, OfflineDataManager(application))

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _questionsError = MutableStateFlow<String?>(null)
    val questionsError: StateFlow<String?> = _questionsError

    private val _questionsLoading = MutableStateFlow(false)
    val questionsLoading: StateFlow<Boolean> = _questionsLoading

    private val _postDetail = MutableStateFlow<Post?>(null)
    val postDetail: StateFlow<Post?> = _postDetail

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    fun loadPostDetail(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
            repo.getPostDetail(postId).fold(
                onSuccess = { _postDetail.value = it },
                onFailure = { /* non-fatal, questions still load */ }
            )
        }
    }

    fun loadQuestions(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _questionsLoading.value = true
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
            repo.getQuestionsForPost(postId).fold(
                onSuccess = { list ->
                    _questions.value = list
                    _questionsError.value = if (list.isEmpty() && _isOffline.value)
                        "Otázky pro tento článek nejsou dostupné v offline režimu" else null
                },
                onFailure = { e -> _questionsError.value = e.message }
            )
            _questionsLoading.value = false
        }
    }

    fun clearQuestions() {
        _questions.value = emptyList()
        _questionsError.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            QuestionsViewModel(application) as T
    }
}
