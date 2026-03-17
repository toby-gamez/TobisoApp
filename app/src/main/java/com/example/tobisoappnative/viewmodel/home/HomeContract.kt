package com.example.tobisoappnative.viewmodel.home

import com.example.tobisoappnative.base.UiEffect
import com.example.tobisoappnative.base.UiIntent
import com.example.tobisoappnative.base.UiState
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post

data class HomeState(
    val categories: List<Category> = emptyList(),
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val offlineDownloading: Boolean = false,
    val offlineDownloadProgress: Float = 0f
) : UiState

sealed interface HomeIntent : UiIntent {
    object Load : HomeIntent
    object DownloadAllOfflineData : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class ShowToast(val message: String) : HomeEffect
}
