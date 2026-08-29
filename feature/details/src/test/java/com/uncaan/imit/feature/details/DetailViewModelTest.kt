package com.uncaan.imit.feature.details

import app.cash.turbine.test
import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.PlayableStream
import com.uncaan.imit.core.model.VideoDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val videoRepository: VideoRepository = mockk()
    private val downloadedVideoDao: DownloadedVideoDao = mockk(relaxed = true)

    private val sampleStreamHd = PlayableStream(
        fileName = "lecture01_720p.mp4",
        format = "mp4",
        sizeBytes = 200 * 1024 * 1024L,
        durationSeconds = 3600.0,
        height = 720,
        width = 1280,
        streamUrl = "https://archive.org/download/lec01/lec01_720p.mp4"
    )

    private val sampleStreamSd = PlayableStream(
        fileName = "lecture01_360p.mp4",
        format = "mp4",
        sizeBytes = 80 * 1024 * 1024L,
        durationSeconds = 3600.0,
        height = 360,
        width = 640,
        streamUrl = "https://archive.org/download/lec01/lec01_360p.mp4"
    )

    private val sampleDetail = VideoDetail(
        identifier = "mit-ocw-6.0001-lec01",
        title = "Lecture 1: What is Computation?",
        description = "Introduction to Computer Science and Programming in Python",
        creator = "Prof. Eric Grimson",
        streams = listOf(sampleStreamSd, sampleStreamHd),
        thumbnailUrl = "https://archive.org/img/mit-ocw-6.0001"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load success updates state to Success with bestStream and download status`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DetailUiState.Success)
            val success = state as DetailUiState.Success
            assertEquals(sampleDetail, success.detail)
            assertEquals(sampleStreamHd, success.selectedStream) // bestStream is 720p
            assertNull(success.downloadStatus)
            assertEquals(0, success.downloadProgress)
            assertFalse(success.isDescriptionExpanded)
        }
    }

    @Test
    fun `initial load failure updates state to Error`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.failure(IOException("Server error"))
        )

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DetailUiState.Error)
            assertEquals("Server error", (state as DetailUiState.Error).message)
        }
    }

    @Test
    fun `selectQuality updates selectedStream in Success state`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.SelectQuality(sampleStreamSd))
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DetailUiState.Success
            assertEquals(sampleStreamSd, state.selectedStream)
        }
    }

    @Test
    fun `streamVideo sets navigateToPlayer with streamUrl when download not completed`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.StreamVideo)
        advanceUntilIdle()

        viewModel.navigateToPlayer.test {
            assertEquals(sampleStreamHd.streamUrl, awaitItem())
        }
    }

    @Test
    fun `streamVideo sets navigateToPlayer with localFilePath when download is completed`() = runTest(testDispatcher) {
        val downloadedEntity = DownloadedVideoEntity(
            identifier = "mit-ocw-6.0001-lec01",
            title = sampleDetail.title,
            description = sampleDetail.description,
            fileName = sampleStreamHd.fileName,
            downloadUrl = sampleStreamHd.streamUrl,
            localFilePath = "/storage/emulated/0/Downloads/lec01.mp4",
            fileSizeBytes = sampleStreamHd.sizeBytes,
            progress = 100,
            status = DownloadStatus.COMPLETED,
            downloadedAt = System.currentTimeMillis()
        )

        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns downloadedEntity

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.StreamVideo)
        advanceUntilIdle()

        viewModel.navigateToPlayer.test {
            assertEquals("/storage/emulated/0/Downloads/lec01.mp4", awaitItem())
        }
    }

    @Test
    fun `initiateDownload inserts entity into downloadedVideoDao and updates uiState to PENDING`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.DownloadVideo)
        advanceUntilIdle()

        coVerify {
            downloadedVideoDao.insert(
                match { entity ->
                    entity.identifier == "mit-ocw-6.0001-lec01" &&
                        entity.status == DownloadStatus.PENDING &&
                        entity.fileName == sampleStreamHd.fileName
                }
            )
        }

        viewModel.uiState.test {
            val state = awaitItem() as DetailUiState.Success
            assertEquals(DownloadStatus.PENDING, state.downloadStatus)
            assertEquals(0, state.downloadProgress)
        }
    }

    @Test
    fun `toggleDescription flips isDescriptionExpanded`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.ToggleDescription)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DetailUiState.Success
            assertTrue(state.isDescriptionExpanded)
        }

        viewModel.onEvent(DetailUiEvent.ToggleDescription)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DetailUiState.Success
            assertFalse(state.isDescriptionExpanded)
        }
    }

    @Test
    fun `retry reloads video detail`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.failure(IOException("Timeout"))
        ) andThen flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.Retry)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DetailUiState.Success
            assertEquals(sampleDetail, state.detail)
        }
    }

    @Test
    fun `onPlayerNavigated resets navigateToPlayer to null`() = runTest(testDispatcher) {
        coEvery { videoRepository.getVideoDetail("mit-ocw-6.0001-lec01") } returns flowOf(
            Result.success(sampleDetail)
        )
        coEvery { downloadedVideoDao.getDownloadedVideoByIdSync("mit-ocw-6.0001-lec01") } returns null

        val viewModel = DetailViewModel(
            identifier = "mit-ocw-6.0001-lec01",
            videoRepository = videoRepository,
            downloadedVideoDao = downloadedVideoDao
        )
        advanceUntilIdle()

        viewModel.onEvent(DetailUiEvent.StreamVideo)
        advanceUntilIdle()

        viewModel.onPlayerNavigated()

        viewModel.navigateToPlayer.test {
            assertNull(awaitItem())
        }
    }
}
