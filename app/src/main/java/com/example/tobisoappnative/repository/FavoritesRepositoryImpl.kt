package com.example.tobisoappnative.repository

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.tobisoappnative.data.savedPostsDataStore
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Snippet
import com.example.tobisoappnative.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private const val SNIPPETS_FILE_NAME = "snippets.json"
private val FAVORITE_POSTS_KEY = stringSetPreferencesKey("favorite_posts_json")

class FavoritesRepositoryImpl(private val application: Application) {

    private val gson = Gson()
    private val dataStore = application.savedPostsDataStore

    val favoritePosts: Flow<List<Post>> = dataStore.data.map { prefs ->
        val jsonSet = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
        jsonSet.mapNotNull { json ->
            try { gson.fromJson(json, Post::class.java) } catch (e: Exception) { null }
        }
    }

    suspend fun savePost(post: Post) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
            val alreadySaved = current.any { json ->
                try { gson.fromJson(json, Post::class.java).id == post.id } catch (e: Exception) { false }
            }
            if (!alreadySaved) prefs[FAVORITE_POSTS_KEY] = current + gson.toJson(post)
        }
    }

    suspend fun unsavePost(postId: Int) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
            prefs[FAVORITE_POSTS_KEY] = current.filterNot { json ->
                try { gson.fromJson(json, Post::class.java).id == postId } catch (e: Exception) { false }
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
            gson.fromJson(file.readText(), Array<Snippet>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSnippet(snippet: Snippet): List<Snippet> = withContext(Dispatchers.IO) {
        val file = File(application.filesDir, SNIPPETS_FILE_NAME)
        val current = readSnippetsFromFile(file)
        val updated = current + snippet
        file.writeText(gson.toJson(updated))
        updated
    }

    suspend fun removeSnippet(snippet: Snippet): List<Snippet> = withContext(Dispatchers.IO) {
        val file = File(application.filesDir, SNIPPETS_FILE_NAME)
        val current = readSnippetsFromFile(file)
        val updated = current.filterNot {
            it.postId == snippet.postId && it.content == snippet.content && it.createdAt == snippet.createdAt
        }
        file.writeText(gson.toJson(updated))
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
            gson.fromJson(file.readText(), Array<Snippet>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
