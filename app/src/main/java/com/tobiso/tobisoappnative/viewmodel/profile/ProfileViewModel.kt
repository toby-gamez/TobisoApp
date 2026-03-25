package com.tobiso.tobisoappnative.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val postsRepo: PostsRepository,
    private val offlineRepo: OfflineRepositoryImpl
) : AndroidViewModel(application) {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _postLoading = MutableStateFlow(false)
    val postLoading: StateFlow<Boolean> = _postLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _offlineDownloading = MutableStateFlow(false)
    val offlineDownloading: StateFlow<Boolean> = _offlineDownloading

    private val _offlineDownloadProgress = MutableStateFlow(0f)
    val offlineDownloadProgress: StateFlow<Float> = _offlineDownloadProgress

    fun loadPosts(categoryId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _postLoading.value = true
            postsRepo.getPostsByCategory(categoryId).fold(
                onSuccess = { posts ->
                    _posts.value = posts
                    _isOffline.value = false
                },
                onFailure = { e ->
                    _isOffline.value = e is IllegalStateException
                }
            )
            _postLoading.value = false
        }
    }

    fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineDownloading.value = true
            offlineRepo.downloadAllData { progress -> _offlineDownloadProgress.value = progress }
            _offlineDownloading.value = false
        }
    }

    private val _copiedImagePath = MutableStateFlow<String?>(null)
    val copiedImagePath: StateFlow<String?> = _copiedImagePath

    /**
     * Copy provided image Uri into the app's internal storage on IO dispatcher
     * and publish the resulting absolute path to [copiedImagePath].
     */
    fun copyImageToInternalStorage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val input: InputStream? = context.contentResolver.openInputStream(uri)
                val fileName = "profile_image.jpg"
                val file = File(context.filesDir, fileName)

                input?.use { ins ->
                    FileOutputStream(file).use { out ->
                        ins.copyTo(out)
                    }
                }

                _copiedImagePath.value = file.absolutePath
            } catch (e: Exception) {
                _copiedImagePath.value = null
            }
        }
    }

    fun clearCopiedImagePath() {
        _copiedImagePath.value = null
    }
}
