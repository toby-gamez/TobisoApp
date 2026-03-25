package com.tobiso.tobisoappnative.viewmodel.postdetail

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.model.Addendum
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.model.RelatedPost
import com.tobiso.tobisoappnative.repository.FavoritesRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostDetailRepository
import com.tobiso.tobisoappnative.repository.PostsRepository
import com.tobiso.tobisoappnative.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import javax.inject.Inject
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import kotlinx.coroutines.withContext
import android.provider.MediaStore

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    application: Application,
    private val detailRepo: PostDetailRepository,
    private val postsRepo: PostsRepository,
    private val favoritesRepo: FavoritesRepositoryImpl
) : AndroidViewModel(application) {

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

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected

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

    // Download + save handled in ViewModel to avoid heavy IO in Composables
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress

    private val _downloadUri = MutableStateFlow<String?>(null)
    val downloadUri: StateFlow<String?> = _downloadUri

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError

    fun startDownloadAndSavePdf(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val responseBody = detailRepo.downloadPdf(postId)
                val fileName = "tobiso_post_${postId}.pdf"
                var pdfUri: Uri? = null

                responseBody.use { body ->
                    val input = body.byteStream()
                    val contentLength = try { body.contentLength() } catch (_: Exception) { -1L }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }

                        val resolver = getApplication<Application>().contentResolver
                        pdfUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                        pdfUri?.let { uri ->
                            resolver.openOutputStream(uri)?.use { outputStream ->
                                val buffer = ByteArray(8 * 1024)
                                var read: Int
                                var total = 0L
                                while (input.read(buffer).also { read = it } != -1) {
                                    outputStream.write(buffer, 0, read)
                                    total += read
                                    if (contentLength > 0) {
                                        val percent = ((total * 100) / contentLength).toInt().coerceIn(0, 100)
                                        _downloadProgress.value = percent
                                    } else {
                                        _downloadProgress.value = -1
                                    }
                                }
                                outputStream.flush()
                            }
                        }
                    } else {
                        val context = getApplication<Application>()
                        val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        val pdfFile = File(appDownloads ?: context.filesDir, fileName)

                        FileOutputStream(pdfFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            var total = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                total += read
                                if (contentLength > 0) {
                                    val percent = ((total * 100) / contentLength).toInt().coerceIn(0, 100)
                                    _downloadProgress.value = percent
                                } else {
                                    _downloadProgress.value = -1
                                }
                            }
                            output.flush()
                        }
                        pdfUri = Uri.fromFile(pdfFile)
                    }
                }

                // Post-download: publish result (StateFlow is thread-safe; update directly)
                _downloadUri.value = pdfUri?.toString()
                _downloadProgress.value = null
                _downloadError.value = null
            } catch (e: Exception) {
                _downloadError.value = e.message ?: e.javaClass.simpleName
                _downloadProgress.value = null
            }
        }
    }

    fun clearDownloadUri() {
        _downloadUri.value = null
    }

    fun savePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.savePost(post) }
    }

    fun unsavePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.unsavePost(postId) }
    }

    private val connectivityManager by lazy {
        getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // ignore: if registration fails, keep default true
        }
    }

    override fun onCleared() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        ttsManager.stop()
        super.onCleared()
    }
}
