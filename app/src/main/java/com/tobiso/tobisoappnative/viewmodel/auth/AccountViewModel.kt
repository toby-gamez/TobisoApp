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

    private val _legalChangedAt = MutableStateFlow<String?>(null)
    /** Timestamp of a Terms/Privacy change the signed-in user has not seen yet; null when there is nothing to show. */
    val legalChangedAt: StateFlow<String?> = _legalChangedAt

    /** Checks for a pending legal notice. Quiet: offline or signed-out simply shows nothing. */
    fun loadLegalNotice() {
        if (!AuthSession.isSignedIn) { _legalChangedAt.value = null; return }
        viewModelScope.launch { _legalChangedAt.value = repo.pendingLegalNotice().getOrNull() }
    }

    /** Hides the notice right away and tells the server so it isn't shown again (also on the web). */
    fun dismissLegalNotice() {
        val changedAt = _legalChangedAt.value ?: return
        _legalChangedAt.value = null
        viewModelScope.launch { repo.dismissLegalNotice(changedAt) }
    }

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

    /** Deletes the account on the server, then erases its data from this device; the user stays signed in on failure. */
    fun deleteAccount(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val owner = AuthSession.ownerKey
            val ok = repo.deleteAccount().isSuccess
            if (ok) gameSync.wipeLocalState(owner)
            onResult(ok)
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
