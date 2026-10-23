package com.tobiso.tobisoappnative.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.model.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.tobiso.tobisoappnative.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sign-out actions for the Profile "Účet" section. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats

    /** Loads the reading stats; failures keep whatever was shown before (offline is fine). */
    fun loadStats() {
        viewModelScope.launch { repo.stats().onSuccess { _stats.value = it } }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }

    fun logoutEverywhere(onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.logoutEverywhere().isSuccess) }
    }
}
