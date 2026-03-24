package com.tobiso.tobisoappnative.model
import timber.log.Timber

import android.content.Context
import android.content.SharedPreferences
import com.tobiso.tobisoappnative.db.AppDatabase
import androidx.room.withTransaction
import com.tobiso.tobisoappnative.db.dao.AddendumDao
import com.tobiso.tobisoappnative.db.dao.CategoryDao
import com.tobiso.tobisoappnative.db.dao.EventDao
import com.tobiso.tobisoappnative.db.dao.ExerciseDao
import com.tobiso.tobisoappnative.db.dao.PostDao
import com.tobiso.tobisoappnative.db.dao.QuestionDao
import com.tobiso.tobisoappnative.db.dao.QuestionPostDao
import com.tobiso.tobisoappnative.db.dao.RelatedPostDao
import com.tobiso.tobisoappnative.db.entity.toDomain
import com.tobiso.tobisoappnative.db.entity.toEntity
import com.tobiso.tobisoappnative.db.entity.toQuestionPostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Spravuje offline cache pomocí Room databáze.
 * Metadata (časová razítka) zůstávají ve SharedPreferences (jsou malá).
 * Velká data jsou uložena v Room – Room garantuje přístup pouze z vláken IO
 * a vyhazuje výjimku při volání z hlavního vlákna, čímž zcela eliminuje ANR riziko.
 */
