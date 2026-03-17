package com.example.tobisoappnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Lean ViewModel for cross-cutting state shared across the entire app:
 *  – Categories and posts list (navigation + FloatingSearchBar)
 *  – Network / offline state and NoInternet overlay control
 *  – Search bar expanded/collapsed state
 *  – Global toast messages
 *
 * All per-screen concerns (post detail, questions, exercises, favourites, TTS,
 * offline download management, etc.) are handled by dedicated ViewModels.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ── Categories & Posts ────────────────────────────────────────────────────

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _categoryError = MutableStateFlow<String?>(null)
    val categoryError: StateFlow<String?> = _categoryError

    private val _categoryLoading = MutableStateFlow(false)
    val categoryLoading: StateFlow<Boolean> = _categoryLoading

    // ── Network / Offline state ───────────────────────────────────────────────

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    /** True once the user has dismissed the NoInternet screen and confirmed offline mode. */
    private val _hasUserDismissedNoInternet = MutableStateFlow(false)
    val hasUserDismissedNoInternet: StateFlow<Boolean> = _hasUserDismissedNoInternet

    // ── Search bar ────────────────────────────────────────────────────────────

    private val _searchBarExpanded = MutableStateFlow(true)
    val searchBarExpanded: StateFlow<Boolean> = _searchBarExpanded

    fun setSearchBarExpanded(expanded: Boolean) {
        _searchBarExpanded.value = expanded
    }

    // ── Toast ─────────────────────────────────────────────────────────────────

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private val offlineDataManager = OfflineDataManager(application)
    private var isFirstLoad = true

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            _categoryLoading.value = true
            val isOnline = NetworkUtils.isOnline(getApplication())

            try {
                if (offlineDataManager.isCacheFresh(15)) {
                    loadOfflineData()
                    _categoryLoading.value = false
                    return@launch
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Error checking cache freshness: ${e.message}")
            }

            if (isOnline) {
                try {
                    val categories = ApiClient.apiService.getCategories().toList()
                    val posts = ApiClient.apiService.getPosts().toList()
                    _categories.value = categories
                    _posts.value = posts
                    _categoryError.value = null
                    _isOffline.value = false
                    if (isFirstLoad) {
                        showToast("Offline obsah byl aktualizován")
                        isFirstLoad = false
                    }
                } catch (e: Exception) {
                    val stillOnline = NetworkUtils.isOnline(getApplication())
                    if (stillOnline) {
                        _categoryError.value = "Chyba serveru: ${e.message}"
                        _isOffline.value = false
                        showToast("Problém s připojením k serveru. Zkuste to později.")
                    } else {
                        loadOfflineData()
                    }
                }
            } else {
                loadOfflineData()
            }
            _categoryLoading.value = false
        }
    }

    fun enableOfflineMode() {
        viewModelScope.launch(Dispatchers.IO) { loadOfflineData() }
    }

    fun confirmOfflineModeTransition() {
        _hasUserDismissedNoInternet.value = true
    }

    fun resetNoInternetDismiss() {
        _hasUserDismissedNoInternet.value = false
    }

    fun refreshNetworkState() {
        viewModelScope.launch(Dispatchers.IO) {
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun loadOfflineData() {
        val cachedCategories = offlineDataManager.getCachedCategories()
        val cachedPosts = offlineDataManager.getCachedPosts()
        if (cachedCategories != null && cachedPosts != null) {
            _categories.value = cachedCategories
            _posts.value = cachedPosts
            _categoryError.value = null
            _isOffline.value = true
        } else {
            _categories.value = emptyList()
            _posts.value = emptyList()
            _categoryError.value = "Žádná offline data k dispozici"
            _isOffline.value = true
            showToast("Žádná offline data. Připojte se k internetu.")
        }
    }
}
