package com.example.tobisoappnative.viewmodel.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.repository.OfflineRepositoryImpl
import com.example.tobisoappnative.repository.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val postsRepo: PostsRepository,
    private val offlineRepo: OfflineRepositoryImpl
) : BaseAndroidViewModel<HomeState, HomeIntent, HomeEffect>(application, HomeState()) {

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
}
