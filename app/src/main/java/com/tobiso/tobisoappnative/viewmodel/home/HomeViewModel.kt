package com.tobiso.tobisoappnative.viewmodel.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseAndroidViewModel
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.tobiso.tobisoappnative.utils.parseDateToMillis
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Category
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val postsRepo: PostsRepository,
    private val offlineRepo: OfflineRepositoryImpl
) : BaseAndroidViewModel<HomeState, HomeIntent, HomeEffect>(application, HomeState()) {

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load()
            HomeIntent.DownloadAllOfflineData -> downloadAllOfflineData()
        }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true) }
            postsRepo.getCategories().fold(
                onSuccess = { cats -> setState { copy(categories = cats, isOffline = false) } },
                onFailure = { e -> setState { copy(isOffline = true, error = e.message) } }
            )
            postsRepo.getPostsByCategory(null).fold(
                onSuccess = { posts -> setState { copy(posts = posts) } },
                onFailure = { /* categories error already set */ }
            )
            // compute newest list after loading
            computeNewest(null)
            setState { copy(isLoading = false) }
        }
    }

    private val _newestPosts = MutableStateFlow<List<Post>>(emptyList())
    val newestPosts: StateFlow<List<Post>> = _newestPosts

    // Public: request recompute on background dispatcher. Provide a selected root subject id or null (all).
    fun computeNewest(selectedSubjectId: Int?) {
        viewModelScope.launch(Dispatchers.Default) {
            val categories = uiState.value.categories
            val posts = uiState.value.posts

            if (posts.isEmpty() || categories.isEmpty()) {
                _newestPosts.value = emptyList()
                return@launch
            }

            val categoryMap: Map<Int, Category> = categories.associateBy { it.id }
            val parentMap: Map<Int?, List<Category>> = categories.groupBy { it.parentId }
            val descendantsCache = mutableMapOf<Int, Set<Int>>()

            fun getDescendants(rootId: Int): Set<Int> {
                return descendantsCache.getOrPut(rootId) {
                    val result = mutableSetOf<Int>()
                    fun dfs(id: Int) {
                        result.add(id)
                        val children = parentMap[id] ?: emptyList()
                        children.forEach { dfs(it.id) }
                    }
                    dfs(rootId)
                    result
                }
            }

            val lastEditMap = posts.associate { it.id to parseDateToMillis(it.lastEdit) }
            val lastFixMap = posts.associate { it.id to parseDateToMillis(it.lastFix) }

            val baseFiltered = posts.filter { p ->
                p.categoryId != null && categoryMap[p.categoryId]?.name != "More"
            }

            val subjectFiltered = selectedSubjectId?.let { sid ->
                val ids = getDescendants(sid)
                baseFiltered.filter { p -> p.categoryId != null && ids.contains(p.categoryId) }
            } ?: baseFiltered

            val withEdit = subjectFiltered.filter { !it.lastEdit.isNullOrBlank() }
                .sortedByDescending { lastEditMap[it.id] ?: Long.MIN_VALUE }

            val remaining = subjectFiltered.filter { it.lastEdit.isNullOrBlank() && !it.lastFix.isNullOrBlank() }
                .sortedByDescending { lastFixMap[it.id] ?: Long.MIN_VALUE }

            val others = subjectFiltered.filter { it.lastEdit.isNullOrBlank() && it.lastFix.isNullOrBlank() }

            val result = (withEdit + remaining + others).distinctBy { it.id }
            _newestPosts.value = result
        }
    }

    private fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(offlineDownloading = true) }
            val success = offlineRepo.downloadAllData { progress ->
                setState { copy(offlineDownloadProgress = progress) }
            }
            setState { copy(offlineDownloading = false) }
            if (success) {
                emitEffect(HomeEffect.ShowToast("Offline obsah byl aktualizován"))
                load()
            }
        }
    }
}
