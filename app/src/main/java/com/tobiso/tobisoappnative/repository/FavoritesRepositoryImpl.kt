package com.tobiso.tobisoappnative.repository

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tobiso.tobisoappnative.data.savedPostsDataStore
import com.tobiso.tobisoappnative.model.ApiClient
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
private val FAVORITE_POSTS_KEY = stringSetPreferencesKey("favorite_posts_json")

class FavoritesRepositoryImpl(private val application: Application) {

    private val json = Json { ignoreUnknownKeys = true }
    private val dataStore = application.savedPostsDataStore

    val favoritePosts: Flow<List<Post>> = dataStore.data.map { prefs ->
        val jsonSet = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
        jsonSet.mapNotNull { jsonString ->
            try { json.decodeFromString(Post.serializer(), jsonString) } catch (e: Exception) { null }
        }
    }

    suspend fun savePost(post: Post) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
            val alreadySaved = current.any { jsonString ->
                try { json.decodeFromString(Post.serializer(), jsonString).id == post.id } catch (e: Exception) { false }
            }
            if (!alreadySaved) prefs[FAVORITE_POSTS_KEY] = current + json.encodeToString(Post.serializer(), post)
        }
    }

    suspend fun unsavePost(postId: Int) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
            prefs[FAVORITE_POSTS_KEY] = current.filterNot { jsonString ->
                try { json.decodeFromString<Post>(jsonString).id == postId } catch (e: Exception) { false }
            }.toSet()
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
            if (NetworkUtils.isOnline(application)) ApiClient.apiService.getPost(postId)
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
