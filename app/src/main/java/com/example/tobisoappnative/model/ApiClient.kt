package com.example.tobisoappnative.model

import com.example.tobisoappnative.config.SecurityConfig
import com.google.gson.GsonBuilder
import okhttp3.CertificatePinner
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.*

object ApiClient {
    private const val BASE_URL = "https://www.tobiso.com/api/"

    /**
     * Vytvoří bezpečný OkHttpClient s produkční SSL konfigurací
     */
    private fun getSecureOkHttpClient(): OkHttpClient {
        val credentials = SecurityConfig.getApiCredentials()
        val builder = OkHttpClient.Builder()
        
        // Produkční SSL konfigurace
        if (!SecurityConfig.shouldUseTrustAllCerts()) {
            // Certificate pinning pro produkci
            val certificatePinner = CertificatePinner.Builder()
                .add("tobiso.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // Placeholder - nahraďte správným hash
                .build()
            builder.certificatePinner(certificatePinner)
        }
        
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
                .addHeader("User-Agent", "TobisoApp-Android/2.0.1")
                .addHeader("X-App-Version", "2.0.1")
                .addHeader("X-Security-Token", SecurityConfig.getSecurityToken())
            
            // Přidej security headers
            requestBuilder.addHeader("X-Requested-With", "XMLHttpRequest")
            
            val request = requestBuilder.build()
            val response = chain.proceed(request)
            
            if (!response.isSuccessful) {
                android.util.Log.w("ApiClient", "HTTP ${response.code}: ${response.message}")
            }
            
            response
        }
        
        return builder.build()
    }

    /**
     * Nebezpečný client pouze pro development - POUŽÍT POUZE PRO DEBUG!
     */
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        // Development credentials - pouze pro debug buildy
        val credential = Credentials.basic("admin", "secret123")
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            
            // Pro Android 15 - použij TLSv1.3 pokud je dostupný, jinak TLSv1.2
            val sslContext = try {
                SSLContext.getInstance("TLSv1.3")
            } catch (e: Exception) {
                SSLContext.getInstance("TLSv1.2")
            }
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val trustManager = trustAllCerts[0] as X509TrustManager
            
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
            builder.hostnameVerifier { _, _ -> true }
            
            // Pro Android 15 - prodluž timeouts a zakáž HTTP/2
            builder.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            builder.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            builder.protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            
            builder.addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", credential)
                    .addHeader("User-Agent", "TobisoApp-Android") // Pro Android 15
                    .build()
                val response = chain.proceed(request)
                if (!response.isSuccessful) {
                    android.util.Log.e("ApiClient", "HTTP error: ${response.code} ${response.message}")
                }
                response
            }
            builder.build()
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Failed to create unsafe client: ${e.message}")
            // Fallback na základní OkHttpClient
            OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request: Request = chain.request().newBuilder()
                        .addHeader("Authorization", credential)
                        .build()
                    chain.proceed(request)
                }
                .build()
        }
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
            android.util.Log.w("ApiClient", "Používá se nebezpečný SSL client - pouze pro vývoj!")
            getUnsafeOkHttpClient()
        } else {
            android.util.Log.i("ApiClient", "Používá se produkční bezpečný SSL client")
            getSecureOkHttpClient()
        }
    }

    val apiService: ApiService by lazy {
        // Kontrola při inicializaci
        if (SecurityConfig.isRunningOnEmulator()) {
            android.util.Log.i("ApiClient", "Aplikace běží na emulátoru")
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


