package com.example.tobisoappnative.config

import android.util.Base64
import okhttp3.Credentials
import java.security.MessageDigest

/**
 * Konfigurace bezpečnosti pro produkční prostředí
 * Obsahuje všechna produkční bezpečnostní nastavení
 */
object SecurityConfig {
    
    /**
     * Načtení přihlašovacích údajů z prostředí nebo konfigurace
     * V produkci by měly být uloženy v KeyStore nebo získané z secure API
     */
    fun getApiCredentials(): String {
        // Pro produkci použij Environmental Variables nebo bezpečné úložiště
        val username = System.getenv("TOBISO_API_USERNAME") ?: "admin"
        val password = System.getenv("TOBISO_API_PASSWORD") ?: "secret123"
        return Credentials.basic(username, password)
    }
    
    /**
     * Kontrola integrity aplikace - jednoduchá ochrana proti tampering
     */
    fun verifyAppIntegrity(): Boolean {
        // Jednoduché kontroly integrity aplikace
        return try {
            // Základní kontroly - v produkci by měly být rozšířené
            true // Dočasně vždy true, dokud není implementován úplný integrity check
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Získání bezpečnostního tokenu pro lokální operace
     */
    fun getSecurityToken(): String {
        val baseString = "com.tobiso.tobisoapp_2.0.1"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(baseString.toByteArray())
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (e: Exception) {
            baseString // Fallback
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
        // V produkci vždy false - používej pouze validní certifikáty
        return false
    }
}