package com.example.tobisoappnative.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class OfflineDataManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "offline_data"
        private const val KEY_CATEGORIES = "categories_json"
        private const val KEY_POSTS = "posts_json"
        private const val KEY_QUESTIONS = "questions_json"
        private const val KEY_QUESTIONS_POSTS = "questions_posts_json"
        private const val KEY_LAST_UPDATE = "last_update_timestamp"
        private const val KEY_LAST_UPDATE_FORMATTED = "last_update_formatted"
            private const val KEY_EVENTS = "events_json"
            private const val KEY_EVENTS_LAST_UPDATE = "events_last_update_timestamp"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Uložení kategorií a postů s časovým razítkem
     */
    suspend fun saveCategoriesAndPosts(categories: List<Category>, posts: List<Post>) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", Locale.Builder().setLanguage("cs").setRegion("CZ").build())
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
     * Uložení kategorií, postů a otázek s časovým razítkem
     */
    suspend fun saveCategoriesPostsAndQuestions(
        categories: List<Category>, 
        posts: List<Post>, 
        questions: List<Question>, 
        questionsPosts: List<Post>
    ) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", Locale.Builder().setLanguage("cs").setRegion("CZ").build())
        val formattedTime = formatter.format(Date(currentTime))
        
        prefs.edit().apply {
            putString(KEY_CATEGORIES, gson.toJson(categories))
            putString(KEY_POSTS, gson.toJson(posts))
            putString(KEY_QUESTIONS, gson.toJson(questions))
            putString(KEY_QUESTIONS_POSTS, gson.toJson(questionsPosts))
            putLong(KEY_LAST_UPDATE, currentTime)
            putString(KEY_LAST_UPDATE_FORMATTED, formattedTime)
            apply()
        }
        
        println("DEBUG: Offline data saved - Categories: ${categories.size}, Posts: ${posts.size}, Questions: ${questions.size}, Questions Posts: ${questionsPosts.size}, Time: $formattedTime")
    }
    
    /**
     * Načtení uložených kategorií
     */
    suspend fun getCachedCategories(): List<Category>? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_CATEGORIES, null)
        return@withContext if (json != null) {
            try {
                // Použití Array místo TypeToken pro Android 15 kompatibilitu
                val categoriesArray = gson.fromJson(json, Array<Category>::class.java)
                categoriesArray?.toList()
            } catch (e: Exception) {
                println("DEBUG: Error loading cached categories: ${e.message}")
                e.printStackTrace()
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
                // Použití Array místo TypeToken pro Android 15 kompatibilitu
                val postsArray = gson.fromJson(json, Array<Post>::class.java)
                postsArray?.toList()
            } catch (e: Exception) {
                println("DEBUG: Error loading cached posts: ${e.message}")
                e.printStackTrace()
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
     * Vrátí čas (millis) poslední aktualizace offline dat nebo null pokud není
     */
    fun getLastUpdateTimestamp(): Long? {
        return if (prefs.contains(KEY_LAST_UPDATE)) prefs.getLong(KEY_LAST_UPDATE, 0L) else null
    }

    /**
     * Ověří, zda byla offline data aktualizována v posledních `minutes` minutách
     */
    fun isCacheFresh(minutes: Int): Boolean {
        val ts = getLastUpdateTimestamp() ?: return false
        val ageMillis = System.currentTimeMillis() - ts
        return ageMillis <= minutes * 60 * 1000L
    }
    
    /**
     * Načtení uložených otázek
     */
    suspend fun getCachedQuestions(): List<Question>? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_QUESTIONS, null)
        return@withContext if (json != null) {
            try {
                val questionsArray = gson.fromJson(json, Array<Question>::class.java)
                questionsArray?.toList()
            } catch (e: Exception) {
                println("DEBUG: Error loading cached questions: ${e.message}")
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Načtení otázek pro konkrétní post z cache
     */
    suspend fun getCachedQuestionsByPostId(postId: Int): List<Question>? = withContext(Dispatchers.IO) {
        getCachedQuestions()?.filter { it.postId == postId }
    }
    
    /**
     * Načtení uložených postů pro otázky
     */
    suspend fun getCachedQuestionsPosts(): List<Post>? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_QUESTIONS_POSTS, null)
        return@withContext if (json != null) {
            try {
                val postsArray = gson.fromJson(json, Array<Post>::class.java)
                postsArray?.toList()
            } catch (e: Exception) {
                println("DEBUG: Error loading cached questions posts: ${e.message}")
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    /**
     * Kontrola, zda máme uložená data
     */
    fun hasCachedData(): Boolean {
        return prefs.contains(KEY_CATEGORIES) && prefs.contains(KEY_POSTS)
    }

    /**
     * Kontrola, zda máme uložené otázky
     */
    fun hasCachedQuestions(): Boolean {
        return prefs.contains(KEY_QUESTIONS) && prefs.contains(KEY_QUESTIONS_POSTS)
    }

    /**
     * Uložení eventů (např. z API) spolu s vlastním timestampem
     */
    suspend fun saveEvents(events: List<Event>) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            prefs.edit().apply {
                putString(KEY_EVENTS, gson.toJson(events))
                putLong(KEY_EVENTS_LAST_UPDATE, currentTime)
                apply()
            }
            println("DEBUG: Offline events saved - Count: ${events.size}")
        } catch (e: Exception) {
            println("DEBUG: Error saving offline events: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Načtení uložených eventů
     */
    suspend fun getCachedEvents(): List<Event>? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_EVENTS, null)
        return@withContext if (json != null) {
            try {
                val eventsArray = gson.fromJson(json, Array<Event>::class.java)
                eventsArray?.toList()
            } catch (e: Exception) {
                println("DEBUG: Error loading cached events: ${e.message}")
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    /**
     * Vrátí čas (millis) poslední aktualizace event cache nebo null pokud není
     */
    fun getLastEventsUpdateTimestamp(): Long? {
        return if (prefs.contains(KEY_EVENTS_LAST_UPDATE)) prefs.getLong(KEY_EVENTS_LAST_UPDATE, 0L) else null
    }

    /**
     * Ověří, zda byla event cache aktualizována v posledních `minutes` minutách
     */
    fun isEventsCacheFresh(minutes: Int): Boolean {
        val ts = getLastEventsUpdateTimestamp() ?: return false
        val ageMillis = System.currentTimeMillis() - ts
        return ageMillis <= minutes * 60 * 1000L
    }
}