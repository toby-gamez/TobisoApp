package com.example.tobisoappnative.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.base.BaseAndroidViewModel
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Lean ViewModel for cross-cutting state shared across the entire app:
 *  – Categories and posts list (navigation + FloatingSearchBar)
 *  – Network / offline state and NoInternet overlay control
 *  – Search bar expanded/collapsed state
 *
 * All per-screen concerns (post detail, questions, exercises, favourites, TTS,
 * offline download management, etc.) are handled by dedicated ViewModels.
 */
class MainViewModel(application: Application) :
    BaseAndroidViewModel<MainState, MainIntent, MainEffect>(application, MainState()) {

    private val offlineDataManager = OfflineDataManager(application)
    private var isFirstLoad = true

    init {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isOffline = !NetworkUtils.isOnline(getApplication())) }
        }
    }

    override fun onIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.LoadCategories -> loadCategories()
            MainIntent.EnableOfflineMode -> viewModelScope.launch(Dispatchers.IO) { loadOfflineData() }
            MainIntent.ConfirmOfflineModeTransition -> setState { copy(hasUserDismissedNoInternet = true) }
            MainIntent.ResetNoInternetDismiss -> setState { copy(hasUserDismissedNoInternet = false) }
            MainIntent.RefreshNetworkState -> viewModelScope.launch(Dispatchers.IO) {
                setState { copy(isOffline = !NetworkUtils.isOnline(getApplication())) }
            }
            is MainIntent.SetSearchBarExpanded -> setState { copy(searchBarExpanded = intent.expanded) }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(categoryLoading = true) }
            val isOnline = NetworkUtils.isOnline(getApplication())

            try {
                if (offlineDataManager.isCacheFresh(15)) {
                    loadOfflineData()
                    setState { copy(categoryLoading = false) }
                    return@launch
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Error checking cache freshness: ${e.message}")
            }

            if (isOnline) {
                try {
                    val categories = ApiClient.apiService.getCategories().toList()
                    val posts = ApiClient.apiService.getPosts().toList()
                    offlineDataManager.saveCategoriesAndPosts(categories, posts)
                    setState { copy(categories = categories, posts = posts, categoryError = null, isOffline = false) }
                    if (isFirstLoad) {
                        emitEffect(MainEffect.ShowToast("Offline obsah byl aktualizován"))
                        isFirstLoad = false
                    }
                } catch (e: Exception) {
                    val stillOnline = NetworkUtils.isOnline(getApplication())
                    if (stillOnline) {
                        setState { copy(categoryError = "Chyba serveru: ${e.message}", isOffline = false) }
                        emitEffect(MainEffect.ShowToast("Problém s připojením k serveru. Zkuste to později."))
                    } else {
                        loadOfflineData()
                    }
                }
            } else {
                loadOfflineData()
            }
            setState { copy(categoryLoading = false) }
        }
    }

    private suspend fun loadOfflineData() {
        val cachedCategories = offlineDataManager.getCachedCategories()
        val cachedPosts = offlineDataManager.getCachedPosts()
        if (cachedCategories != null && cachedPosts != null) {
            setState { copy(categories = cachedCategories, posts = cachedPosts, categoryError = null, isOffline = true) }
        } else {
            setState { copy(categories = emptyList(), posts = emptyList(), categoryError = "Žádná offline data k dispozici", isOffline = true) }
            emitEffect(MainEffect.ShowToast("Žádná offline data. Připojte se k internetu."))
        }
    }
}
