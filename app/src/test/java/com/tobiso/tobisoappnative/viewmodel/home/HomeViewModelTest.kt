package com.tobiso.tobisoappnative.viewmodel.home

import com.tobiso.tobisoappnative.model.Category
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private lateinit var postsRepo: PostsRepository
    private lateinit var offlineRepo: OfflineRepositoryImpl
    private lateinit var viewModel: HomeViewModel

    private val sampleCategory = Category(id = 1, name = "Mluvnice", slug = "mluvnice", parentId = null)
    private val samplePost = Post(
        id = 10, title = "Test", content = null, filePath = "test.md",
        createdAt = "2024-01-01T10:00:00", lastEdit = "2024-01-02T10:00:00", categoryId = 1
    )

    @Before
    fun setUp() {
        postsRepo = mockk()
        offlineRepo = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun `load sets categories and posts on success`() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        coEvery { postsRepo.getCategories() } returns Result.success(listOf(sampleCategory))
        coEvery { postsRepo.getPostsByCategory(null, any()) } returns Result.success(listOf(samplePost))

        viewModel = HomeViewModel(mockk(relaxed = true), postsRepo, offlineRepo)
        viewModel.onIntent(HomeIntent.Load)
        delay(200)

        assert(viewModel.uiState.value.categories.size == 1)
        assert(viewModel.uiState.value.posts.size == 1)
        assert(!viewModel.uiState.value.isOffline)
    }

    @Test
    fun `load sets offline flag when categories fail`() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        coEvery { postsRepo.getCategories() } returns Result.failure(Exception("Offline"))
        coEvery { postsRepo.getPostsByCategory(null, any()) } returns Result.failure(Exception("Offline"))

        viewModel = HomeViewModel(mockk(relaxed = true), postsRepo, offlineRepo)
        viewModel.onIntent(HomeIntent.Load)
        delay(200)

        assert(viewModel.uiState.value.isOffline)
        assert(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `computeNewest filters posts by subject descendants`() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val childCategory = Category(id = 2, name = "Podstatná jména", slug = "podstatna-jmena", parentId = 1)
        val childPost = Post(
            id = 20, title = "Child post", content = null, filePath = "child.md",
            createdAt = "2024-01-01T10:00:00", categoryId = 2
        )
        coEvery { postsRepo.getCategories() } returns Result.success(listOf(sampleCategory, childCategory))
        coEvery { postsRepo.getPostsByCategory(null, any()) } returns Result.success(listOf(samplePost, childPost))

        viewModel = HomeViewModel(mockk(relaxed = true), postsRepo, offlineRepo)
        viewModel.onIntent(HomeIntent.Load)
        delay(200)

        assert(viewModel.newestPosts.value.any { it.id == 10 })
    }
}
