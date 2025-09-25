package com.example.tobisoappnative.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Správa šifrování citlivých dat pomocí Android KeyStore
 * Používá AES-256-GCM pro maximální bezpečnost
 */
class EncryptionManager private constructor() {
    
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "TobisoAppSecretKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        @Volatile
        private var INSTANCE: EncryptionManager? = null
        
        fun getInstance(): EncryptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncryptionManager().also { INSTANCE = it }
            }
        }
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }
    
    init {
        generateSecretKey()
    }
    
    /**
     * Generuje nebo načte existující klíč z Android KeyStore
     */
    private fun generateSecretKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // Pro automatickou aplikaci
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    /**
     * Zašifruje data pomocí AES-256-GCM
     */
    fun encrypt(plaintext: String): String? {
        try {
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Kombinuj IV a šifrovaný text
            val encryptedData = iv + cipherText
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Chyba při šifrování: ${e.message}")
            return null
        }
    }
    
    /**
     * Dešifruje data pomocí AES-256-GCM
     */
    fun decrypt(encryptedData: String): String? {
        try {
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val decodedData = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Rozděl IV a šifrovaný text
            val iv = decodedData.sliceArray(0 until GCM_IV_LENGTH)
            val cipherText = decodedData.sliceArray(GCM_IV_LENGTH until decodedData.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
            
            val plaintext = cipher.doFinal(cipherText)
            return String(plaintext, Charsets.UTF_8)
            
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Chyba při dešifrování: ${e.message}")
            return null
        }
    }
    
    /**
     * Kontrola, zda je šifrování dostupné
     */
    fun isEncryptionAvailable(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Smazání šifrovacího klíče (pro reset)
     */
    fun deleteSecretKey(): Boolean {
        return try {
            keyStore.deleteEntry(KEY_ALIAS)
            true
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Chyba při mazání klíče: ${e.message}")
            false
        }
    }
    
    /**
     * Bezpečné uložení stringu do SharedPreferences s šifrováním
     */
    fun secureStoreString(context: Context, key: String, value: String): Boolean {
        val encryptedValue = encrypt(value) ?: return false
        return try {
            val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(key, encryptedValue).apply()
            true
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Chyba při ukládání: ${e.message}")
            false
        }
    }
    
    /**
     * Bezpečné načtení stringu z SharedPreferences s dešifrováním
     */
    fun secureRetrieveString(context: Context, key: String): String? {
        return try {
            val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
            val encryptedValue = prefs.getString(key, null) ?: return null
            decrypt(encryptedValue)
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Chyba při načítání: ${e.message}")
            null
        }
    }
}