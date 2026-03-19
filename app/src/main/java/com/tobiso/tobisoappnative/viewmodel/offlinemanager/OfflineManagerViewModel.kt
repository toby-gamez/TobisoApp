package com.tobiso.tobisoappnative.viewmodel.offlinemanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineManagerViewModel @Inject constructor(
    application: Application,
    private val offlineRepo: OfflineRepositoryImpl
) : AndroidViewModel(application) {

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _offlineDownloading = MutableStateFlow(false)
    val offlineDownloading: StateFlow<Boolean> = _offlineDownloading

    private val _offlineProgress = MutableStateFlow(0f)
    val offlineDownloadProgress: StateFlow<Float> = _offlineProgress

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
        }
    }

    fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineDownloading.value = true
            val success = offlineRepo.downloadAllData { progress -> _offlineProgress.value = progress }
            _offlineDownloading.value = false
            _toastMessage.value = if (success) "Offline obsah byl aktualizován"
                else "Stažení selhalo. Zkontrolujte připojení."
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
