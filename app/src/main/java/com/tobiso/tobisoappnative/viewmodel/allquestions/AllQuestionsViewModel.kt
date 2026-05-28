package com.tobiso.tobisoappnative.viewmodel.allquestions

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.QuestionProgressManager
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import com.tobiso.tobisoappnative.repository.QuestionsRepository
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WeakCategory(
    val categoryId: Int,
    val name: String,
    val percentage: Float,
    val questionCount: Int
)

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

    private val _weakCategories = MutableStateFlow<List<WeakCategory>>(emptyList())
    val weakCategories: StateFlow<List<WeakCategory>> = _weakCategories

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
                    computeWeakCategories(questions, posts, _categories.value)
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

    fun getCategoryQuestionIds(categoryId: Int): List<Int> {
        val posts = _questionsPosts.value
        val questions = _allQuestions.value
        val cats = _categories.value
        val catIds = getAllSubcategoryIds(categoryId, cats)
        val postIds = posts.filter { it.categoryId in catIds }.map { it.id }.toSet()
        return questions.filter { it.postId in postIds }.map { it.id }
    }

    fun getRandomQuestionIds(count: Int): List<Int> {
        return _allQuestions.value.map { it.id }.shuffled().take(count)
    }

    fun getDailyQuestionIds(count: Int = 20): List<Int> {
        val seed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().toEpochDay()
        } else {
            System.currentTimeMillis() / 86_400_000L
        }
        return _allQuestions.value.map { it.id }.shuffled(java.util.Random(seed)).take(count)
    }

    private fun computeWeakCategories(
        questions: List<Question>,
        posts: List<Post>,
        cats: List<Category>
    ) {
        val rootCats = cats.filter { it.parentId == null }
        val weak = rootCats.mapNotNull { cat ->
            val catIds = getAllSubcategoryIds(cat.id, cats)
            val postIds = posts.filter { it.categoryId in catIds }.map { it.id }.toSet()
            val questionIds = questions.filter { it.postId in postIds }.map { it.id }.toSet()
            if (questionIds.isEmpty()) return@mapNotNull null
            val progress = QuestionProgressManager.instance.getProgressForQuestions(questionIds)
            if (progress < 0f) return@mapNotNull null // no attempts yet
            WeakCategory(
                categoryId = cat.id,
                name = cat.name,
                percentage = progress,
                questionCount = questionIds.size
            )
        }.filter { it.percentage < 0.75f }
            .sortedBy { it.percentage }
            .take(3)
        _weakCategories.value = weak
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
