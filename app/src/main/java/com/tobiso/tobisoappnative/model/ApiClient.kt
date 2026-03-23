package com.tobiso.tobisoappnative.model
import timber.log.Timber

import com.tobiso.tobisoappnative.config.SecurityConfig
import com.tobiso.tobisoappnative.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.CertificatePinner
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://www.tobiso.com/api/"

    /**
     * Vytvoří bezpečný OkHttpClient s produkční SSL konfigurací
     */
    private fun getSecureOkHttpClient(): OkHttpClient {
        val credentials = SecurityConfig.getApiCredentials()
        val builder = OkHttpClient.Builder()
        
        // Certificate pinning – Public Key Pinning pro tobiso.com
        // Hashi vygenerovány: 2026-03-19 pomocí get_ssl_hash.sh
        val certificatePinner = CertificatePinner.Builder()
            .add("tobiso.com", "sha256/i0rpPYzV8YE/KbZ7yWnCBqTdW5LcUhWRXomSrxWFkEU=")
            .add("www.tobiso.com", "sha256/r/tLBf9qkHs3KP7qtA2tjoDCw4GSKnyoxjEycJRblyg=")
            .build()
        builder.certificatePinner(certificatePinner)
        
        // Konfigurace timeouts pro produkci
        builder.connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        builder.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        
        // Protokoly - preferuj HTTP/1.1 pro stabilitu
        builder.protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        
        // Interceptor s bezpečnostními hlavičkami
        builder.addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Authorization", credentials)
                .addHeader("User-Agent", "TobisoApp-Android/${BuildConfig.VERSION_NAME}")
                .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                .addHeader("X-Security-Token", SecurityConfig.getSecurityToken())
            
            // Přidej security headers
            requestBuilder.addHeader("X-Requested-With", "XMLHttpRequest")
            
            val request = requestBuilder.build()
            val response = chain.proceed(request)
            
            if (!response.isSuccessful) {
                Timber.w("HTTP ${response.code}: ${response.message}")
            }
            
            response
        }
        
        return builder.build()
    }

    /**
     * Debug client – standardní SSL validace, pouze bez certificate pinningu.
     * SSL ověření zajišťuje systémové CA + debug-overrides v network_security_config.xml.
     */
    private fun getDebugOkHttpClient(): OkHttpClient {
        val credential = Credentials.basic(BuildConfig.API_USERNAME, BuildConfig.API_PASSWORD)
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", credential)
                    .addHeader("User-Agent", "TobisoApp-Android/${BuildConfig.VERSION_NAME}")
                    .build()
                val response = chain.proceed(request)
                if (!response.isSuccessful) {
                    Timber.e("HTTP error: ${response.code} ${response.message}")
                }
                response
            }
            .build()
    }
    
    /**
     * Výběr správného HTTP klienta na základě prostředí
     */
    private fun getHttpClient(): OkHttpClient {
        // Kontrola integrity aplikace
        if (!SecurityConfig.verifyAppIntegrity()) {
            throw SecurityException("Aplikace selhala při ověření integrity")
        }
        
        return if (SecurityConfig.shouldUseTrustAllCerts()) {
            Timber.w("Používá se debug SSL client - certificate pinning vypnut!")
            getDebugOkHttpClient()
        } else {
            Timber.i("Používá se produkční bezpečný SSL client")
            getSecureOkHttpClient()
        }
    }

    val apiService: ApiService by lazy {
        // Kontrola při inicializaci
        if (SecurityConfig.isRunningOnEmulator()) {
            Timber.i("Aplikace běží na emulátoru")
        }
        
        // Základní Gson bez custom TypeAdapter pro Android 15 kompatibilitu
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .serializeNulls() // Explicitně zacházej s null hodnotami
            .setLenient() // Více tolerantní parsing pro problematické API
            .create()
            
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}


