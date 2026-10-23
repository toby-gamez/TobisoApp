package com.tobiso.tobisoappnative.repository

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import com.tobiso.tobisoappnative.model.AuthSession
import kotlinx.coroutines.flow.first
import timber.log.Timber
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tobiso.tobisoappnative.data.savedPostsDataStore
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Snippet
import com.tobiso.tobisoappnative.utils.NetworkUtils
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private const val SNIPPETS_FILE_NAME = "snippets.json"
private const val PENDING_ADD = "pending_add"
private const val PENDING_REMOVE = "pending_remove"
private const val SYNCED_USERS = "synced_users"
private val FAVORITE_POSTS_KEY = stringSetPreferencesKey("favorite_posts_json")

class FavoritesRepositoryImpl(
    private val application: Application,
    private val api: ApiService
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val dataStore = application.savedPostsDataStore

    val favoritePosts: Flow<List<Post>> = dataStore.data.map { prefs ->
        val jsonSet = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
        jsonSet.mapNotNull { jsonString ->
            try { json.decodeFromString(Post.serializer(), jsonString) } catch (e: Exception) { null }
        }
    }

    suspend fun savePost(post: Post) {
        saveLocal(post)
        queue(add = post.id)
        pushPending()
    }

    suspend fun unsavePost(postId: Int) {
        unsaveLocal(postId)
        queue(remove = postId)
        pushPending()
    }

    private suspend fun saveLocal(post: Post) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
            val alreadySaved = current.any { jsonString ->
                try { json.decodeFromString(Post.serializer(), jsonString).id == post.id } catch (e: Exception) { false }
            }
            if (!alreadySaved) prefs[FAVORITE_POSTS_KEY] = current + json.encodeToString(Post.serializer(), post)
        }
    }

    private suspend fun unsaveLocal(postId: Int) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
            prefs[FAVORITE_POSTS_KEY] = current.filterNot { jsonString ->
                try { json.decodeFromString<Post>(jsonString).id == postId } catch (e: Exception) { false }
            }.toSet()
        }
    }

    // ── Bookmark sync (signed-in users) ──────────────────────────────────────

    private val syncPrefs by lazy { application.getSharedPreferences("favorites_sync", Context.MODE_PRIVATE) }

    private fun ids(key: String): Set<Int> =
        syncPrefs.getStringSet(key, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()

    private fun saveIds(key: String, value: Set<Int>) {
        syncPrefs.edit().putStringSet(key, value.map { it.toString() }.toSet()).apply()
    }

    private fun queue(add: Int? = null, remove: Int? = null) {
        if (!AuthSession.isSignedIn) return
        if (add != null) {
            saveIds(PENDING_ADD, ids(PENDING_ADD) + add)
            saveIds(PENDING_REMOVE, ids(PENDING_REMOVE) - add)
        }
        if (remove != null) {
            saveIds(PENDING_REMOVE, ids(PENDING_REMOVE) + remove)
            saveIds(PENDING_ADD, ids(PENDING_ADD) - remove)
        }
    }

    private fun userKey(): String? = AuthSession.user.value?.let { it.email ?: it.displayName }

    /** Best-effort push of the outbox after a local change; failures stay queued for the next full sync. */
    private suspend fun pushPending() {
        if (!AuthSession.isSignedIn) return
        for (id in ids(PENDING_ADD)) {
            if (runCatching { api.addBookmark(id) }.getOrNull()?.isSuccessful == true)
                saveIds(PENDING_ADD, ids(PENDING_ADD) - id) else return
        }
        for (id in ids(PENDING_REMOVE)) {
            if (runCatching { api.removeBookmark(id) }.getOrNull()?.isSuccessful == true)
                saveIds(PENDING_REMOVE, ids(PENDING_REMOVE) - id) else return
        }
    }

    /** Full reconcile with the server's bookmarks; no-op when anonymous or offline. */
    suspend fun syncBookmarks() = withContext(Dispatchers.IO) {
        val key = userKey() ?: return@withContext
        if (!NetworkUtils.isOnline(application)) return@withContext
        try {
            val server = api.getBookmarks().toSet()
            val localPosts = favoritePosts.first()
            val local = localPosts.map { it.id }.toSet()
            val synced = syncPrefs.getStringSet(SYNCED_USERS, emptySet())!!
            val plan = planBookmarkSync(local, server, ids(PENDING_ADD), ids(PENDING_REMOVE), firstSync = key !in synced)

            plan.push.forEach { api.addBookmark(it) }
            plan.delete.forEach { api.removeBookmark(it) }

            (local - plan.finalIds).forEach { unsaveLocal(it) }
            (plan.finalIds - local).forEach { id ->
                fetchPost(id)?.let { saveLocal(it.copy(content = null, versions = null)) }
            }
            saveIds(PENDING_ADD, emptySet())
            saveIds(PENDING_REMOVE, emptySet())
            syncPrefs.edit().putStringSet(SYNCED_USERS, synced + key).apply()
        } catch (e: Exception) {
            Timber.w(e, "Bookmark sync failed; will retry later")
        }
    }

    suspend fun clearFavoritePosts() {
        dataStore.edit { prefs -> prefs[FAVORITE_POSTS_KEY] = emptySet() }
    }

    suspend fun loadSnippets(): List<Snippet> = withContext(Dispatchers.IO) {
        val file = File(application.filesDir, SNIPPETS_FILE_NAME)
        if (!file.exists()) return@withContext emptyList()
        try {
            try { json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Snippet.serializer()), file.readText()) } catch (e: Exception) { emptyList() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSnippet(snippet: Snippet): List<Snippet> = withContext(Dispatchers.IO) {
        val file = File(application.filesDir, SNIPPETS_FILE_NAME)
        val current = readSnippetsFromFile(file)
        val updated = current + snippet
        file.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Snippet.serializer()), updated))
        updated
    }

    suspend fun removeSnippet(snippet: Snippet): List<Snippet> = withContext(Dispatchers.IO) {
        val file = File(application.filesDir, SNIPPETS_FILE_NAME)
        val current = readSnippetsFromFile(file)
        val updated = current.filterNot {
            it.postId == snippet.postId && it.content == snippet.content && it.createdAt == snippet.createdAt
        }
        file.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Snippet.serializer()), updated))
        updated
    }

    suspend fun clearSnippets() = withContext(Dispatchers.IO) {
        File(application.filesDir, SNIPPETS_FILE_NAME).writeText("[]")
    }

    suspend fun fetchPost(postId: Int): Post? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isOnline(application)) api.getPost(postId)
            else null
        } catch (e: Exception) { null }
    }

    private fun readSnippetsFromFile(file: File): List<Snippet> {
        if (!file.exists()) return emptyList()
        return try {
            try { json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Snippet.serializer()), file.readText()) } catch (e: Exception) { emptyList() }
        } catch (e: Exception) { emptyList() }
    }
}
