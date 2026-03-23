package com.tobiso.tobisoappnative.viewmodel
import timber.log.Timber

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseAndroidViewModel
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lean ViewModel for cross-cutting state shared across the entire app:
 *  – Categories and posts list (navigation + FloatingSearchBar)
 *  – Network / offline state and NoInternet overlay control
 *  – Search bar expanded/collapsed state
 *
 * All per-screen concerns (post detail, questions, exercises, favourites, TTS,
 * offline download management, etc.) are handled by dedicated ViewModels.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val offlineDataManager: OfflineDataManager,
    private val offlineRepo: OfflineRepositoryImpl
) : BaseAndroidViewModel<MainState, MainIntent, MainEffect>(application, MainState()) {

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
            val isOnline = NetworkUtils.isOnline(getApplication())

            // If cache is fresh AND data actually exists in DB, load from cache and skip download.
            // Note: after a Room schema migration, the DB is wiped but SharedPreferences timestamp
            // remains – so we must verify data is actually present before skipping the download.
            try {
                if (offlineDataManager.isCacheFresh(OfflineDataManager.CACHE_FRESHNESS_MINUTES)) {
                    val cachedCategories = offlineDataManager.getCachedCategories()
                    if (cachedCategories != null) {
                        loadOfflineData(isOnline)
                        return@launch
                    }
                    // Timestamp is fresh but DB is empty (schema migration wiped it) → re-download
                    Timber.w("Cache timestamp fresh but DB empty – forcing re-download")
                }
            } catch (e: Exception) {
                Timber.w("Error checking cache freshness: ${e.message}")
            }

            if (isOnline) {
                try {
                    // Phase 1: Download categories + posts → update state immediately so the UI
                    // becomes interactive right away without any full-screen loading block.
                    val base = offlineRepo.downloadCategoriesAndPosts()
                    if (base != null) {
                        val (categories, posts) = base
                        setState { copy(categories = categories, posts = posts, categoryError = null, isOffline = false) }

                        // Phase 2: Download the rest (questions, exercises, etc.) silently in the
                        // background. Data from Phase 1 is passed directly — no DB re-read needed.
                        launch {
                            try {
                                offlineRepo.downloadRemainingData(categories, posts)
                            } catch (e: Exception) {
                                // downloadRemainingData never throws, but defensively catch anyway
                                Timber.e(e, "Phase 2 background failed: ${e.message}")
                            }
                            if (isFirstLoad) isFirstLoad = false
                        }
                    } else {
                        loadOfflineData(isOnline)
                    }
                } catch (e: Exception) {
                    val stillOnline = NetworkUtils.isOnline(getApplication())
                    if (stillOnline) {
                        setState { copy(categoryError = "Chyba serveru: ${e.message}", isOffline = false) }
                        emitEffect(MainEffect.ShowToast("Problém s připojením k serveru. Zkuste to později."))
                    } else {
                        loadOfflineData(isOnline = false)
                    }
                }
            } else {
                loadOfflineData(isOnline = false)
            }
        }
    }

    private suspend fun loadOfflineData(isOnline: Boolean = false) {
        val cachedCategories = offlineDataManager.getCachedCategories()
        val cachedPosts = offlineDataManager.getCachedPosts()
        if (cachedCategories != null && cachedPosts != null) {
            setState { copy(categories = cachedCategories, posts = cachedPosts, categoryError = null, isOffline = !isOnline) }
        } else {
            setState { copy(categories = emptyList(), posts = emptyList(), categoryError = "Žádná offline data k dispozici", isOffline = !isOnline) }
            if (!isOnline) {
                emitEffect(MainEffect.ShowToast("Žádná offline data. Připojte se k internetu."))
            }
        }
    }
}
