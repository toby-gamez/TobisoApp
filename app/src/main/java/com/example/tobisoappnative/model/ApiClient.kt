package com.example.tobisoappnative.model

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // Trust all certificates (pouze pro vývoj!)
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val credential = Credentials.basic(USERNAME, PASSWORD)
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val trustManager = trustAllCerts[0] as X509TrustManager
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
            builder.hostnameVerifier { _, _ -> true }
            builder.addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", credential)
                    .build()
                val response = chain.proceed(request)
                if (!response.isSuccessful) {
                    android.util.Log.e("ApiClient", "HTTP error: ${response.code} ${response.message}")
                }
                response
            }
            builder.build()
        } catch (e: Exception) {
            OkHttpClient.Builder().build()
        }
    }

    val apiService: ApiService by lazy {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(Date::class.java, object : TypeAdapter<Date>() {
            private val format1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault())
            private val format2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            private val format3 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            
            override fun write(out: JsonWriter, value: Date?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.value(format2.format(value))
                }
            }
            
            override fun read(reader: JsonReader): Date? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                }
                
                val str = reader.nextString()
                return try { 
                    format1.parse(str) 
                } catch (e: Exception) { 
                    try {
                        format3.parse(str)
                    } catch (e: Exception) {
                        try {
                            format2.parse(str)
                        } catch (e: Exception) { 
                            // Pokud se nepodaří parsovat datum, vrátíme null místo chyby
                            android.util.Log.w("ApiClient", "Failed to parse date: $str")
                            null 
                        }
                    }
                }
            }
        })
        val gson = gsonBuilder.create()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
