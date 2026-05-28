package com.tobiso.tobisoappnative.viewmodel.postdetail

import android.app.Application
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.RelatedPost
import com.tobiso.tobisoappnative.repository.FavoritesRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PostDetailViewModelTest {

    private lateinit var repo: PostsRepository
    private lateinit var favoritesRepo: FavoritesRepositoryImpl
    private lateinit var app: Application
    private lateinit var viewModel: PostDetailViewModel

    private val samplePost = Post(
        id = 10, title = "Podstatná jména", filePath = "pj.md",
        createdAt = "2024-01-01T10:00:00", categoryId = 3
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        app = mockk(relaxed = true)
        repo = mockk()
        favoritesRepo = mockk(relaxed = true)
        coEvery { favoritesRepo.favoritePosts } returns flowOf(emptyList())
        viewModel = PostDetailViewModel(app, repo, repo, favoritesRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun `loadPostDetail populates postDetail on success`() = runBlocking {
        coEvery { repo.getPost(10, any()) } returns Result.success(samplePost)

        viewModel.loadPostDetail(10)
        delay(100)

        assertEquals(samplePost, viewModel.postDetail.value)
        assertNull(viewModel.postDetailError.value)
        assertFalse(viewModel.isOffline.value)
    }

    @Test
    fun `loadPostDetail sets error on network failure`() = runBlocking {
        coEvery { repo.getPost(10, any()) } returns Result.failure(RuntimeException("Server error"))

        viewModel.loadPostDetail(10)
        delay(100)

        assertNull(viewModel.postDetail.value)
        assertNotNull(viewModel.postDetailError.value)
        assertTrue(viewModel.postDetailError.value!!.contains("Server error"))
    }

    @Test
    fun `loadPostDetail sets isOffline on IllegalStateException`() = runBlocking {
        coEvery { repo.getPost(10, any()) } returns Result.failure(IllegalStateException("Offline"))

        viewModel.loadPostDetail(10)
        delay(100)

        assertTrue(viewModel.isOffline.value)
    }

    @Test
    fun `loadPosts populates posts list on success`() = runBlocking {
        coEvery { repo.getPostsByCategory(null) } returns Result.success(listOf(samplePost))

        viewModel.loadPosts()
        delay(100)

        assertEquals(1, viewModel.posts.value.size)
        assertEquals(10, viewModel.posts.value[0].id)
    }

    @Test
    fun `loadPosts tolerates failure without affecting other state`() = runBlocking {
        coEvery { repo.getPostsByCategory(null) } returns Result.failure(RuntimeException("Timeout"))

        viewModel.loadPosts()
        delay(100)

        assertTrue(viewModel.posts.value.isEmpty())
    }

    @Test
    fun `loadRelatedPosts populates relatedPosts on success`() = runBlocking {
        val related = RelatedPost(id = 1, postId = 10, relatedPostId = 20, postTitle = "A", relatedPostTitle = "B")
        coEvery { repo.getRelatedPosts(10, any(), any()) } returns Result.success(listOf(related))

        viewModel.loadRelatedPosts(10)
        delay(100)

        assertEquals(1, viewModel.relatedPosts.value.size)
        assertEquals(20, viewModel.relatedPosts.value[0].relatedPostId)
        assertNull(viewModel.relatedPostsError.value)
        assertFalse(viewModel.relatedPostsLoading.value)
    }

    @Test
    fun `loadRelatedPosts sets error on failure`() = runBlocking {
        coEvery { repo.getRelatedPosts(10, any(), any()) } returns Result.failure(RuntimeException("Not found"))

        viewModel.loadRelatedPosts(10)
        delay(100)

        assertTrue(viewModel.relatedPosts.value.isEmpty())
        assertNotNull(viewModel.relatedPostsError.value)
        assertFalse(viewModel.relatedPostsLoading.value)
    }

    @Test
    fun `savePost delegates to favoritesRepo`() = runBlocking {
        viewModel.savePost(samplePost)
        delay(50)

        coVerify { favoritesRepo.savePost(samplePost) }
    }

    @Test
    fun `unsavePost delegates to favoritesRepo`() = runBlocking {
        viewModel.unsavePost(10)
        delay(50)

        coVerify { favoritesRepo.unsavePost(10) }
    }

    @Test
    fun `clearDownloadUri resets downloadUri to null`() {
        viewModel.clearDownloadUri()
        assertNull(viewModel.downloadUri.value)
    }
}
