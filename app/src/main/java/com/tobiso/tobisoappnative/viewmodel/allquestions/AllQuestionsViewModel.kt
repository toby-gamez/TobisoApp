package com.tobiso.tobisoappnative.viewmodel.allquestions

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.QuestionProgressManager
import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.repository.ExerciseRepository
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
    private val exerciseRepo: ExerciseRepository,
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

    // categoryId → list of question IDs; recomputed whenever questions or categories change
    private val _categoryQuestionIdsMap = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())
    val categoryQuestionIdsMap: StateFlow<Map<Int, List<Int>>> = _categoryQuestionIdsMap

    // categoryId → exercises for that category
    private val _categoryExercisesMap = MutableStateFlow<Map<Int, List<InteractiveExerciseResponse>>>(emptyMap())
    val categoryExercisesMap: StateFlow<Map<Int, List<InteractiveExerciseResponse>>> = _categoryExercisesMap

    // all active exercises, shown in the top-level exercises section
    private val _allExercises = MutableStateFlow<List<InteractiveExerciseResponse>>(emptyList())
    val allExercises: StateFlow<List<InteractiveExerciseResponse>> = _allExercises

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            postsRepo.getCategories().onSuccess { cats ->
                _categories.value = cats
                recomputeCategoryMap(_allQuestions.value, _questionsPosts.value, cats)
                computeWeakCategories(cats, _categoryQuestionIdsMap.value)
            }
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
                    val cats = _categories.value
                    recomputeCategoryMap(questions, posts, cats)
                    computeWeakCategories(cats, _categoryQuestionIdsMap.value)
                    val exercises = exerciseRepo.getAllExercises(posts.map { it.id })
                    _allExercises.value = exercises.filter { it.isActive != false }
                    buildCategoryExercisesMap(exercises, posts)
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

    fun getCategoryQuestionIds(categoryId: Int): List<Int> =
        _categoryQuestionIdsMap.value[categoryId] ?: emptyList()

    fun getRandomQuestionIds(count: Int): List<Int> {
        return _allQuestions.value.map { it.id }.shuffled().take(count)
    }

    fun getRandomExercise(): InteractiveExerciseResponse? =
        _allExercises.value.filter { it.type != "circuit" }.randomOrNull()

    fun getDailyQuestionIds(count: Int = 20): List<Int> {
        val seed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().toEpochDay()
        } else {
            System.currentTimeMillis() / 86_400_000L
        }
        return _allQuestions.value.map { it.id }.shuffled(java.util.Random(seed)).take(count)
    }

    private fun buildCategoryExercisesMap(
        exercises: List<InteractiveExerciseResponse>,
        posts: List<Post> = emptyList()
    ) {
        val postCategoryMap = posts.mapNotNull { p -> p.categoryId?.let { p.id to it } }.toMap()
        val map = mutableMapOf<Int, MutableList<InteractiveExerciseResponse>>()
        exercises.filter { it.isActive != false }.forEach { ex ->
            // Prefer direct categoryIds; fall back to deriving category from postIds
            val catIds = ex.categoryIds?.toSet()?.takeIf { it.isNotEmpty() }
                ?: ex.postIds?.mapNotNull { postCategoryMap[it] }?.toSet()
                ?: emptySet()
            catIds.forEach { catId -> map.getOrPut(catId) { mutableListOf() }.add(ex) }
        }
        _categoryExercisesMap.value = map
    }

    private fun recomputeCategoryMap(
        questions: List<Question>,
        posts: List<Post>,
        cats: List<Category>
    ) {
        if (questions.isEmpty() || cats.isEmpty()) return
        // Build postId→categoryId lookup from whichever posts have categoryId set
        val postCategoryMap = posts.mapNotNull { p -> p.categoryId?.let { p.id to it } }.toMap()
        val map = mutableMapOf<Int, MutableList<Int>>()
        for (cat in cats) {
            val catIds = getAllSubcategoryIds(cat.id, cats)
            val postIds = postCategoryMap.entries
                .filter { it.value in catIds }
                .map { it.key }
                .toSet()
            val qIds = questions.filter { it.postId in postIds }.map { it.id }
            if (qIds.isNotEmpty()) map[cat.id] = qIds.toMutableList()
        }
        _categoryQuestionIdsMap.value = map
    }

    private fun computeWeakCategories(
        cats: List<Category>,
        catQuestionMap: Map<Int, List<Int>>
    ) {
        val rootCats = cats.filter { it.parentId == null }
        val weak = rootCats.mapNotNull { cat ->
            val questionIds = (catQuestionMap[cat.id] ?: return@mapNotNull null).toSet()
            val progress = QuestionProgressManager.instance.getProgressForQuestions(questionIds)
            if (progress < 0f) return@mapNotNull null
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
