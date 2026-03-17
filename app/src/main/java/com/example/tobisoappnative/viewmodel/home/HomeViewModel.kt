package com.example.tobisoappnative.viewmodel.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.repository.OfflineRepositoryImpl
import com.example.tobisoappnative.repository.PostsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) :
    BaseAndroidViewModel<HomeState, HomeIntent, HomeEffect>(application, HomeState()) {

    private val postsRepo = PostsRepositoryImpl(application, OfflineDataManager(application))
    private val offlineRepo = OfflineRepositoryImpl(application, OfflineDataManager(application))

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load()
            HomeIntent.DownloadAllOfflineData -> downloadAllOfflineData()
        }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true) }
            postsRepo.getCategories().fold(
                onSuccess = { cats -> setState { copy(categories = cats, isOffline = false) } },
                onFailure = { e -> setState { copy(isOffline = true, error = e.message) } }
            )
            postsRepo.getPostsByCategory(null).fold(
                onSuccess = { posts -> setState { copy(posts = posts) } },
                onFailure = { /* categories error already set */ }
            )
            setState { copy(isLoading = false) }
        }
    }

    private fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(offlineDownloading = true) }
            val success = offlineRepo.downloadAllData { progress ->
                setState { copy(offlineDownloadProgress = progress) }
            }
            setState { copy(offlineDownloading = false) }
            if (success) {
                emitEffect(HomeEffect.ShowToast("Offline obsah byl aktualizován"))
                load()
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(application) as T
    }
}
