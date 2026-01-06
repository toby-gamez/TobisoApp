package com.example.tobisoappnative.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tobisoappnative.model.Addendum
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Category
import com.example.tobisoappnative.model.Post
import okhttp3.ResponseBody
import com.example.tobisoappnative.model.Question
import com.example.tobisoappnative.model.RelatedPost
import com.example.tobisoappnative.model.Snippet
import com.example.tobisoappnative.model.Explanation
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.utils.NetworkUtils
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import com.example.tobisoappnative.tts.TtsManager
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore(name = "saved_posts")

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _categoryError = MutableStateFlow<String?>(null)
    val categoryError: StateFlow<String?> = _categoryError
    
    private val _categoryLoading = MutableStateFlow(false)
    val categoryLoading: StateFlow<Boolean> = _categoryLoading

    private val _postError = MutableStateFlow<String?>(null)
    val postError: StateFlow<String?> = _postError
    
    private val _postLoading = MutableStateFlow(false)
    val postLoading: StateFlow<Boolean> = _postLoading

    private val _postDetail = MutableStateFlow<Post?>(null)
    val postDetail: StateFlow<Post?> = _postDetail
    private val _postDetailError = MutableStateFlow<String?>(null)
    val postDetailError: StateFlow<String?> = _postDetailError

    // Questions state
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions
    private val _questionsError = MutableStateFlow<String?>(null)
    val questionsError: StateFlow<String?> = _questionsError
    private val _questionsLoading = MutableStateFlow(false)
    val questionsLoading: StateFlow<Boolean> = _questionsLoading

    // All Questions state
    private val _allQuestions = MutableStateFlow<List<Question>>(emptyList())
    val allQuestions: StateFlow<List<Question>> = _allQuestions
    private val _allQuestionsError = MutableStateFlow<String?>(null)
    val allQuestionsError: StateFlow<String?> = _allQuestionsError
    private val _allQuestionsLoading = MutableStateFlow(false)
    val allQuestionsLoading: StateFlow<Boolean> = _allQuestionsLoading

    // Filtered Questions state
    private val _filteredQuestions = MutableStateFlow<List<Question>>(emptyList())
    val filteredQuestions: StateFlow<List<Question>> = _filteredQuestions
    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId
    private val _selectedPostId = MutableStateFlow<Int?>(null)
    val selectedPostId: StateFlow<Int?> = _selectedPostId

    // Questions Posts mapping
    private val _questionsPosts = MutableStateFlow<List<Post>>(emptyList())
    val questionsPosts: StateFlow<List<Post>> = _questionsPosts

    // Related posts state
    private val _relatedPosts = MutableStateFlow<List<RelatedPost>>(emptyList())
    val relatedPosts: StateFlow<List<RelatedPost>> = _relatedPosts
    private val _relatedPostsError = MutableStateFlow<String?>(null)
    val relatedPostsError: StateFlow<String?> = _relatedPostsError
    private val _relatedPostsLoading = MutableStateFlow(false)
    val relatedPostsLoading: StateFlow<Boolean> = _relatedPostsLoading

    // Addendums state
    private val _addendums = MutableStateFlow<List<Addendum>>(emptyList())
    val addendums: StateFlow<List<Addendum>> = _addendums
    private val _addendumsError = MutableStateFlow<String?>(null)
    val addendumsError: StateFlow<String?> = _addendumsError
    private val _addendumsLoading = MutableStateFlow(false)
    val addendumsLoading: StateFlow<Boolean> = _addendumsLoading

    // Toast systém
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage
    
    // Offline režim
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline
    
    // Stav pro ruční opuštění "No Internet" obrazovky
    private val _hasUserDismissedNoInternet = MutableStateFlow(false)
    val hasUserDismissedNoInternet: StateFlow<Boolean> = _hasUserDismissedNoInternet

    // Search bar expanded/collapsed state (persisted in ViewModel so UI remembers when navigated away)
    private val _searchBarExpanded = MutableStateFlow(true)
    val searchBarExpanded: StateFlow<Boolean> = _searchBarExpanded

    fun setSearchBarExpanded(expanded: Boolean) {
        _searchBarExpanded.value = expanded
    }
    
    // Flag pro sledování prvního načtení
    private var isFirstLoad = true
    
    private val offlineDataManager = OfflineDataManager(application)
    private val dataStore = application.dataStore
    private val FAVORITE_POSTS_KEY = stringSetPreferencesKey("favorite_posts_json")
    private val gson = Gson()
    
    // TTS Manager
    private val _ttsManager = MutableStateFlow<TtsManager?>(null)
    val ttsManager: StateFlow<TtsManager?> = _ttsManager

    private val _favoritePosts = MutableStateFlow<List<Post>>(emptyList())
    val favoritePosts: StateFlow<List<Post>> = _favoritePosts

    private val _snippets = MutableStateFlow<List<Snippet>>(emptyList())
    val snippets: StateFlow<List<Snippet>> = _snippets
    private val SNIPPETS_FILE_NAME = "snippets.json"
    // Track last clipboard text that was already handled (saved or dismissed) so we
    // don't repeatedly prompt the user for the same clipboard content across screens
    private val _lastHandledClipboard = MutableStateFlow<String?>(null)
    val lastHandledClipboard: StateFlow<String?> = _lastHandledClipboard

    fun markClipboardHandled(text: String) {
        _lastHandledClipboard.value = text
    }
    // New: offline download state and helpers (class-level)
    private val _offlineDownloading = MutableStateFlow(false)
    val offlineDownloading: StateFlow<Boolean> = _offlineDownloading
    private val _offlineDownloadProgress = MutableStateFlow(0f)
    val offlineDownloadProgress: StateFlow<Float> = _offlineDownloadProgress

    init {
        // Inicializace offline stavu na začátku
        viewModelScope.launch(Dispatchers.IO) {
            val isOnline = NetworkUtils.isOnline(getApplication())
            _isOffline.value = !isOnline
            println("DEBUG: App initialized - Online: $isOnline, Offline: ${!isOnline}")

            // Automatické stahování offline dat pokud jsou starší než 15 min
            try {
                if (!offlineDataManager.isCacheFresh(15)) {
                    downloadAllOfflineData(getApplication())
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Failed checking cache freshness: ${e.message}")
            }
        }

        // Inicializace TTS manageru
        initializeTts()

        viewModelScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { prefs ->
                    val jsonSet = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
                    jsonSet.mapNotNull { json ->
                        try {
                            gson.fromJson(json, Post::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                .collect { posts ->
                    _favoritePosts.value = posts
                }
        }
    }

    fun savePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
                // Pokud už je post uložen, nepřidávej znovu
                val alreadySaved = current.any { json ->
                    try {
                        gson.fromJson(json, Post::class.java).id == post.id
                    } catch (e: Exception) {
                        false
                    }
                }
                if (!alreadySaved) {
                    prefs[FAVORITE_POSTS_KEY] = current + gson.toJson(post)
                }
            }
        }
    }

    fun unsavePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                val current = prefs[FAVORITE_POSTS_KEY] ?: emptySet()
                val newSet = current.filterNot { json ->
                    try {
                        gson.fromJson(json, Post::class.java).id == postId
                    } catch (e: Exception) {
                        false
                    }
                }.toSet()
                prefs[FAVORITE_POSTS_KEY] = newSet
            }
        }
    }

    // Toast funkce
    fun showToast(message: String) {
        _toastMessage.value = message
    }

    /**
     * Spustí stahování všech offline dat na pozadí a aktualizuje progress
     */
    fun downloadAllOfflineData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _offlineDownloading.value = true
            _offlineDownloadProgress.value = 0f
            try {
                // 1. Kategorie
                val categoriesArray = ApiClient.apiService.getCategories()
                _offlineDownloadProgress.value = 0.2f

                // 2. Články
                val postsArray = ApiClient.apiService.getPosts()
                _offlineDownloadProgress.value = 0.4f

                // 3. Otázky
                val questionsArray = ApiClient.apiService.getAllQuestions()
                _offlineDownloadProgress.value = 0.5f

                // 4. Posty pro otázky
                val questionsPostsArray = ApiClient.apiService.getPostsForQuestions()
                _offlineDownloadProgress.value = 0.65f

                // 5. Související články
                val relatedPostsArray = ApiClient.apiService.getAllRelatedPosts()
                _offlineDownloadProgress.value = 0.8f

                // 6. Dodatky
                val addendumsArray = ApiClient.apiService.getAddendums()
                _offlineDownloadProgress.value = 0.9f

                // Uložení všeho do offline cache
                offlineDataManager.saveCategoriesPostsAndQuestions(
                    categoriesArray.toList(),
                    postsArray.toList(),
                    questionsArray.toList(),
                    questionsPostsArray.toList(),
                    relatedPostsArray.toList(),
                    addendumsArray.toList()
                )

                // Aktualizuj in-memory stav
                _categories.value = categoriesArray.toList()
                _posts.value = postsArray.toList()
                _addendums.value = addendumsArray.toList()

                _offlineDownloadProgress.value = 1f
                showToast("Offline obsah byl aktualizován")
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Offline download failed: ${e.message}")
            } finally {
                _offlineDownloading.value = false
            }
        }
    }
    
    fun clearToast() {
        _toastMessage.value = null
    }

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            _categoryLoading.value = true
            val isOnline = NetworkUtils.isOnline(getApplication())
            
                // Pokud jsou offline data čerstvá (do 15 minut), načti je pro urychlení
                try {
                    if (offlineDataManager.isCacheFresh(15)) {
                        println("DEBUG: Using fresh offline cache for categories (<=15min). Skipping API call.")
                        loadOfflineData()
                        _categoryLoading.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    // Pokud něco selže při čtení cache, pokračujeme běžně
                    android.util.Log.w("MainViewModel", "Error checking offline cache freshness: ${e.message}")
                }

                if (isOnline) {
                try {
                    // Online - načti z API a ulož offline
                    val categoriesArray = ApiClient.apiService.getCategories()
                    val postsArray = ApiClient.apiService.getPosts()
                    
                    // Konvertuj na List
                    val categories = categoriesArray.toList()
                    val posts = postsArray.toList()
                    
                    // Pokus se načíst i otázky, related posts a addendums pro offline cache
                    try {
                        val questionsArray = ApiClient.apiService.getAllQuestions()
                        val questionsPostsArray = ApiClient.apiService.getPosts()
                        val relatedPostsArray = ApiClient.apiService.getAllRelatedPosts()
                        val addendumsArray = ApiClient.apiService.getAddendums()
                        val questions = questionsArray.toList()
                        val questionsPosts = questionsPostsArray.toList()
                        val relatedPosts = relatedPostsArray.toList()
                        val addendums = addendumsArray.toList()
                        
                        // Ulož vše do offline cache včetně otázek, related posts a addendums
                        offlineDataManager.saveCategoriesPostsAndQuestions(categories, posts, questions, questionsPosts, relatedPosts, addendums)
                        println("DEBUG: Saved offline data with questions, related posts and addendums - Questions: ${questions.size}, Related Posts: ${relatedPosts.size}, Addendums: ${addendums.size}")
                    } catch (e: Exception) {
                        // Pokud se nepodaří načíst otázky/related posts/addendums, ulož alespoň kategorie a posty
                        offlineDataManager.saveCategoriesAndPosts(categories, posts)
                        println("DEBUG: Failed to load questions/related posts/addendums for offline cache: ${e.message}")
                    }
                    
                    _categories.value = categories
                    _posts.value = posts
                    _categoryError.value = null
                    _postError.value = null
                    _isOffline.value = false
                    
                    // Zobraz toast pouze při prvním úspěšném načtení online
                    if (isFirstLoad) {
                        showToast("Offline obsah byl aktualizován")
                        isFirstLoad = false
                    }
                } catch (e: Exception) {
                    // Znovu zkontroluj připojení - možná se mezitím ztratilo
                    val stillOnline = NetworkUtils.isOnline(getApplication())
                    if (stillOnline) {
                        // Online ale API nefunguje - zobraz chybu API, ne offline režim
                        _categories.value = emptyList()
                        _posts.value = emptyList()
                        _categoryError.value = "Chyba serveru: ${e.message}"
                        _postError.value = "Chyba serveru: ${e.message}"
                        _isOffline.value = false // Zůstáváme online!
                        showToast("Problém s připojením k serveru. Zkuste to později.")
                    } else {
                        // Mezitím se skutečně ztratilo připojení
                        loadOfflineData()
                    }
                }
            } else {
                // Offline - načti z cache
                loadOfflineData()
            }
            _categoryLoading.value = false
        }
    }
    
    /**
     * Načítání dat z offline cache
     */
    private suspend fun loadOfflineData() {
        val cachedCategories = offlineDataManager.getCachedCategories()
        val cachedPosts = offlineDataManager.getCachedPosts()
        
            if (cachedCategories != null && cachedPosts != null) {
            _categories.value = cachedCategories
            _posts.value = cachedPosts
            _categoryError.value = null
            _postError.value = null
            _isOffline.value = true
            println("DEBUG: Loaded offline data - Categories: ${cachedCategories.size}, Posts: ${cachedPosts.size}")
        } else {
            _categories.value = emptyList()
            _posts.value = emptyList()
            _categoryError.value = "Žádná offline data k dispozici"
            _postError.value = "Žádná offline data k dispozici"
            _isOffline.value = true
                showToast("Žádná offline data. Připojte se k internetu.")
        }
    }
    
    /**
     * Spustí offline režim ručně
     */
    fun enableOfflineMode() {
        viewModelScope.launch(Dispatchers.IO) {
            loadOfflineData()
        }
    }
    
    /**
     * Potvrdí přechod do offline režimu a skryje NoInternetScreen
     */
    fun confirmOfflineModeTransition() {
        _hasUserDismissedNoInternet.value = true
    }
    
    /**
     * Resetuje stav dismissu (např. při obnovení připojení)
     */
    fun resetNoInternetDismiss() {
        _hasUserDismissedNoInternet.value = false
    }
    
    /**
     * Aktualizuje network stav
     */
    fun refreshNetworkState() {
        viewModelScope.launch(Dispatchers.IO) {
            val isOnline = NetworkUtils.isOnline(getApplication())
            val wasOffline = _isOffline.value
            _isOffline.value = !isOnline
            println("DEBUG: Network state refreshed - Online: $isOnline, Was offline: $wasOffline, Now offline: ${!isOnline}")
        }
    }

    fun loadPosts(categoryId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _postLoading.value = true
            try {
                // Pokud nejsme offline, ale cache je čerstvá, použij cache pro rychlé načtení
                if (!_isOffline.value) {
                    try {
                        if (offlineDataManager.isCacheFresh(15)) {
                            println("DEBUG: Using fresh offline cache for posts (<=15min). Skipping API call.")
                            val posts = if (categoryId != null) {
                                offlineDataManager.getCachedPostsByCategory(categoryId) ?: emptyList()
                            } else {
                                offlineDataManager.getCachedPosts() ?: emptyList()
                            }
                            _posts.value = posts
                            _postError.value = null
                            _postLoading.value = false
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MainViewModel", "Error checking offline cache freshness for posts: ${e.message}")
                    }
                }

                // V offline režimu používáme cached data
                if (_isOffline.value) {
                    val posts = if (categoryId != null) {
                        offlineDataManager.getCachedPostsByCategory(categoryId) ?: emptyList()
                    } else {
                        offlineDataManager.getCachedPosts() ?: emptyList()
                    }
                    _posts.value = posts
                    _postError.value = null
                    println("DEBUG: Loaded offline posts - Category: $categoryId, Posts: ${posts.size}")
                } else {
                    // Online režim - načítáme z API
                    val postsArray = ApiClient.apiService.getPosts(categoryId)
                    _posts.value = postsArray.toList()
                    _postError.value = null
                }
            } catch (e: Throwable) {
                // Znovu zkontroluj připojení při chybě
                val isOnline = NetworkUtils.isOnline(getApplication())
                if (isOnline) {
                    // Online ale API nefunguje
                    _posts.value = emptyList()
                    _postError.value = "Chyba serveru: ${e.message}"
                    showToast("Problém s připojením k serveru")
                } else {
                    // Skutečně offline - zkus cached data
                    val posts = if (categoryId != null) {
                        offlineDataManager.getCachedPostsByCategory(categoryId) ?: emptyList()
                    } else {
                        offlineDataManager.getCachedPosts() ?: emptyList()
                    }
                    if (posts.isNotEmpty()) {
                        _posts.value = posts
                        _postError.value = null
                        _isOffline.value = true
                        showToast("Přepnuto do offline režimu")
                    } else {
                        _posts.value = emptyList()
                        _postError.value = "Žádná offline data k dispozici"
                    }
                }
            }
            _postLoading.value = false
        }
    }

    fun loadPostDetail(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Nejprve zkontrolujeme aktuální network stav
                refreshNetworkState()
                
                // V offline režimu používáme cached data
                if (_isOffline.value) {
                    val post = offlineDataManager.getCachedPost(postId)
                    _postDetail.value = post
                    _postDetailError.value = if (post == null) "Post nebyl nalezen v offline datech" else null
                    println("DEBUG: Loaded offline post detail - Post ID: $postId, Found: ${post != null}")
                } else {
                    // Online režim - načítáme z API
                    val post = ApiClient.apiService.getPost(postId)
                    _postDetail.value = post
                    _postDetailError.value = null
                    println("DEBUG: Loaded online post detail - Post ID: $postId")
                }
            } catch (e: Throwable) {
                // Znovu zkontroluj připojení při chybě
                val isOnline = NetworkUtils.isOnline(getApplication())
                if (isOnline) {
                    // Online ale API nefunguje
                    _postDetail.value = null
                    _postDetailError.value = "Chyba serveru: ${e.message}"
                    showToast("Problém s připojením k serveru")
                } else {
                    // Skutečně offline - zkus cached data jako fallback
                    val post = offlineDataManager.getCachedPost(postId)
                    if (post != null) {
                        _postDetail.value = post
                        _postDetailError.value = null
                        _isOffline.value = true
                        showToast("Přepnuto do offline režimu")
                        println("DEBUG: API failed, using cached post detail - Post ID: $postId")
                    } else {
                        _postDetail.value = null
                        _postDetailError.value = "Post nebyl nalezen ani v offline datech"
                        println("DEBUG: Failed to load post detail - Post ID: $postId, Error: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Fetch a single post from the API by id and append it to the posts cache.
     * Best-effort; failure is logged but doesn't crash the UI.
     */
    fun fetchAndCachePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If we already cached it, nothing to do
                if (_posts.value.any { it.id == postId }) return@launch
                val post = ApiClient.apiService.getPost(postId)
                val list = _posts.value.toMutableList()
                list.add(post)
                _posts.value = list
                println("DEBUG: fetchAndCachePost cached post id=$postId title=${post.title}")
            } catch (e: Exception) {
                println("DEBUG: fetchAndCachePost failed for id=$postId: ${e.message}")
            }
        }
    }

    fun loadSnippets() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, SNIPPETS_FILE_NAME)
            if (!file.exists()) {
                _snippets.value = emptyList()
                return@launch
            }
            try {
                val json = file.readText()
                val loaded = gson.fromJson(json, Array<Snippet>::class.java)?.toList() ?: emptyList()
                _snippets.value = loaded
            } catch (e: Exception) {
                _snippets.value = emptyList()
            }
        }
    }

    fun addSnippet(snippet: Snippet) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, SNIPPETS_FILE_NAME)
            val current = try {
                val json = file.takeIf { it.exists() }?.readText() ?: "[]"
                gson.fromJson(json, Array<Snippet>::class.java)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf<Snippet>()
            }
            current.add(snippet)
            val json = gson.toJson(current)
            file.writeText(json)
            _snippets.value = current
            // If we have the post detail loaded for this snippet and the global
            // posts cache doesn't contain it, add it so FavoritesScreen can
            // immediately resolve the post title.
            try {
                if (snippet.postId != 0) {
                    val hasPost = _posts.value.any { it.id == snippet.postId }
                    val loadedDetail = _postDetail.value
                    if (!hasPost && loadedDetail != null && loadedDetail.id == snippet.postId) {
                        // Append the post to cached posts so other screens (Favorites)
                        // can find the title without requiring a full reload.
                        val newPosts = _posts.value.toMutableList()
                        newPosts.add(loadedDetail)
                        _posts.value = newPosts
                    }
                }
            } catch (e: Exception) {
                // ignore any errors here; this is a best-effort cache update
            }
        }
    }

    fun removeSnippet(snippet: Snippet) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, SNIPPETS_FILE_NAME)
            val current = try {
                val json = file.takeIf { it.exists() }?.readText() ?: "[]"
                gson.fromJson(json, Array<Snippet>::class.java)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf<Snippet>()
            }
            val newList = current.filterNot {
                it.postId == snippet.postId && it.content == snippet.content && it.createdAt == snippet.createdAt
            }
            val json = gson.toJson(newList)
            file.writeText(json)
            _snippets.value = newList
        }
    }

    fun clearSnippets() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, SNIPPETS_FILE_NAME)
            file.writeText("[]")
            _snippets.value = emptyList()
        }
    }

    fun clearFavoritePosts() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[FAVORITE_POSTS_KEY] = emptySet()
            }
            _favoritePosts.value = emptyList()
        }
    }

    fun loadQuestions(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _questionsLoading.value = true
            try {
                // Znovu zkontroluj připojení při načítání otázek
                refreshNetworkState()
                
                // V offline režimu používáme cached data
                if (_isOffline.value) {
                    val questions = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
                    _questions.value = questions
                    _questionsError.value = if (questions.isEmpty()) "Otázky pro tento článek nejsou dostupné v offline režimu" else null
                    println("DEBUG: Loaded offline questions - Post ID: $postId, Questions: ${questions.size}")
                } else {
                    // Online režim - načítáme z API
                    val questionsArray = ApiClient.apiService.getQuestionsByPostId(postId)
                    val questions = questionsArray.toList()
                    _questions.value = questions
                    _questionsError.value = null
                    println("DEBUG: Loaded ${questions.size} questions for post $postId")
                    questions.forEachIndexed { index, question ->
                        println("DEBUG: Question $index - text: ${question.questionText}, answers: ${question.answers.size}, correctAnswer: ${question.correctAnswer}")
                        question.answers.forEachIndexed { answerIndex, answer ->
                            println("DEBUG:   Answer $answerIndex: ${answer.answerText} (correct: ${answer.correct})")
                        }
                    }
                }
            } catch (e: Throwable) {
                // Znovu zkontroluj připojení při chybě
                val isOnline = NetworkUtils.isOnline(getApplication())
                if (isOnline) {
                    // Online ale API nefunguje
                    _questions.value = emptyList()
                    _questionsError.value = "Chyba serveru: ${e.message}"
                    showToast("Problém s připojením k serveru")
                } else {
                    // Skutečně offline - zkus cached data jako fallback
                    val questions = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
                    if (questions.isNotEmpty()) {
                        _questions.value = questions
                        _questionsError.value = null
                        _isOffline.value = true
                        showToast("Přepnuto do offline režimu")
                        println("DEBUG: API failed, using cached questions - Post ID: $postId, Questions: ${questions.size}")
                    } else {
                        _questions.value = emptyList()
                        _questionsError.value = "Otázky nebyly nalezeny ani v offline datech"
                        println("DEBUG: Failed to load questions - Post ID: $postId, Error: ${e.message}")
                    }
                }
            } finally {
                _questionsLoading.value = false
            }
        }
    }

    suspend fun checkHasQuestions(postId: Int): Boolean {
        return try {
            // Nejprve zkontrolujeme aktuální network stav
            refreshNetworkState()
            
            if (_isOffline.value) {
                // V offline režimu zkontrolujeme cached data
                val questions = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
                val hasQuestions = questions.isNotEmpty()
                println("DEBUG: Offline mode - checked cached questions for post $postId - Has questions: $hasQuestions")
                return hasQuestions
            }
            
            // Online režim - zkontrolujeme API
            val questionsArray = ApiClient.apiService.getQuestionsByPostId(postId)
            val hasQuestions = questionsArray.isNotEmpty()
            println("DEBUG: Checked questions for post $postId - Has questions: $hasQuestions")
            hasQuestions
        } catch (e: Exception) {
            // Při chybě zkusíme fallback na cached data
            val questions = offlineDataManager.getCachedQuestionsByPostId(postId) ?: emptyList()
            val hasQuestions = questions.isNotEmpty()
            println("DEBUG: Error checking questions for post $postId: ${e.message}, fallback to cached: $hasQuestions")
            hasQuestions
        }
    }

    fun clearQuestions() {
        _questions.value = emptyList()
        _questionsError.value = null
        _questionsLoading.value = false
    }

    fun loadRelatedPosts(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _relatedPostsLoading.value = true
            _relatedPostsError.value = null
            try {
                // Nejprve zkontrolujeme aktuální network stav
                refreshNetworkState()
                
                val relatedPostsList = mutableListOf<RelatedPost>()
                
                // V offline režimu načítáme z offline dat
                if (_isOffline.value) {
                    // Načteme související články z offline cache
                    val cachedRelatedPosts = offlineDataManager.getCachedRelatedPostsByPostId(postId)
                    if (cachedRelatedPosts != null) {
                        relatedPostsList.addAll(cachedRelatedPosts)
                    }
                    
                    // Přidat články ze stejné kategorie
                    val currentPost = _postDetail.value
                    val cachedPosts = offlineDataManager.getCachedPosts()
                    
                    if (currentPost?.categoryId != null && !cachedPosts.isNullOrEmpty() && relatedPostsList.size < 5) {
                        val remainingSlots = 5 - relatedPostsList.size
                        val categoryPosts = cachedPosts
                            .filter { p -> p.categoryId == currentPost.categoryId && p.id != currentPost.id }
                            .take(remainingSlots)
                            .map { p ->
                                RelatedPost(
                                    id = 0,
                                    postId = currentPost.id,
                                    relatedPostId = p.id,
                                    text = "souvisí s tématem",
                                    postTitle = currentPost.title,
                                    relatedPostTitle = p.title
                                )
                            }
                        
                        // Vyfiltrovat články, které už jsou v relatedPostsList
                        val existingIds = relatedPostsList.map { it.relatedPostId }.toSet()
                        val newCategoryPosts = categoryPosts.filter { cp -> !existingIds.contains(cp.relatedPostId) }
                        
                        relatedPostsList.addAll(newCategoryPosts)
                    }
                    
                    _relatedPosts.value = relatedPostsList
                    _relatedPostsError.value = null
                    println("DEBUG: Offline mode - Loaded related posts for post $postId - Count: ${relatedPostsList.size}")
                    return@launch
                }
                
                // Online režim - načítáme z API
                val relatedPostsArray = ApiClient.apiService.getRelatedPostsByPostId(postId)
                relatedPostsList.addAll(relatedPostsArray.toList())
                
                // Přidat články ze stejné kategorie (max 5 celkem)
                val currentPost = _postDetail.value
                if (currentPost?.categoryId != null && _posts.value.isNotEmpty() && relatedPostsList.size < 5) {
                    val remainingSlots = 5 - relatedPostsList.size
                    val categoryPosts = _posts.value
                        .filter { p -> p.categoryId == currentPost.categoryId && p.id != currentPost.id }
                        .take(remainingSlots)
                        .map { p ->
                            RelatedPost(
                                id = 0, // ID není důležité pro zobrazení
                                postId = currentPost.id,
                                relatedPostId = p.id,
                                text = "souvisí s tématem",
                                postTitle = currentPost.title,
                                relatedPostTitle = p.title
                            )
                        }
                    
                    // Vyfiltrovat články, které už jsou v relatedPostsList
                    val existingIds = relatedPostsList.map { it.relatedPostId }.toSet()
                    val newCategoryPosts = categoryPosts.filter { cp -> !existingIds.contains(cp.relatedPostId) }
                    
                    relatedPostsList.addAll(newCategoryPosts)
                }
                
                _relatedPosts.value = relatedPostsList
                _relatedPostsError.value = null
                println("DEBUG: Loaded related posts for post $postId - Count: ${relatedPostsList.size}")
            } catch (e: Exception) {
                _relatedPosts.value = emptyList()
                _relatedPostsError.value = "Chyba při načítání souvisejících článků: ${e.message}"
                println("DEBUG: Error loading related posts for post $postId: ${e.message}")
                e.printStackTrace()
            } finally {
                _relatedPostsLoading.value = false
            }
        }
    }

    fun clearRelatedPosts() {
        _relatedPosts.value = emptyList()
        _relatedPostsError.value = null
    }

    fun loadAllQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _allQuestionsLoading.value = true
            try {
                // Znovu zkontroluj připojení při načítání otázek
                refreshNetworkState()

                // Pokud jsou offline otázky čerstvé (např. uložené v posledních 15 minutách), použij je
                try {
                    if (offlineDataManager.isCacheFresh(15)) {
                        val questions = offlineDataManager.getCachedQuestions() ?: emptyList()
                        val posts = offlineDataManager.getCachedQuestionsPosts() ?: emptyList()

                        _allQuestions.value = questions
                        _questionsPosts.value = posts
                        _allQuestionsError.value = if (questions.isEmpty()) "Otázky nejsou dostupné v offline režimu" else null

                        // Aplikuj aktuální filtr
                        if (questions.isNotEmpty()) {
                            applyQuestionsFilter()
                        }

                        println("DEBUG: Using fresh cached all questions (<=15min) - Questions: ${questions.size}, Posts: ${posts.size}")
                        _allQuestionsLoading.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Error checking offline cache freshness for all questions: ${e.message}")
                    // pokud kontrola cache selže, pokračujeme dál (fallback níže)
                }

                // V offline režimu používáme cached data
                if (_isOffline.value) {
                    val questions = offlineDataManager.getCachedQuestions() ?: emptyList()
                    val posts = offlineDataManager.getCachedQuestionsPosts() ?: emptyList()

                    _allQuestions.value = questions
                    _questionsPosts.value = posts
                    _allQuestionsError.value = if (questions.isEmpty()) "Otázky nejsou dostupné v offline režimu" else null

                    // Aplikuj aktuální filtr
                    if (questions.isNotEmpty()) {
                        applyQuestionsFilter()
                    }

                    println("DEBUG: Loaded offline all questions - Count: ${questions.size}, Posts: ${posts.size}")
                } else {
                    // Online režim - načítáme z API
                    val questionsArray = ApiClient.apiService.getAllQuestions()
                    val postsArray = ApiClient.apiService.getPosts()

                    val questions = questionsArray.toList()
                    val posts = postsArray.toList()

                    _allQuestions.value = questions
                    _questionsPosts.value = posts
                    _allQuestionsError.value = null

                    // Aplikuj aktuální filtr
                    applyQuestionsFilter()

                    println("DEBUG: Loaded all questions - Count: ${questions.size}, Posts: ${posts.size}")
                }
            } catch (e: Exception) {
                // Znovu zkontroluj připojení při chybě
                val isOnline = NetworkUtils.isOnline(getApplication())
                if (isOnline) {
                    // Online ale API nefunguje
                    _allQuestions.value = emptyList()
                    _questionsPosts.value = emptyList()
                    _allQuestionsError.value = "Chyba serveru: ${e.message}"
                    showToast("Problém s připojením k serveru")
                } else {
                    // Skutečně offline - zkus cached data jako fallback
                    val questions = offlineDataManager.getCachedQuestions() ?: emptyList()
                    val posts = offlineDataManager.getCachedQuestionsPosts() ?: emptyList()
                    
                    if (questions.isNotEmpty()) {
                        _allQuestions.value = questions
                        _questionsPosts.value = posts
                        _allQuestionsError.value = null
                        _isOffline.value = true
                        showToast("Přepnuto do offline režimu")
                        
                        // Aplikuj aktuální filtr
                        applyQuestionsFilter()
                        
                        println("DEBUG: API failed, using cached all questions - Questions: ${questions.size}, Posts: ${posts.size}")
                    } else {
                        _allQuestions.value = emptyList()
                        _questionsPosts.value = emptyList()
                        _allQuestionsError.value = "Otázky nebyly nalezeny ani v offline datech"
                        println("DEBUG: Failed to load all questions - Error: ${e.message}")
                    }
                }
            } finally {
                _allQuestionsLoading.value = false
            }
        }
    }

    fun setQuestionsFilter(categoryId: Int? = null, postId: Int? = null) {
        _selectedCategoryId.value = categoryId
        _selectedPostId.value = postId
        applyQuestionsFilter()
    }

    fun clearQuestionsFilter() {
        _selectedCategoryId.value = null
        _selectedPostId.value = null
        applyQuestionsFilter()
    }

    private fun applyQuestionsFilter() {
        val allQuestions = _allQuestions.value
        val posts = _questionsPosts.value
        val categories = _categories.value
        val selectedCategoryId = _selectedCategoryId.value
        val selectedPostId = _selectedPostId.value

        val filtered = when {
            // Filtr podle konkrétního článku
            selectedPostId != null -> {
                allQuestions.filter { it.postId == selectedPostId }
            }
            // Filtr podle kategorie
            selectedCategoryId != null -> {
                // Získej všechny podkategorie
                val relevantCategoryIds = getAllSubcategoryIds(selectedCategoryId, categories)
                // Najdi posty v těchto kategoriích
                val relevantPostIds = posts
                    .filter { post -> post.categoryId in relevantCategoryIds }
                    .map { it.id }
                    .toSet()
                // Filtruj otázky podle postů
                allQuestions.filter { it.postId in relevantPostIds }
            }
            // Žádný filtr
            else -> allQuestions
        }

        _filteredQuestions.value = filtered
    }

    private fun getAllSubcategoryIds(categoryId: Int, categories: List<Category>): Set<Int> {
        val result = mutableSetOf(categoryId)
        val subcategories = categories.filter { it.parentId == categoryId }
        
        for (subcategory in subcategories) {
            result.addAll(getAllSubcategoryIds(subcategory.id, categories))
        }
        
        return result
    }

    fun clearAllQuestions() {
        _allQuestions.value = emptyList()
        _filteredQuestions.value = emptyList()
        _questionsPosts.value = emptyList()
        _allQuestionsError.value = null
        _allQuestionsLoading.value = false
        _selectedCategoryId.value = null
        _selectedPostId.value = null
    }
    
    // TTS Methods
    private fun initializeTts() {
        _ttsManager.value = TtsManager(getApplication())
    }
    
    fun getTtsManager(): TtsManager? {
        return _ttsManager.value
    }
    
    fun speakText(text: String) {
        _ttsManager.value?.speak(text)
    }
    
    fun pauseTts() {
        _ttsManager.value?.pause()
    }
    
    fun resumeTts() {
        _ttsManager.value?.resume()
    }
    
    fun stopTts() {
        _ttsManager.value?.stop()
    }
    
    fun skipToNextSegment() {
        _ttsManager.value?.skipToNext()
    }
    
    fun skipToPreviousSegment() {
        _ttsManager.value?.skipToPrevious()
    }
    
    // Addendums Methods
    fun loadAddendums() {
        viewModelScope.launch(Dispatchers.IO) {
            _addendumsLoading.value = true
            _addendumsError.value = null
            try {
                // Nejprve zkontrolujeme aktuální network stav
                refreshNetworkState()
                
                if (_isOffline.value) {
                    // Offline režim - načteme z cache
                    val cachedAddendums = offlineDataManager.getCachedAddendums() ?: emptyList()
                    _addendums.value = cachedAddendums
                    println("DEBUG: Loaded offline addendums - Count: ${cachedAddendums.size}")
                } else {
                    // Online režim - načteme z API a uložíme do cache
                    val addendumsArray = ApiClient.apiService.getAddendums()
                    val addendumsList = addendumsArray.toList()
                    _addendums.value = addendumsList
                    // Uložíme do offline cache pro příští použití
                    offlineDataManager.saveAddendums(addendumsList)
                    println("DEBUG: Loaded online addendums - Count: ${addendumsList.size}")
                }
            } catch (e: Exception) {
                _addendumsError.value = "Chyba při načítání dodatků: ${e.message}"
                println("DEBUG: Error loading addendums: ${e.message}")
                e.printStackTrace()
            } finally {
                _addendumsLoading.value = false
            }
        }
    }
    
    suspend fun downloadPostPdf(postId: Int): ResponseBody {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("MainViewModel", "Volání API pro PDF generování, postId: $postId")
                val response = ApiClient.apiService.generatePostPdf(postId)
                android.util.Log.d("MainViewModel", "API odpovědělo, velikost: ${response.contentLength()}")
                response
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Chyba při volání PDF API: ${e.message}", e)
                throw e
            }
        }
    }

    fun loadAddendum(addendumId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _addendumsLoading.value = true
            _addendumsError.value = null
            try {
                // Nejprve zkontrolujeme aktuální network stav
                refreshNetworkState()
                
                if (_isOffline.value) {
                    // Offline režim - načteme z cache
                    val cachedAddendum = offlineDataManager.getCachedAddendum(addendumId)
                    if (cachedAddendum != null) {
                        // Přidáme/aktualizujeme v seznamu addendums
                        val currentList = _addendums.value.toMutableList()
                        val existingIndex = currentList.indexOfFirst { it.id == addendumId }
                        if (existingIndex >= 0) {
                            currentList[existingIndex] = cachedAddendum
                        } else {
                            currentList.add(cachedAddendum)
                        }
                        _addendums.value = currentList
                        println("DEBUG: Loaded offline addendum - ID: $addendumId")
                    } else {
                        _addendumsError.value = "Dodatek nenalezen v offline cache"
                    }
                } else {
                    // Online režim - načteme z API
                    val addendum = ApiClient.apiService.getAddendum(addendumId)
                    // Přidáme/aktualizujeme v seznamu addendums
                    val currentList = _addendums.value.toMutableList()
                    val existingIndex = currentList.indexOfFirst { it.id == addendumId }
                    if (existingIndex >= 0) {
                        currentList[existingIndex] = addendum
                    } else {
                        currentList.add(addendum)
                    }
                    _addendums.value = currentList
                    println("DEBUG: Loaded online addendum - ID: $addendumId")
                }
            } catch (e: Exception) {
                _addendumsError.value = "Chyba při načítání dodatku: ${e.message}"
                println("DEBUG: Error loading addendum $addendumId: ${e.message}")
                e.printStackTrace()
            } finally {
                _addendumsLoading.value = false
            }
        }
    }
    
    fun clearAddendums() {
        _addendums.value = emptyList()
        _addendumsError.value = null
        _addendumsLoading.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        _ttsManager.value?.destroy()
    }
}