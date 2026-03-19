package com.tobiso.tobisoappnative.config

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import com.tobiso.tobisoappnative.BuildConfig
import okhttp3.Credentials
import java.security.MessageDigest

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
        val username = BuildConfig.API_USERNAME
        val password = BuildConfig.API_PASSWORD
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
    fun verifyAppIntegrity(): Boolean {
        if (BuildConfig.DEBUG) {
            logCurrentCertFingerprint()
            return true
        }
        return checkAppSignature()
    }

    private fun logCurrentCertFingerprint() {
        try {
            val ctx = appContext ?: return
            getCurrentSignatures(ctx)?.forEach { sig ->
                val digest = MessageDigest.getInstance("SHA-256")
                val encoded = Base64.encodeToString(digest.digest(sig.toByteArray()), Base64.NO_WRAP)
                android.util.Log.i("SecurityConfig", "Current cert fingerprint: $encoded")
            }
        } catch (e: Exception) {
            android.util.Log.e("SecurityConfig", "Could not read cert fingerprint", e)
        }
    }

    private fun checkAppSignature(): Boolean {
        return try {
            val ctx = appContext ?: return false.also {
                android.util.Log.e("SecurityConfig", "SecurityConfig.initialize() not called")
            }
            val expected = BuildConfig.CERT_FINGERPRINT
            if (expected.isBlank()) {
                android.util.Log.e("SecurityConfig", "CERT_FINGERPRINT not set in local.properties")
                return false
            }
            getCurrentSignatures(ctx)?.any { sig ->
                val digest = MessageDigest.getInstance("SHA-256")
                val encoded = Base64.encodeToString(digest.digest(sig.toByteArray()), Base64.NO_WRAP)
                encoded == expected
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("SecurityConfig", "App signature verification failed", e)
            false
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun getCurrentSignatures(context: android.content.Context): Array<Signature>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures
            }
        } catch (e: Exception) {
            null
        }
    }
    
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
        val secret = BuildConfig.SECURITY_TOKEN_SECRET
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
            android.util.Log.e("SecurityConfig", "Failed to generate security token", e)
            ""
        }
    }
    
    /**
     * Kontrola, zda běží na emulátoru (pro dodatečnou bezpečnost)
     */
    fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
    }
    
    /**
     * Produkční SSL konfigurace - povoluje pouze validní certifikáty
     */
    fun shouldUseTrustAllCerts(): Boolean {
        // Pouze pro debug build variantu – nikdy v release
        return BuildConfig.DEBUG
    }
}