class OfflineDataManager(
    context: Context,
    private val categoryDao: CategoryDao,
    private val postDao: PostDao,
    private val questionPostDao: QuestionPostDao,
    private val questionDao: QuestionDao,
    private val eventDao: EventDao,
    private val addendumDao: AddendumDao,
    private val relatedPostDao: RelatedPostDao,
    private val exerciseDao: ExerciseDao,
    private val db: AppDatabase
) {

    companion object {
        private const val META_PREFS = "offline_meta"
        private const val KEY_LAST_UPDATE = "last_update_timestamp"
        const val CACHE_FRESHNESS_MINUTES = 15
        private const val KEY_LAST_UPDATE_FORMATTED = "last_update_formatted"
        private const val KEY_EVENTS_LAST_UPDATE = "events_last_update_timestamp"

        private val csTimeFormatter = DateTimeFormatter.ofPattern(
            "dd. MM. yyyy 'v' HH:mm", Locale.forLanguageTag("cs-CZ")
        )
    }

    private val appContext = context.applicationContext
    private val metaPrefs: SharedPreferences =
        appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)

    init {
        // Clean up legacy file cache from the previous file-based implementation.
        val legacyCacheDir = File(appContext.filesDir, "offline_cache")
        if (legacyCacheDir.exists()) {
            legacyCacheDir.deleteRecursively()
        }
    }

    // ── Zápis dat ─────────────────────────────────────────────────────────────

    suspend fun saveCategoriesAndPosts(categories: List<Category>, posts: List<Post>) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                categoryDao.deleteAll()
                categoryDao.insertAll(categories.map { it.toEntity() })
                postDao.deleteAll()
                postDao.insertAll(posts.map { it.toEntity() })
            }
            // Záměrně NENASTAVUJEME KEY_LAST_UPDATE – ten nastavuje jen saveRemainingData
            // po stažení kompletních dat. Jinak by isCacheFresh(CACHE_FRESHNESS_MINUTES) blokoval Phase 2.
        }

    /**
     * Uloží zbývající data (otázky, related posts, addenda, cvičení) a nastaví timestamp.
     * Kategorie a posty jsou už v DB z předchozí saveCategoriesAndPosts — nemazat je znovu.
     */
    suspend fun saveRemainingData(
        categories: List<Category>,
        posts: List<Post>,
        questions: List<Question>,
        questionsPosts: List<Post>,
        relatedPosts: List<RelatedPost>,
        addendums: List<Addendum>,
        exercises: List<InteractiveExerciseResponse>
    ) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val formattedTime = Instant.ofEpochMilli(currentTime)
            .atZone(ZoneId.systemDefault()).format(csTimeFormatter)

        // Uložit vše atomicky v transakci. Pokud něco selže, DB zůstane konzistentní.
        db.withTransaction {
            // Kategorie a posty znovu uložit (pro případ přímého volání z OfflineManagerScreen)
            categoryDao.deleteAll()
            categoryDao.insertAll(categories.mapNotNull { runCatching { it.toEntity() }.getOrElse { e -> Timber.w("skip category ${it.id}: ${e.message}"); null } })
            postDao.deleteAll()
            postDao.insertAll(posts.mapNotNull { runCatching { it.toEntity() }.getOrElse { e -> Timber.w("skip post ${it.id}: ${e.message}"); null } })

            questionPostDao.deleteAll()
            questionPostDao.insertAll(questionsPosts.mapNotNull { runCatching { it.toQuestionPostEntity() }.getOrElse { e -> Timber.w("skip questionPost ${it.id}: ${e.message}"); null } })
            questionDao.deleteAll()
            questionDao.insertAll(questions.mapNotNull { runCatching { it.toEntity() }.getOrElse { e -> Timber.w("skip question ${it.id}: ${e.message}"); null } })
            relatedPostDao.deleteAll()
            relatedPostDao.insertAll(relatedPosts.mapNotNull { runCatching { it.toEntity() }.getOrElse { e -> Timber.w("skip relatedPost ${it.id}: ${e.message}"); null } })
            addendumDao.deleteAll()
            addendumDao.insertAll(addendums.mapNotNull { runCatching { it.toEntity() }.getOrElse { e -> Timber.w("skip addendum ${it.id}: ${e.message}"); null } })
            exerciseDao.deleteAll()
            exerciseDao.insertAll(exercises.mapNotNull { runCatching { it.toEntity() }.getOrElse { e -> Timber.w("skip exercise ${it.id}: ${e.message}"); null } })
        }

        // Nastavit metadata PO úspěšné transakci
        metaPrefs.edit()
            .putLong(KEY_LAST_UPDATE, currentTime)
            .putString(KEY_LAST_UPDATE_FORMATTED, formattedTime)
            .apply()
    }

    suspend fun saveEvents(events: List<Event>) = withContext(Dispatchers.IO) {
        try {
            eventDao.deleteRemoteEvents()
            eventDao.insertAll(events.map { it.toEntity() })
            metaPrefs.edit().putLong(KEY_EVENTS_LAST_UPDATE, System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error saving events")
        }
    }

    suspend fun saveAddendums(addendums: List<Addendum>) = withContext(Dispatchers.IO) {
        try {
            addendumDao.deleteAll()
            addendumDao.insertAll(addendums.map { it.toEntity() })
        } catch (e: Exception) {
            Timber.e(e, "Error saving addendums")
        }
    }

    // ── Čtení dat ─────────────────────────────────────────────────────────────

    suspend fun getCachedCategories(): List<Category>? = withContext(Dispatchers.IO) {
        try {
            val entities = categoryDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading categories")
            null
        }
    }

    suspend fun getCachedPosts(): List<Post>? = withContext(Dispatchers.IO) {
        try {
            val entities = postDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading posts")
            null
        }
    }

    suspend fun getCachedPostsByCategory(categoryId: Int): List<Post>? =
        withContext(Dispatchers.IO) {
            try {
                val entities = postDao.getByCategory(categoryId)
                if (entities.isEmpty()) null else entities.map { it.toDomain() }
            } catch (e: Exception) {
                Timber.e(e, "Error loading posts by category")
                null
            }
        }

    suspend fun getCachedPost(postId: Int): Post? = withContext(Dispatchers.IO) {
        try {
            postDao.getById(postId)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error loading post")
            null
        }
    }

    suspend fun getCachedQuestions(): List<Question>? = withContext(Dispatchers.IO) {
        try {
            val entities = questionDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading questions")
            null
        }
    }

    suspend fun getCachedQuestionsByPostId(postId: Int): List<Question>? =
        withContext(Dispatchers.IO) {
            try {
                val entities = questionDao.getByPostId(postId)
                if (entities.isEmpty()) null else entities.map { it.toDomain() }
            } catch (e: Exception) {
                Timber.e(e, "Error loading questions by post")
                null
            }
        }

    suspend fun getCachedQuestionsPosts(): List<Post>? = withContext(Dispatchers.IO) {
        try {
            val entities = questionPostDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading questions posts")
            null
        }
    }

    suspend fun getCachedRelatedPosts(): List<RelatedPost>? = withContext(Dispatchers.IO) {
        try {
            val entities = relatedPostDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading related posts")
            null
        }
    }

    suspend fun getCachedRelatedPostsByPostId(postId: Int): List<RelatedPost>? =
        withContext(Dispatchers.IO) {
            try {
                val entities = relatedPostDao.getByPostId(postId)
                if (entities.isEmpty()) null else entities.map { it.toDomain() }
            } catch (e: Exception) {
                Timber.e(e, "Error loading related posts by post")
                null
            }
        }

    suspend fun getCachedEvents(): List<Event>? = withContext(Dispatchers.IO) {
        try {
            val entities = eventDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading events")
            null
        }
    }

    suspend fun getCachedAddendums(): List<Addendum>? = withContext(Dispatchers.IO) {
        try {
            val entities = addendumDao.getAll()
            if (entities.isEmpty()) null else entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading addendums")
            null
        }
    }

    suspend fun getCachedAddendum(addendumId: Int): Addendum? = withContext(Dispatchers.IO) {
        try {
            addendumDao.getById(addendumId)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error loading addendum")
            null
        }
    }

    suspend fun getCachedExercises(): List<InteractiveExerciseResponse>? =
        withContext(Dispatchers.IO) {
            try {
                val entities = exerciseDao.getAll()
                if (entities.isEmpty()) null else entities.map { it.toDomain() }
            } catch (e: Exception) {
                Timber.e(e, "Error loading exercises")
                null
            }
        }

    suspend fun getCachedExercisesByPostId(postId: Int): List<InteractiveExerciseResponse>? =
        withContext(Dispatchers.IO) {
            // postIds is stored as JSON string; filter in-memory after loading all exercises.
            getCachedExercises()?.filter { it.postIds?.contains(postId) == true }
        }

    suspend fun getCachedExercise(exerciseId: Int): InteractiveExerciseResponse? =
        withContext(Dispatchers.IO) {
            try {
                exerciseDao.getById(exerciseId)?.toDomain()
            } catch (e: Exception) {
                Timber.e(e, "Error loading exercise")
                null
            }
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

    fun hasCachedData(): Boolean = getLastUpdateTimestamp() != null

    fun hasCachedQuestions(): Boolean = getLastUpdateTimestamp() != null
}


