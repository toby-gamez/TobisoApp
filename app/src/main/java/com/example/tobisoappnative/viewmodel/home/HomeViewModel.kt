package com.example.tobisoappnative.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.repository.OfflineRepositoryImpl
import com.example.tobisoappnative.repository.PostsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val postsRepo = PostsRepositoryImpl(application, OfflineDataManager(application))
    private val offlineRepo = OfflineRepositoryImpl(application, OfflineDataManager(application))

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _offlineDownloading = MutableStateFlow(false)
    val offlineDownloading: StateFlow<Boolean> = _offlineDownloading

    private val _offlineProgress = MutableStateFlow(0f)
    val offlineDownloadProgress: StateFlow<Float> = _offlineProgress

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            postsRepo.getCategories().fold(
                onSuccess = { cats ->
                    _categories.value = cats
                    _isOffline.value = false
                },
                onFailure = { e ->
                    _isOffline.value = true
                    _error.value = e.message
                }
            )
            postsRepo.getPostsByCategory(null).fold(
                onSuccess = { posts -> _posts.value = posts },
                onFailure = { /* categories error already set */ }
            )
            _isLoading.value = false
        }
    }

    fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineDownloading.value = true
            val success = offlineRepo.downloadAllData { progress ->
                _offlineProgress.value = progress
            }
            _offlineDownloading.value = false
            if (success) {
                _toastMessage.value = "Offline obsah byl aktualizován"
                load()
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(application) as T
    }
}
