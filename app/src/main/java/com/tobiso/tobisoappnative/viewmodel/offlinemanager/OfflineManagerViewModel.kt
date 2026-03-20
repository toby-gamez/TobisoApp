package com.tobiso.tobisoappnative.viewmodel.offlinemanager
import timber.log.Timber

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CacheInfo(
    val categoriesCount: Int? = null,
    val postsCount: Int? = null,
    val questionsCount: Int? = null,
    val questionsPostsCount: Int? = null,
    val relatedPostsCount: Int? = null,
    val addendumsCount: Int? = null,
    val exercisesCount: Int? = null,
    val eventsCount: Int? = null,
    val lastUpdateFormatted: String? = null,
    val lastUpdateTimestamp: Long? = null,
    val cacheFresh15: Boolean? = null
)

@HiltViewModel
class OfflineManagerViewModel @Inject constructor(
    application: Application,
    private val offlineRepo: OfflineRepositoryImpl,
    private val offlineDataManager: OfflineDataManager
) : AndroidViewModel(application) {

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _offlineDownloading = MutableStateFlow(false)
    val offlineDownloading: StateFlow<Boolean> = _offlineDownloading

    private val _offlineProgress = MutableStateFlow(0f)
    val offlineDownloadProgress: StateFlow<Float> = _offlineProgress

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val _cacheInfo = MutableStateFlow(CacheInfo())
    val cacheInfo: StateFlow<CacheInfo> = _cacheInfo

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
        }
    }

    fun loadCacheInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _cacheInfo.value = CacheInfo(
                    categoriesCount = offlineDataManager.getCachedCategories()?.size,
                    postsCount = offlineDataManager.getCachedPosts()?.size,
                    questionsCount = offlineDataManager.getCachedQuestions()?.size,
                    questionsPostsCount = offlineDataManager.getCachedQuestionsPosts()?.size,
                    relatedPostsCount = offlineDataManager.getCachedRelatedPosts()?.size,
                    addendumsCount = offlineDataManager.getCachedAddendums()?.size,
                    exercisesCount = offlineDataManager.getCachedExercises()?.size,
                    eventsCount = offlineDataManager.getCachedEvents()?.size,
                    lastUpdateFormatted = offlineDataManager.getLastUpdateFormatted(),
                    lastUpdateTimestamp = offlineDataManager.getLastUpdateTimestamp(),
                    cacheFresh15 = offlineDataManager.isCacheFresh(15)
                )
            } catch (e: Exception) {
                _cacheInfo.value = CacheInfo()
            }
        }
    }

    fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineDownloading.value = true
            _offlineProgress.value = 0f
            try {
                val success = offlineRepo.downloadAllData { progress -> _offlineProgress.value = progress }
                _offlineDownloading.value = false
                _toastMessage.value = if (success) "Offline obsah byl aktualizován"
                    else "Stažení selhalo. Zkontrolujte připojení."
                if (success) loadCacheInfo()
            } catch (e: Exception) {
                Timber.e(e, "downloadAllOfflineData failed")
                _offlineDownloading.value = false
                _toastMessage.value = "Chyba stahování: ${e.message}"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
