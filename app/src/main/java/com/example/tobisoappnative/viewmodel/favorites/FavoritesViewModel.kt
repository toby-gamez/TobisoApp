package com.example.tobisoappnative.viewmodel.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Snippet
import com.example.tobisoappnative.repository.FavoritesRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    application: Application,
    private val repo: FavoritesRepositoryImpl
) : AndroidViewModel(application) {

    val favoritePosts: StateFlow<List<Post>> = repo.favoritePosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _snippets = MutableStateFlow<List<Snippet>>(emptyList())
    val snippets: StateFlow<List<Snippet>> = _snippets

    // In-memory cache of fetched posts for snippet title resolution
    private val _fetchedPosts = MutableStateFlow<List<Post>>(emptyList())
    val fetchedPosts: StateFlow<List<Post>> = _fetchedPosts

    fun loadSnippets() {
        viewModelScope.launch(Dispatchers.IO) {
            _snippets.value = repo.loadSnippets()
        }
    }

    fun removeSnippet(snippet: Snippet) {
        viewModelScope.launch(Dispatchers.IO) {
            _snippets.value = repo.removeSnippet(snippet)
        }
    }

    fun clearSnippets() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearSnippets()
            _snippets.value = emptyList()
        }
    }

    fun clearFavoritePosts() {
        viewModelScope.launch(Dispatchers.IO) { repo.clearFavoritePosts() }
    }

    fun unsavePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) { repo.unsavePost(postId) }
    }

    fun fetchAndCachePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_fetchedPosts.value.any { it.id == postId }) return@launch
            val post = repo.fetchPost(postId) ?: return@launch
            _fetchedPosts.value = _fetchedPosts.value + post
        }
    }
}
