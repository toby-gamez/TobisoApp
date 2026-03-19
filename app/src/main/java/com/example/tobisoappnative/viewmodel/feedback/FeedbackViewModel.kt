package com.example.tobisoappnative.viewmodel.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.FeedbackDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor() : ViewModel() {

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
                android.util.Log.e("FeedbackViewModel", "Error sending feedback", e)
                _isError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
