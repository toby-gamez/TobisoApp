package com.tobiso.tobisoappnative.viewmodel.categorylist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.repository.FavoritesRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryListState(
    val categories: List<Category> = emptyList(),
    val posts: List<Post> = emptyList(),
    val favoritePosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    application: Application,
    private val postsRepo: PostsRepository,
    private val favoritesRepo: FavoritesRepositoryImpl
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CategoryListState())
    val state: StateFlow<CategoryListState> = _state

    val favoritePosts: StateFlow<List<Post>> = favoritesRepo.favoritePosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            postsRepo.getCategories().fold(
                onSuccess = { cats ->
                    _state.value = _state.value.copy(categories = cats, isLoading = false)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message,
                        isOffline = e is IllegalStateException
                    )
                }
            )
        }
    }

    fun loadPosts(categoryId: Int?) {
        if (categoryId == null) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true)
            postsRepo.getPostsByCategory(categoryId).fold(
                onSuccess = { posts ->
                    _state.value = _state.value.copy(posts = posts, isLoading = false, error = null)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message,
                        isOffline = e is IllegalStateException
                    )
                }
            )
        }
    }

    fun savePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.savePost(post) }
    }

    fun unsavePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.unsavePost(postId) }
    }
}
