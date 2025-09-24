package com.example.tobisoappnative.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class OfflineDataManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "offline_data"
        private const val KEY_CATEGORIES = "categories_json"
        private const val KEY_POSTS = "posts_json"
        private const val KEY_LAST_UPDATE = "last_update_timestamp"
        private const val KEY_LAST_UPDATE_FORMATTED = "last_update_formatted"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Uložení kategorií a postů s časovým razítkem
     */
    suspend fun saveCategoriesAndPosts(categories: List<Category>, posts: List<Post>) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", Locale("cs", "CZ"))
        val formattedTime = formatter.format(Date(currentTime))
        
        prefs.edit().apply {
            putString(KEY_CATEGORIES, gson.toJson(categories))
            putString(KEY_POSTS, gson.toJson(posts))
            putLong(KEY_LAST_UPDATE, currentTime)
            putString(KEY_LAST_UPDATE_FORMATTED, formattedTime)
            apply()
        }
        
        println("DEBUG: Offline data saved - Categories: ${categories.size}, Posts: ${posts.size}, Time: $formattedTime")
    }
    
    /**
     * Načtení uložených kategorií
     */
    suspend fun getCachedCategories(): List<Category>? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_CATEGORIES, null)
        return@withContext if (json != null) {
            try {
                val type = object : TypeToken<List<Category>>() {}.type
                gson.fromJson<List<Category>>(json, type)
            } catch (e: Exception) {
                println("DEBUG: Error loading cached categories: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Načtení uložených postů
     */
    suspend fun getCachedPosts(): List<Post>? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_POSTS, null)
        return@withContext if (json != null) {
            try {
                val type = object : TypeToken<List<Post>>() {}.type
                gson.fromJson<List<Post>>(json, type)
            } catch (e: Exception) {
                println("DEBUG: Error loading cached posts: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Načtení postů pro konkrétní kategorii z cache
     */
    suspend fun getCachedPostsByCategory(categoryId: Int): List<Post>? = withContext(Dispatchers.IO) {
        getCachedPosts()?.filter { it.categoryId == categoryId }
    }
    
    /**
     * Načtení konkrétního postu z cache
     */
    suspend fun getCachedPost(postId: Int): Post? = withContext(Dispatchers.IO) {
        getCachedPosts()?.find { it.id == postId }
    }
    
    /**
     * Získání formátovaného času poslední aktualizace
     */
    fun getLastUpdateFormatted(): String? {
        return prefs.getString(KEY_LAST_UPDATE_FORMATTED, null)
    }
    
    /**
     * Kontrola, zda máme uložená data
     */
    fun hasCachedData(): Boolean {
        return prefs.contains(KEY_CATEGORIES) && prefs.contains(KEY_POSTS)
    }
}