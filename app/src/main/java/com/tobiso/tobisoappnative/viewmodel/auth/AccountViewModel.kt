package com.tobiso.tobisoappnative.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sign-out actions for the Profile "Účet" section. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }

    fun logoutEverywhere(onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.logoutEverywhere().isSuccess) }
    }
}
