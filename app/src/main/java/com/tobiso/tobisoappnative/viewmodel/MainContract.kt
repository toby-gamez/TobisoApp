package com.tobiso.tobisoappnative.viewmodel

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post

data class MainState(
    val categories: List<Category> = emptyList(),
    val posts: List<Post> = emptyList(),
    val categoryError: String? = null,
    val categoryLoading: Boolean = false,
    val isOffline: Boolean = false,
    val hasUserDismissedNoInternet: Boolean = false,
    val searchBarExpanded: Boolean = true
) : UiState

sealed interface MainIntent : UiIntent {
    object LoadCategories : MainIntent
    object EnableOfflineMode : MainIntent
    object ConfirmOfflineModeTransition : MainIntent
    object ResetNoInternetDismiss : MainIntent
    object RefreshNetworkState : MainIntent
    data class SetSearchBarExpanded(val expanded: Boolean) : MainIntent
}

sealed interface MainEffect : UiEffect {
    data class ShowToast(val message: String) : MainEffect
}
