package com.tobiso.tobisoappnative.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Spravuje offline cache. Velká JSON data jsou ukládána jako soubory na disk
 * (filesDir/offline_cache/), nikoli do SharedPreferences – čímž se zabrání
 * potenciálním ANR způsobeným synchronním načítáním velikých SharedPreferences souborů.
 * Malá metadata (časová razítka) zůstávají ve SharedPreferences.
 */
class OfflineDataManager(context: Context) {

    companion object {
        private const val META_PREFS = "offline_meta"
        private const val KEY_LAST_UPDATE = "last_update_timestamp"
        private const val KEY_LAST_UPDATE_FORMATTED = "last_update_formatted"
        private const val KEY_EVENTS_LAST_UPDATE = "events_last_update_timestamp"

        private const val FILE_CATEGORIES = "categories.json"
        private const val FILE_POSTS = "posts.json"
        private const val FILE_QUESTIONS = "questions.json"
        private const val FILE_QUESTIONS_POSTS = "questions_posts.json"
        private const val FILE_RELATED_POSTS = "related_posts.json"
        private const val FILE_EVENTS = "events.json"
        private const val FILE_ADDENDUMS = "addendums.json"
        private const val FILE_EXERCISES = "exercises.json"

        private val csTimeFormatter = DateTimeFormatter.ofPattern(
            "dd. MM. yyyy 'v' HH:mm", Locale.forLanguageTag("cs-CZ")
        )
    }

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "offline_cache")
    private val metaPrefs: SharedPreferences =
        appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        cacheDir.mkdirs()
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun writeJson(fileName: String, json: String) {
        File(cacheDir, fileName).writeText(json)
    }

    private fun readJson(fileName: String): String? {
        val file = File(cacheDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    private fun fileExists(fileName: String) = File(cacheDir, fileName).exists()

    // ── Zápis dat ─────────────────────────────────────────────────────────────

    suspend fun saveCategoriesAndPosts(categories: List<Category>, posts: List<Post>) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val formattedTime = Instant.ofEpochMilli(currentTime)
            .atZone(ZoneId.systemDefault()).format(csTimeFormatter)

        writeJson(FILE_CATEGORIES, gson.toJson(categories))
        writeJson(FILE_POSTS, gson.toJson(posts))
        metaPrefs.edit()
            .putLong(KEY_LAST_UPDATE, currentTime)
            .putString(KEY_LAST_UPDATE_FORMATTED, formattedTime)
            .apply()
    }

    suspend fun saveCategoriesPostsAndQuestions(
        categories: List<Category>,
        posts: List<Post>,
        questions: List<Question>,
        questionsPosts: List<Post>,
        relatedPosts: List<RelatedPost> = emptyList(),
        addendums: List<Addendum> = emptyList(),
        exercises: List<InteractiveExerciseResponse> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val formattedTime = Instant.ofEpochMilli(currentTime)
            .atZone(ZoneId.systemDefault()).format(csTimeFormatter)

        writeJson(FILE_CATEGORIES, gson.toJson(categories))
        writeJson(FILE_POSTS, gson.toJson(posts))
        writeJson(FILE_QUESTIONS, gson.toJson(questions))
        writeJson(FILE_QUESTIONS_POSTS, gson.toJson(questionsPosts))
        writeJson(FILE_RELATED_POSTS, gson.toJson(relatedPosts))
        writeJson(FILE_ADDENDUMS, gson.toJson(addendums))
        writeJson(FILE_EXERCISES, gson.toJson(exercises))
        metaPrefs.edit()
            .putLong(KEY_LAST_UPDATE, currentTime)
            .putString(KEY_LAST_UPDATE_FORMATTED, formattedTime)
            .apply()
    }

    suspend fun saveEvents(events: List<Event>) = withContext(Dispatchers.IO) {
        try {
            writeJson(FILE_EVENTS, gson.toJson(events))
            metaPrefs.edit().putLong(KEY_EVENTS_LAST_UPDATE, System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            android.util.Log.e("OfflineDataManager", "Error saving events", e)
        }
    }

    suspend fun saveAddendums(addendums: List<Addendum>) = withContext(Dispatchers.IO) {
        try {
            writeJson(FILE_ADDENDUMS, gson.toJson(addendums))
        } catch (e: Exception) {
            android.util.Log.e("OfflineDataManager", "Error saving addendums", e)
        }
    }

    suspend fun saveExercises(exercises: List<InteractiveExerciseResponse>) = withContext(Dispatchers.IO) {
        try {
            writeJson(FILE_EXERCISES, gson.toJson(exercises))
        } catch (e: Exception) {
            android.util.Log.e("OfflineDataManager", "Error saving exercises", e)
        }
    }

    // ── Čtení dat ─────────────────────────────────────────────────────────────

    suspend fun getCachedCategories(): List<Category>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_CATEGORIES) ?: return@withContext null
        try { gson.fromJson(json, Array<Category>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading categories", e); null }
    }

    suspend fun getCachedPosts(): List<Post>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_POSTS) ?: return@withContext null
        try { gson.fromJson(json, Array<Post>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading posts", e); null }
    }

    suspend fun getCachedPostsByCategory(categoryId: Int): List<Post>? = withContext(Dispatchers.IO) {
        getCachedPosts()?.filter { it.categoryId == categoryId }
    }

    suspend fun getCachedPost(postId: Int): Post? = withContext(Dispatchers.IO) {
        getCachedPosts()?.find { it.id == postId }
    }

    suspend fun getCachedQuestions(): List<Question>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_QUESTIONS) ?: return@withContext null
        try { gson.fromJson(json, Array<Question>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading questions", e); null }
    }

    suspend fun getCachedQuestionsByPostId(postId: Int): List<Question>? = withContext(Dispatchers.IO) {
        getCachedQuestions()?.filter { it.postId == postId }
    }

    suspend fun getCachedQuestionsPosts(): List<Post>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_QUESTIONS_POSTS) ?: return@withContext null
        try { gson.fromJson(json, Array<Post>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading questions posts", e); null }
    }

    suspend fun getCachedRelatedPosts(): List<RelatedPost>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_RELATED_POSTS) ?: return@withContext null
        try { gson.fromJson(json, Array<RelatedPost>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading related posts", e); null }
    }

    suspend fun getCachedRelatedPostsByPostId(postId: Int): List<RelatedPost>? = withContext(Dispatchers.IO) {
        getCachedRelatedPosts()?.filter { it.postId == postId }
    }

    suspend fun getCachedEvents(): List<Event>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_EVENTS) ?: return@withContext null
        try { gson.fromJson(json, Array<Event>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading events", e); null }
    }

    suspend fun getCachedAddendums(): List<Addendum>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_ADDENDUMS) ?: return@withContext null
        try { gson.fromJson(json, Array<Addendum>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading addendums", e); null }
    }

    suspend fun getCachedAddendum(addendumId: Int): Addendum? = withContext(Dispatchers.IO) {
        getCachedAddendums()?.find { it.id == addendumId }
    }

    suspend fun getCachedExercises(): List<InteractiveExerciseResponse>? = withContext(Dispatchers.IO) {
        val json = readJson(FILE_EXERCISES) ?: return@withContext null
        try { gson.fromJson(json, Array<InteractiveExerciseResponse>::class.java)?.toList() }
        catch (e: Exception) { android.util.Log.e("OfflineDataManager", "Error loading exercises", e); null }
    }

    suspend fun getCachedExercisesByPostId(postId: Int): List<InteractiveExerciseResponse>? = withContext(Dispatchers.IO) {
        getCachedExercises()?.filter { it.postIds?.contains(postId) == true }
    }

    suspend fun getCachedExercise(exerciseId: Int): InteractiveExerciseResponse? = withContext(Dispatchers.IO) {
        getCachedExercises()?.find { it.id == exerciseId }
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    fun getLastUpdateFormatted(): String? = metaPrefs.getString(KEY_LAST_UPDATE_FORMATTED, null)

    fun getLastUpdateTimestamp(): Long? =
        if (metaPrefs.contains(KEY_LAST_UPDATE)) metaPrefs.getLong(KEY_LAST_UPDATE, 0L) else null

    fun isCacheFresh(minutes: Int): Boolean {
        val ts = getLastUpdateTimestamp() ?: return false
        return (System.currentTimeMillis() - ts) <= minutes * 60 * 1000L
    }

    fun getLastEventsUpdateTimestamp(): Long? =
        if (metaPrefs.contains(KEY_EVENTS_LAST_UPDATE)) metaPrefs.getLong(KEY_EVENTS_LAST_UPDATE, 0L) else null

    fun isEventsCacheFresh(minutes: Int): Boolean {
        val ts = getLastEventsUpdateTimestamp() ?: return false
        return (System.currentTimeMillis() - ts) <= minutes * 60 * 1000L
    }

    fun hasCachedData(): Boolean = fileExists(FILE_CATEGORIES) && fileExists(FILE_POSTS)

    fun hasCachedQuestions(): Boolean = fileExists(FILE_QUESTIONS) && fileExists(FILE_QUESTIONS_POSTS)
}


