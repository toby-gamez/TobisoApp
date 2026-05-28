package com.tobiso.tobisoappnative.viewmodel.feedback
import timber.log.Timber

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tobiso.tobisoappnative.FeedbackSyncWorker
import com.tobiso.tobisoappnative.db.AppDatabase
import com.tobiso.tobisoappnative.db.entity.FeedbackEntity
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.FeedbackDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    fun onNameChange(value: String) {
        _name.value = value
        resetStatus()
    }

    fun onEmailChange(value: String) {
        _email.value = value.trim()
        resetStatus()
    }

    fun onMessageChange(value: String) {
        _message.value = value
        resetStatus()
    }

    private fun resetStatus() {
        _isSuccess.value = false
        _isError.value = false
    }

    fun sendFeedback() {
        resetStatus()
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dto = FeedbackDto(
                    name = _name.value,
                    email = _email.value,
                    message = _message.value,
                    platform = "Aplikace"
                )
                ApiClient.apiService.sendFeedback(dto)
                _isSuccess.value = true
            } catch (e: Exception) {
                Timber.e(e, "Error sending feedback, queueing for later")
                try {
                    val entity = FeedbackEntity(
                        name = _name.value,
                        email = _email.value,
                        message = _message.value,
                        platform = "Aplikace"
                    )
                    val db = AppDatabase.getInstance(getApplication())
                    db.feedbackDao().insert(entity)
                    val syncWork = OneTimeWorkRequestBuilder<FeedbackSyncWorker>()
                        .build()
                    WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                        "feedback_sync",
                        ExistingWorkPolicy.REPLACE,
                        syncWork
                    )
                    _isSuccess.value = true
                } catch (e2: Exception) {
                    Timber.e(e2, "Failed to queue feedback")
                    _isError.value = true
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
