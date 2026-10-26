package com.tobiso.tobisoappnative.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.repository.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resolves the legal documents (posts identified by slug) to post ids so they open in-app. */
@HiltViewModel
class LegalDocsViewModel @Inject constructor(
    private val postsRepo: PostsRepository
) : ViewModel() {

    private val _postIds = MutableStateFlow<Map<String, Int>>(emptyMap())
    val postIds: StateFlow<Map<String, Int>> = _postIds

    init {
        viewModelScope.launch {
            postsRepo.getPostSummaries().onSuccess { summaries ->
                _postIds.value = summaries
                    .filter { slugOf(it.filePath) in SLUGS }
                    .associate { slugOf(it.filePath) to it.id }
            }
        }
    }

    private fun slugOf(filePath: String) =
        filePath.substringAfterLast('/').removeSuffix(".md").removeSuffix(".html")

    companion object {
        const val TERMS = "terms-of-use-cs"
        const val PRIVACY = "privacy-policy-cs"
        private val SLUGS = setOf(TERMS, PRIVACY)
    }
}
