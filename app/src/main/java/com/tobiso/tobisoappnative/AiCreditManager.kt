package com.tobiso.tobisoappnative

import android.content.Context
import com.tobiso.tobisoappnative.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AiCreditManager private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(KEY_DEVICE_ID, it).apply()
    }

    private val _bonusRemaining = MutableStateFlow(0)
    val bonusRemaining: StateFlow<Int> = _bonusRemaining

    /** Last remaining-question count the server reported, or null if never asked yet. */
    val lastKnownRemaining: Int? = prefs.getInt(KEY_LAST_REMAINING, -1).takeIf { it >= 0 }

    fun updateBonusRemaining(remaining: Int) {
        _bonusRemaining.value = remaining
        prefs.edit().putInt(KEY_LAST_REMAINING, remaining).apply()
    }

    fun signCreditRequest(deviceId: String, count: Int, validUntilUtc: Long): String {
        val secret = BuildConfig.SECURITY_TOKEN_SECRET
        if (secret.isBlank()) return ""
        val payload = "$deviceId:$count:$validUntilUtc"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "ai_credit_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_REMAINING = "last_remaining"

        lateinit var instance: AiCreditManager
            private set

        fun initialize(context: Context) {
            instance = AiCreditManager(context.applicationContext)
        }
    }
}
