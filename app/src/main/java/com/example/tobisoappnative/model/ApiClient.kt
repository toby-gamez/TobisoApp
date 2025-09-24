package com.example.tobisoappnative.model

import com.google.gson.GsonBuilder
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.*

object ApiClient {
    private const val BASE_URL = "https://www.tobiso.com/api/"

    // Přihlašovací údaje natvrdo
    private const val USERNAME = "admin"
    private const val PASSWORD = "secret123"

    // Trust all certificates (pouze pro vývoj!) - Android 15 compatible
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val credential = Credentials.basic(USERNAME, PASSWORD)
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

    val apiService: ApiService by lazy {
        // Základní Gson bez custom TypeAdapter pro Android 15 kompatibilitu
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}


