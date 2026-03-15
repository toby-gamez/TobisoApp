package com.example.tobisoappnative.viewmodel.postdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.Addendum
import com.example.tobisoappnative.model.InteractiveExerciseResponse
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.model.RelatedPost
import com.example.tobisoappnative.repository.FavoritesRepositoryImpl
import com.example.tobisoappnative.repository.PostDetailRepositoryImpl
import com.example.tobisoappnative.repository.PostsRepositoryImpl
import com.example.tobisoappnative.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val detailRepo = PostDetailRepositoryImpl(application, OfflineDataManager(application))
    private val postsRepo = PostsRepositoryImpl(application, OfflineDataManager(application))
    private val favoritesRepo = FavoritesRepositoryImpl(application)
    private val ttsManager = TtsManager(application)

    val favoritePosts: StateFlow<List<Post>> = favoritesRepo.favoritePosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _postDetail = MutableStateFlow<Post?>(null)
    val postDetail: StateFlow<Post?> = _postDetail

    private val _postDetailError = MutableStateFlow<String?>(null)
    val postDetailError: StateFlow<String?> = _postDetailError

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _relatedPosts = MutableStateFlow<List<RelatedPost>>(emptyList())
    val relatedPosts: StateFlow<List<RelatedPost>> = _relatedPosts

    private val _relatedPostsError = MutableStateFlow<String?>(null)
    val relatedPostsError: StateFlow<String?> = _relatedPostsError

    private val _relatedPostsLoading = MutableStateFlow(false)
    val relatedPostsLoading: StateFlow<Boolean> = _relatedPostsLoading

    private val _addendums = MutableStateFlow<List<Addendum>>(emptyList())
    val addendums: StateFlow<List<Addendum>> = _addendums

    private val _addendumsError = MutableStateFlow<String?>(null)
    val addendumsError: StateFlow<String?> = _addendumsError

    private val _addendumsLoading = MutableStateFlow(false)
    val addendumsLoading: StateFlow<Boolean> = _addendumsLoading

    private val _exercises = MutableStateFlow<List<InteractiveExerciseResponse>>(emptyList())
    val exercises: StateFlow<List<InteractiveExerciseResponse>> = _exercises

    private val _exercisesLoading = MutableStateFlow(false)
    val exercisesLoading: StateFlow<Boolean> = _exercisesLoading

    private val _exercisesError = MutableStateFlow<String?>(null)
    val exercisesError: StateFlow<String?> = _exercisesError

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    fun getTtsManager(): TtsManager = ttsManager

    fun loadPostDetail(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            detailRepo.getPostDetail(postId).fold(
                onSuccess = { post ->
                    _postDetail.value = post
                    _postDetailError.value = null
                    _isOffline.value = false
                },
                onFailure = { e ->
                    _postDetailError.value = e.message
                    _isOffline.value = e is IllegalStateException
                }
            )
        }
    }

    fun loadPosts() {
        viewModelScope.launch(Dispatchers.IO) {
            postsRepo.getPostsByCategory(null).onSuccess { _posts.value = it }
        }
    }

    fun loadRelatedPosts(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _relatedPostsLoading.value = true
            detailRepo.getRelatedPosts(postId, _postDetail.value, _posts.value).fold(
                onSuccess = { list ->
                    _relatedPosts.value = list
                    _relatedPostsError.value = null
                },
                onFailure = { e -> _relatedPostsError.value = e.message }
            )
            _relatedPostsLoading.value = false
        }
    }

    fun loadAddendums() {
        viewModelScope.launch(Dispatchers.IO) {
            _addendumsLoading.value = true
            detailRepo.getAddendums().fold(
                onSuccess = { list ->
                    _addendums.value = list
                    _addendumsError.value = null
                },
                onFailure = { e -> _addendumsError.value = e.message }
            )
            _addendumsLoading.value = false
        }
    }

    fun loadExercisesByPostId(postId: Int, postCategoryId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _exercisesLoading.value = true
            detailRepo.getExercisesForPost(postId, postCategoryId).fold(
                onSuccess = { list ->
                    _exercises.value = list
                    _exercisesError.value = null
                },
                onFailure = { e ->
                    _exercisesError.value = "Chyba při načítání cvičení: ${e.message}"
                }
            )
            _exercisesLoading.value = false
        }
    }

    suspend fun checkHasQuestions(postId: Int): Boolean {
        return detailRepo.getQuestionsForPost(postId).getOrNull()?.isNotEmpty() == true
    }

    suspend fun checkHasExercises(postId: Int, postCategoryId: Int? = null): Boolean {
        return detailRepo.getExercisesForPost(postId, postCategoryId).getOrNull()?.isNotEmpty() == true
    }

    suspend fun downloadPostPdf(postId: Int): ResponseBody = detailRepo.downloadPdf(postId)

    fun savePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.savePost(post) }
    }

    fun unsavePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.unsavePost(postId) }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            PostDetailViewModel(application) as T
    }
}
