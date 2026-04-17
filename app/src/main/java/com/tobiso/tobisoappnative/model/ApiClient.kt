package com.tobiso.tobisoappnative.model
import timber.log.Timber

import com.tobiso.tobisoappnative.config.SecurityConfig
import com.tobiso.tobisoappnative.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {
    private const val BASE_URL = "https://www.tobiso.com/api/"

    /**
     * Vytvoří bezpečný OkHttpClient s produkční SSL konfigurací
     */
    private fun getSecureOkHttpClient(): OkHttpClient {
        val credentials = SecurityConfig.getApiCredentials()
        val builder = OkHttpClient.Builder()
        
        // Certificate pinning removed per request — use system trust anchors.
        Timber.w("Certificate pinning disabled; using system trust anchors")
        
        // Konfigurace timeouts pro produkci
        builder.connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        builder.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        
        // Nevynucuj protokol zde — nechej OkHttp vyjednat nejlepší protokol (HTTP/2 pokud server podporuje)
        // (Původně: builder.protocols(listOf(okhttp3.Protocol.HTTP_1_1)))
        
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
        
        // Použij kotlinx.serialization pro Retrofit
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}


