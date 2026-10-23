package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Runs the account syncs whenever a user signs in (and once at app start if already signed in). */
@Singleton
class UserSyncManager @Inject constructor(
    private val favorites: FavoritesRepositoryImpl
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            AuthSession.user
                .map { user -> user?.let { it.email ?: it.displayName } }
                .distinctUntilChanged()
                .collect { key -> if (key != null) favorites.syncBookmarks() }
        }
    }
}
