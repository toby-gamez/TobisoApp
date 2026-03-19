package com.example.tobisoappnative.viewmodel.allquestions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.repository.OfflineRepositoryImpl
import com.example.tobisoappnative.repository.PostsRepository
import com.example.tobisoappnative.repository.QuestionsRepository
import com.example.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllQuestionsViewModel @Inject constructor(
    application: Application,
    private val questionsRepo: QuestionsRepository,
    private val postsRepo: PostsRepository,
    private val offlineRepo: OfflineRepositoryImpl
) : AndroidViewModel(application) {

    private val _allQuestions = MutableStateFlow<List<Question>>(emptyList())
    val allQuestions: StateFlow<List<Question>> = _allQuestions

    private val _filteredQuestions = MutableStateFlow<List<Question>>(emptyList())
    val filteredQuestions: StateFlow<List<Question>> = _filteredQuestions

    private val _questionsPosts = MutableStateFlow<List<Post>>(emptyList())
    val questionsPosts: StateFlow<List<Post>> = _questionsPosts

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _allQuestionsError = MutableStateFlow<String?>(null)
    val allQuestionsError: StateFlow<String?> = _allQuestionsError

    private val _allQuestionsLoading = MutableStateFlow(false)
    val allQuestionsLoading: StateFlow<Boolean> = _allQuestionsLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _offlineDownloading = MutableStateFlow(false)
    val offlineDownloading: StateFlow<Boolean> = _offlineDownloading

    private val _offlineProgress = MutableStateFlow(0f)
    val offlineDownloadProgress: StateFlow<Float> = _offlineProgress

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId

    private val _selectedPostId = MutableStateFlow<Int?>(null)
    val selectedPostId: StateFlow<Int?> = _selectedPostId

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            postsRepo.getCategories().onSuccess { _categories.value = it }
        }
    }

    fun loadAllQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _allQuestionsLoading.value = true
            _isOffline.value = !NetworkUtils.isOnline(getApplication())
            questionsRepo.getAllQuestions().fold(
                onSuccess = { (questions, posts) ->
                    _allQuestions.value = questions
                    _questionsPosts.value = posts
                    _allQuestionsError.value = if (questions.isEmpty() && _isOffline.value)
                        "Otázky nejsou dostupné v offline režimu" else null
                    applyFilter()
                },
                onFailure = { e ->
                    _allQuestionsError.value = e.message
                }
            )
            _allQuestionsLoading.value = false
        }
    }

    fun setQuestionsFilter(categoryId: Int? = null, postId: Int? = null) {
        _selectedCategoryId.value = categoryId
        _selectedPostId.value = postId
        applyFilter()
    }

    fun clearQuestionsFilter() {
        _selectedCategoryId.value = null
        _selectedPostId.value = null
        applyFilter()
    }

    fun clearAllQuestions() {
        _allQuestions.value = emptyList()
        _filteredQuestions.value = emptyList()
        _questionsPosts.value = emptyList()
        _allQuestionsError.value = null
        _selectedCategoryId.value = null
        _selectedPostId.value = null
    }

    fun downloadAllOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineDownloading.value = true
            offlineRepo.downloadAllData { progress -> _offlineProgress.value = progress }
            _offlineDownloading.value = false
        }
    }

    private fun applyFilter() {
        val all = _allQuestions.value
        val posts = _questionsPosts.value
        val cats = _categories.value
        val catId = _selectedCategoryId.value
        val postId = _selectedPostId.value
        _filteredQuestions.value = when {
            postId != null -> all.filter { it.postId == postId }
            catId != null -> {
                val relevantCatIds = getAllSubcategoryIds(catId, cats)
                val relevantPostIds = posts.filter { it.categoryId in relevantCatIds }.map { it.id }.toSet()
                all.filter { it.postId in relevantPostIds }
            }
            else -> all
        }
    }

    private fun getAllSubcategoryIds(categoryId: Int, categories: List<Category>): Set<Int> {
        val result = mutableSetOf(categoryId)
        categories.filter { it.parentId == categoryId }
            .forEach { result.addAll(getAllSubcategoryIds(it.id, categories)) }
        return result
    }
}
