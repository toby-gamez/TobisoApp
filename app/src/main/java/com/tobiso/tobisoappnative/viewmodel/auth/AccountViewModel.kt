package com.tobiso.tobisoappnative.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.gamesync.GameSyncManager
import com.tobiso.tobisoappnative.model.AuthSession
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
    private val repo: AuthRepository,
    private val gameSync: GameSyncManager
) : ViewModel() {

    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats

    /** Loads the reading stats; failures keep whatever was shown before (offline is fine). */
    fun loadStats() {
        viewModelScope.launch { repo.stats().onSuccess { _stats.value = it } }
    }

    /**
     * Signs out and erases the account's data from this device. Unsynced progress would be lost, so when it can't be
     * uploaded first the user stays signed in ([onResult] false).
     */
    fun logout(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            if (!gameSync.flushBeforeSignOut()) return@launch onResult(false)
            val owner = AuthSession.ownerKey
            repo.logout()
            gameSync.wipeLocalState(owner)
            onResult(true)
        }
    }

    fun logoutEverywhere(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!gameSync.flushBeforeSignOut()) return@launch onResult(false)
            val owner = AuthSession.ownerKey
            val ok = repo.logoutEverywhere().isSuccess
            if (ok) gameSync.wipeLocalState(owner)
            onResult(ok)
        }
    }

    /**
     * Claims the daily AI-credit bonus the first time a post is opened each day while signed in. Quiet: [onClaimed]
     * only fires on success (the server answers 409 when today's bonus was already taken), and one attempt per day
     * is made per app process.
     */
    fun claimDailyBonusOnPostOpen(onClaimed: (Int) -> Unit) {
        if (!AuthSession.isSignedIn) return
        val today = java.time.LocalDate.now().toString()
        if (lastBonusAttemptDay == today) return
        lastBonusAttemptDay = today
        viewModelScope.launch { repo.claimDailyBonus().onSuccess(onClaimed) }
    }

    /** Downloads the account data; [onResult] gets the JSON text or null on failure. */
    fun exportData(onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.exportData().getOrNull()) }
    }

    private companion object {
        var lastBonusAttemptDay: String? = null
    }
}
