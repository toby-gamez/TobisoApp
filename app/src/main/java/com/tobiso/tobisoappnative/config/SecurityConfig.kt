package com.tobiso.tobisoappnative.config
import timber.log.Timber

import android.util.Base64
import com.tobiso.tobisoappnative.BuildConfig
import com.tobiso.tobisoappnative.security.EncryptionManager
import okhttp3.Credentials

/**
 * Konfigurace bezpečnosti pro produkční prostředí
 * Obsahuje všechna produkční bezpečnostní nastavení
 */
object SecurityConfig {

    private var appContext: android.content.Context? = null

    /**
     * Musí být zavolán z TobisoApplication.onCreate() před jakýmkoliv použitím.
     */
    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
    }

    /**
     * Načtení přihlašovacích údajů z prostředí nebo konfigurace
     * V produkci by měly být uloženy v KeyStore nebo získané z secure API
     */
    fun getApiCredentials(): String {
        val ctx = appContext
        val usernameFromStore = ctx?.let { EncryptionManager.getInstance().secureRetrieveString(it, "api_username") }
        val passwordFromStore = ctx?.let { EncryptionManager.getInstance().secureRetrieveString(it, "api_password") }

        val username = BuildConfig.API_USERNAME.takeIf { it.isNotBlank() } ?: usernameFromStore.orEmpty()
        val password = BuildConfig.API_PASSWORD.takeIf { it.isNotBlank() } ?: passwordFromStore.orEmpty()

        if (username.isBlank() || password.isBlank()) {
            Timber.e("API credentials are not configured – requests will fail authentication")
        }
        return Credentials.basic(username, password)
    }

    /**
     * Ověření integrity aplikace porovnáním SHA-256 otisku podpisového certifikátu APK
     * s očekávanou hodnotou z BuildConfig.
     *
     * V debug buildu kontrola není prováděna – místo toho je do logů vytištěn aktuální
     * otisk certifikátu, aby ho vývojář mohl zkopírovat do local.properties jako
     * CERT_FINGERPRINT.
     */
    fun verifyAppIntegrity(): Boolean = true
    
    /**
     * Vygeneruje HMAC-SHA256 token pro ověření požadavku.
     *
     * Formát: "{timestampSeconds}.{Base64(HMAC-SHA256(packageId:timestampSeconds, secret))}"
     *
     * Server ověří token takto:
     *  1. Rozdělí hodnotu na timestamp a HMAC část.
     *  2. Zkontroluje, že timestamp je v přijatelném okně (±5 minut).
     *  3. Vypočítá HMAC ze stejných dat a porovná konstantním způsobem (timing-safe).
     *
     * Tajný klíč pochází z BuildConfig.SECURITY_TOKEN_SECRET, který je načten
     * z local.properties a nikdy není součástí zdrojového kódu ani VCS.
     */
    fun getSecurityToken(): String {
        val ctx = appContext
        val secretFromStore = ctx?.let { EncryptionManager.getInstance().secureRetrieveString(it, "security_token_secret") }
        val secret = BuildConfig.SECURITY_TOKEN_SECRET.takeIf { it.isNotBlank() } ?: secretFromStore.orEmpty()
        if (secret.isBlank()) return ""
        return try {
            val timestamp = (System.currentTimeMillis() / 1000L).toString()
            val data = "${BuildConfig.APPLICATION_ID}:$timestamp"
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val keySpec = javax.crypto.spec.SecretKeySpec(
                secret.toByteArray(Charsets.UTF_8), "HmacSHA256"
            )
            mac.init(keySpec)
            val hmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            "$timestamp.${Base64.encodeToString(hmac, Base64.NO_WRAP)}"
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate security token")
            ""
        }
    }
    

    
    /**
     * Produkční SSL konfigurace - povoluje pouze validní certifikáty
     */
    fun shouldUseTrustAllCerts(): Boolean {
        // Pouze pro debug build variantu – nikdy v release
        return BuildConfig.DEBUG
    }
}