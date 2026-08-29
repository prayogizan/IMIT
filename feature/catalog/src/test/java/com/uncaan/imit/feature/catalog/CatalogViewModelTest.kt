package com.uncaan.imit.feature.catalog

import app.cash.turbine.test
import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.network.exception.NoConnectivityException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val videoRepository: VideoRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSampleVideos(count: Int, startId: Int = 1): List<CourseVideo> {
        return (startId until startId + count).map { id ->
            CourseVideo(
                identifier = "video-$id",
                title = "MIT Lecture $id",
                description = "Description $id",
                creator = "MIT OCW",
                year = 2024,
                thumbnailUrl = "https://archive.org/img/$id",
                downloadsCount = 100L * id
            )
        }
    }

    @Test
    fun `initial load success updates state to Success with videos`() = runTest(testDispatcher) {
        val sampleVideos = createSampleVideos(20)
        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(sampleVideos))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is CatalogUiState.Success)
            val successState = state as CatalogUiState.Success
            assertEquals(20, successState.videos.size)
            assertEquals(1, successState.currentPage)
            assertTrue(successState.canLoadMore)
            assertFalse(successState.isOffline)
            assertFalse(successState.isRefreshing)
            assertFalse(successState.isLoadingMore)
        }
    }

    @Test
    fun `initial load failure with no connectivity updates state to Error with internet message`() = runTest(testDispatcher) {
        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(
            Result.failure(NoConnectivityException())
        )

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is CatalogUiState.Error)
            assertEquals("No internet connection.", (state as CatalogUiState.Error).message)
        }
    }

    @Test
    fun `initial load generic failure updates state to Error with localized message`() = runTest(testDispatcher) {
        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(
            Result.failure(IOException("Server error"))
        )

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is CatalogUiState.Error)
            assertEquals("Failed to load videos: Server error", (state as CatalogUiState.Error).message)
        }
    }

    @Test
    fun `refresh reloads page 1 and updates state`() = runTest(testDispatcher) {
        val initialVideos = createSampleVideos(20, startId = 1)
        val refreshedVideos = createSampleVideos(20, startId = 100)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(initialVideos)) andThen flowOf(Result.success(refreshedVideos))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.Refresh)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(20, state.videos.size)
            assertEquals("video-100", state.videos.first().identifier)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `loadMore appends next page videos and increments currentPage`() = runTest(testDispatcher) {
        val page1Videos = createSampleVideos(20, startId = 1)
        val page2Videos = createSampleVideos(10, startId = 21)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(page1Videos))
        coEvery { videoRepository.getMitOcwVideos(page = 2) } returns flowOf(Result.success(page2Videos))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.LoadMore)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(30, state.videos.size)
            assertEquals(2, state.currentPage)
            assertFalse(state.canLoadMore) // page 2 returned 10 items (< 20)
            assertFalse(state.isLoadingMore)
        }
    }

    @Test
    fun `loadMore on error retains existing videos and marks offline if NoConnectivityException`() = runTest(testDispatcher) {
        val page1Videos = createSampleVideos(20, startId = 1)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(page1Videos))
        coEvery { videoRepository.getMitOcwVideos(page = 2) } returns flowOf(Result.failure(NoConnectivityException()))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.LoadMore)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(20, state.videos.size)
            assertTrue(state.isOffline)
            assertFalse(state.isLoadingMore)
        }
    }

    @Test
    fun `retry reloads first page`() = runTest(testDispatcher) {
        val sampleVideos = createSampleVideos(5)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(
            Result.failure(IOException("Timeout"))
        ) andThen flowOf(Result.success(sampleVideos))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.Retry)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(5, state.videos.size)
        }
    }

    @Test
    fun `searchQueryChanged debounces and executes search after 300ms`() = runTest(testDispatcher) {
        val initialVideos = createSampleVideos(20)
        val searchResults = createSampleVideos(5, startId = 50)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(initialVideos))
        coEvery { videoRepository.searchVideos("physics", page = 1) } returns flowOf(Result.success(searchResults))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.SearchQueryChanged("phys"))
        advanceTimeBy(100) // Not yet debounced

        viewModel.onEvent(CatalogUiEvent.SearchQueryChanged("physics"))
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(5, state.videos.size)
            assertEquals("physics", state.searchQuery)
            assertEquals("video-50", state.videos.first().identifier)
        }
    }

    @Test
    fun `searchQueryChanged with blank query resets to initial video list`() = runTest(testDispatcher) {
        val initialVideos = createSampleVideos(10)
        val searchResults = createSampleVideos(2, startId = 99)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(initialVideos))
        coEvery { videoRepository.searchVideos("test", page = 1) } returns flowOf(Result.success(searchResults))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.SearchQueryChanged("test"))
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.SearchQueryChanged("   "))
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(10, state.videos.size)
        }
    }

    @Test
    fun `clearSearch cancels search and reloads initial video page`() = runTest(testDispatcher) {
        val initialVideos = createSampleVideos(10)
        val searchResults = createSampleVideos(3, startId = 70)

        coEvery { videoRepository.getMitOcwVideos(page = 1) } returns flowOf(Result.success(initialVideos))
        coEvery { videoRepository.searchVideos("math", page = 1) } returns flowOf(Result.success(searchResults))

        val viewModel = CatalogViewModel(videoRepository)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.SearchQueryChanged("math"))
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.onEvent(CatalogUiEvent.ClearSearch)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as CatalogUiState.Success
            assertEquals(10, state.videos.size)
        }
    }
}
