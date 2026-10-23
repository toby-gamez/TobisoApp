package com.tobiso.tobisoappnative.model

import android.content.Context
import com.tobiso.tobisoappnative.security.EncryptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class AuthUser(
    val displayName: String,
    val email: String? = null,
    val credits: Int = 0
)

/**
 * Optional user session. When a JWT is set, requests carry `Authorization: Bearer <jwt>`
 * instead of the app's Basic credentials (the server picks its auth scheme from that prefix,
 * see Tobiso.Web `SmartAuth`). With no token the app stays anonymous.
 *
 * The token and profile are persisted encrypted (Android Keystore, AES-GCM) so the user stays
 * signed in across restarts. Without [initialize] (e.g. in unit tests) the session is memory-only.
 */
object AuthSession {
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER = "auth_user"

    @Volatile
    var token: String? = null
        private set

    private val _user = MutableStateFlow<AuthUser?>(null)
    val user: StateFlow<AuthUser?> = _user

    /** True after the server rejected our token; cleared by [consumeExpired]. */
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    private var appContext: Context? = null
    private val json = Json { ignoreUnknownKeys = true }

    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val enc = EncryptionManager.getInstance()
        val stored = enc.secureRetrieveString(context, KEY_TOKEN)
        if (stored.isNullOrBlank()) return
        token = stored
        _user.value = enc.secureRetrieveString(context, KEY_USER)
            ?.let { runCatching { json.decodeFromString<AuthUser>(it) }.getOrNull() }
            ?: AuthUser(displayName = "")
    }

    fun signIn(token: String, user: AuthUser) {
        this.token = token
        _user.value = user
        _sessionExpired.value = false
        appContext?.let {
            val enc = EncryptionManager.getInstance()
            enc.secureStoreString(it, KEY_TOKEN, token)
            enc.secureStoreString(it, KEY_USER, json.encodeToString(user))
        }
    }

    fun updateUser(user: AuthUser) {
        if (!isSignedIn) return
        _user.value = user
        appContext?.let {
            EncryptionManager.getInstance().secureStoreString(it, KEY_USER, json.encodeToString(user))
        }
    }

    fun signOut() {
        token = null
        _user.value = null
        appContext?.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
            ?.edit()?.remove(KEY_TOKEN)?.remove(KEY_USER)?.apply()
    }

    /** Called when a request with our token got a 401: drop the session and ask the user to sign in again. */
    fun onUnauthorized() {
        if (!isSignedIn) return
        Timber.w("Session rejected by server (401), falling back to anonymous")
        signOut()
        _sessionExpired.value = true
    }

    fun consumeExpired() {
        _sessionExpired.value = false
    }
}
