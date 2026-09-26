package com.uncaan.imit.feature.downloads

import app.cash.turbine.test
import androidx.work.Data
import androidx.work.WorkInfo
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.download.DownloadManagerHelper
import com.uncaan.imit.core.download.VideoDownloadWorker
import com.uncaan.imit.core.model.DownloadStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val downloadedVideoDao: DownloadedVideoDao = mockk(relaxed = true)
    private val downloadManager: DownloadManagerHelper = mockk(relaxed = true)

    private val sampleEntity = DownloadedVideoEntity(
        identifier = "mit-ocw-lec01",
        title = "Lecture 1: Introduction",
        description = "Intro to computer science",
        fileName = "lec01.mp4",
        downloadUrl = "https://archive.org/download/lec01.mp4",
        localFilePath = "/storage/emulated/0/Downloads/lec01.mp4",
        fileSizeBytes = 250L * 1024L * 1024L,
        progress = 100,
        status = DownloadStatus.COMPLETED,
        downloadedAt = 1700000000000L
    )

    private val downloadingEntity = DownloadedVideoEntity(
        identifier = "mit-ocw-lec02",
        title = "Lecture 2: Branching",
        description = "Branching and Iteration",
        fileName = "lec02.mp4",
        downloadUrl = "https://archive.org/download/lec02.mp4",
        localFilePath = null,
        fileSizeBytes = 180L * 1024L * 1024L,
        progress = 30,
        status = DownloadStatus.DOWNLOADING,
        downloadedAt = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default stub: no active WorkInfo items
        every { downloadManager.getAllDownloadsWorkInfoFlow() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load emits Empty when dao returns empty downloads list`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(emptyList())

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DownloadsUiState.Empty)
        }
    }

    @Test
    fun `initial load emits Success with downloads and storage metrics when dao returns records`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))
        coEvery { downloadedVideoDao.getTotalDownloadedSize() } returns (250L * 1024L * 1024L)
        every { downloadManager.getAvailableStorageMb() } returns 12500L

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DownloadsUiState.Success)
            val success = state as DownloadsUiState.Success
            assertEquals(1, success.downloads.size)
            assertEquals("mit-ocw-lec01", success.downloads.first().identifier)
            assertEquals(250L * 1024L * 1024L, success.totalStorageUsedBytes)
            assertEquals(12500L, success.availableStorageMb)
        }
    }

    @Test
    fun `live progress from WorkInfo populates liveProgressMap for RUNNING downloads`() = runTest(testDispatcher) {
        val progressData = Data.Builder()
            .putInt(VideoDownloadWorker.KEY_PROGRESS, 45)
            .build()

        val runningWorkInfo = createWorkInfo(
            state = WorkInfo.State.RUNNING,
            progress = progressData,
            tags = setOf(
                DownloadManagerHelper.TAG_ALL_DOWNLOADS,
                "${DownloadManagerHelper.TAG_DOWNLOAD_PREFIX}mit-ocw-lec02"
            )
        )

        every { downloadedVideoDao.getAllDownloads() } returns flowOf(
            listOf(sampleEntity, downloadingEntity)
        )
        every { downloadManager.getAllDownloadsWorkInfoFlow() } returns flowOf(
            listOf(runningWorkInfo)
        )
        every { downloadManager.getIdentifierFromWorkInfo(runningWorkInfo) } returns "mit-ocw-lec02"
        coEvery { downloadedVideoDao.getTotalDownloadedSize() } returns (250L * 1024L * 1024L)
        every { downloadManager.getAvailableStorageMb() } returns 12500L

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DownloadsUiState.Success
            assertEquals(2, state.downloads.size)
            assertEquals(45, state.liveProgressMap["mit-ocw-lec02"])
            assertNull(state.liveProgressMap["mit-ocw-lec01"])
        }
    }

    @Test
    fun `WorkInfo SUCCEEDED state maps to 100 in liveProgressMap`() = runTest(testDispatcher) {
        val succeededWorkInfo = createWorkInfo(
            state = WorkInfo.State.SUCCEEDED,
            tags = setOf(
                DownloadManagerHelper.TAG_ALL_DOWNLOADS,
                "${DownloadManagerHelper.TAG_DOWNLOAD_PREFIX}mit-ocw-lec02"
            )
        )

        every { downloadedVideoDao.getAllDownloads() } returns flowOf(
            listOf(sampleEntity, downloadingEntity)
        )
        every { downloadManager.getAllDownloadsWorkInfoFlow() } returns flowOf(
            listOf(succeededWorkInfo)
        )
        every { downloadManager.getIdentifierFromWorkInfo(succeededWorkInfo) } returns "mit-ocw-lec02"
        coEvery { downloadedVideoDao.getTotalDownloadedSize() } returns (430L * 1024L * 1024L)
        every { downloadManager.getAvailableStorageMb() } returns 12000L

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DownloadsUiState.Success
            assertEquals(100, state.liveProgressMap["mit-ocw-lec02"])
        }
    }

    @Test
    fun `WorkInfo ENQUEUED state maps to 0 in liveProgressMap`() = runTest(testDispatcher) {
        val enqueuedWorkInfo = createWorkInfo(
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(
                DownloadManagerHelper.TAG_ALL_DOWNLOADS,
                "${DownloadManagerHelper.TAG_DOWNLOAD_PREFIX}mit-ocw-lec02"
            )
        )

        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(downloadingEntity))
        every { downloadManager.getAllDownloadsWorkInfoFlow() } returns flowOf(
            listOf(enqueuedWorkInfo)
        )
        every { downloadManager.getIdentifierFromWorkInfo(enqueuedWorkInfo) } returns "mit-ocw-lec02"
        coEvery { downloadedVideoDao.getTotalDownloadedSize() } returns 0L
        every { downloadManager.getAvailableStorageMb() } returns 15000L

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DownloadsUiState.Success
            assertEquals(0, state.liveProgressMap["mit-ocw-lec02"])
        }
    }

    @Test
    fun `WorkInfo CANCELLED state excluded from liveProgressMap`() = runTest(testDispatcher) {
        val cancelledWorkInfo = createWorkInfo(
            state = WorkInfo.State.CANCELLED,
            tags = setOf(
                DownloadManagerHelper.TAG_ALL_DOWNLOADS,
                "${DownloadManagerHelper.TAG_DOWNLOAD_PREFIX}mit-ocw-lec02"
            )
        )

        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(downloadingEntity))
        every { downloadManager.getAllDownloadsWorkInfoFlow() } returns flowOf(
            listOf(cancelledWorkInfo)
        )
        every { downloadManager.getIdentifierFromWorkInfo(cancelledWorkInfo) } returns "mit-ocw-lec02"
        coEvery { downloadedVideoDao.getTotalDownloadedSize() } returns 0L
        every { downloadManager.getAvailableStorageMb() } returns 15000L

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DownloadsUiState.Success
            assertTrue(state.liveProgressMap.isEmpty())
        }
    }

    @Test
    fun `storage recalculates on every combined emission`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))
        coEvery { downloadedVideoDao.getTotalDownloadedSize() } returns (500L * 1024L * 1024L)
        every { downloadManager.getAvailableStorageMb() } returns 9000L

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() as DownloadsUiState.Success
            assertEquals(500L * 1024L * 1024L, state.totalStorageUsedBytes)
            assertEquals(9000L, state.availableStorageMb)
        }
    }

    @Test
    fun `DeleteDownload event sets showDeleteDialog state with identifier and path`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(
            DownloadsUiEvent.DeleteDownload(
                identifier = "mit-ocw-lec01",
                localFilePath = "/storage/emulated/0/Downloads/lec01.mp4"
            )
        )

        viewModel.showDeleteDialog.test {
            val dialogTarget = awaitItem()
            assertEquals("mit-ocw-lec01", dialogTarget?.first)
            assertEquals("/storage/emulated/0/Downloads/lec01.mp4", dialogTarget?.second)
        }
    }

    @Test
    fun `DismissDeleteDialog clears showDeleteDialog state`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(
            DownloadsUiEvent.DeleteDownload(
                identifier = "mit-ocw-lec01",
                localFilePath = "/storage/emulated/0/Downloads/lec01.mp4"
            )
        )
        viewModel.onEvent(DownloadsUiEvent.DismissDeleteDialog)

        viewModel.showDeleteDialog.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `ConfirmDelete deletes file, deletes record from dao, cancels download work, and clears dialog`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))
        coEvery { downloadedVideoDao.deleteDownload(any()) } returns 1

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(
            DownloadsUiEvent.ConfirmDelete(
                identifier = "mit-ocw-lec01",
                localFilePath = "/storage/emulated/0/Downloads/lec01.mp4"
            )
        )
        advanceUntilIdle()

        verify { downloadManager.deleteDownloadedFile("/storage/emulated/0/Downloads/lec01.mp4") }
        coVerify { downloadedVideoDao.deleteDownload("mit-ocw-lec01") }
        verify { downloadManager.cancelDownload("mit-ocw-lec01") }

        viewModel.showDeleteDialog.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `PauseDownload calls downloadManager pauseDownload and updates dao status to PAUSED`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(DownloadsUiEvent.PauseDownload("mit-ocw-lec01"))
        advanceUntilIdle()

        verify { downloadManager.pauseDownload("mit-ocw-lec01") }
        coVerify { downloadedVideoDao.updateStatus("mit-ocw-lec01", DownloadStatus.PAUSED) }
    }

    @Test
    fun `ResumeDownload calls downloadManager resumeDownload`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))
        every {
            downloadManager.resumeDownload(any(), any(), any(), any())
        } returns Result.success(UUID.randomUUID())

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(
            DownloadsUiEvent.ResumeDownload(
                identifier = "mit-ocw-lec01",
                title = "Lecture 1",
                downloadUrl = "https://archive.org/download/lec01.mp4",
                fileName = "lec01.mp4"
            )
        )
        advanceUntilIdle()

        verify {
            downloadManager.resumeDownload(
                identifier = "mit-ocw-lec01",
                title = "Lecture 1",
                downloadUrl = "https://archive.org/download/lec01.mp4",
                fileName = "lec01.mp4"
            )
        }
    }

    @Test
    fun `RetryDownload calls downloadManager resumeDownload`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))
        every {
            downloadManager.resumeDownload(any(), any(), any(), any())
        } returns Result.success(UUID.randomUUID())

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(
            DownloadsUiEvent.RetryDownload(
                identifier = "mit-ocw-lec01",
                title = "Lecture 1",
                downloadUrl = "https://archive.org/download/lec01.mp4",
                fileName = "lec01.mp4"
            )
        )
        advanceUntilIdle()

        verify {
            downloadManager.resumeDownload(
                identifier = "mit-ocw-lec01",
                title = "Lecture 1",
                downloadUrl = "https://archive.org/download/lec01.mp4",
                fileName = "lec01.mp4"
            )
        }
    }

    @Test
    fun `CancelDownload cancels download in helper and deletes record from dao`() = runTest(testDispatcher) {
        every { downloadedVideoDao.getAllDownloads() } returns flowOf(listOf(sampleEntity))

        val viewModel = DownloadsViewModel(
            downloadedVideoDao = downloadedVideoDao,
            downloadManager = downloadManager
        )
        advanceUntilIdle()

        viewModel.onEvent(
            DownloadsUiEvent.CancelDownload(
                identifier = "mit-ocw-lec01",
                fileName = "lec01.mp4"
            )
        )
        advanceUntilIdle()

        verify { downloadManager.cancelDownload("mit-ocw-lec01", "lec01.mp4") }
        coVerify { downloadedVideoDao.deleteDownload("mit-ocw-lec01") }
    }

    /**
     * Creates a [WorkInfo] instance for testing purposes.
     *
     * Uses MockK to create a relaxed mock with the specified state, progress, and tags.
     */
    private fun createWorkInfo(
        state: WorkInfo.State,
        progress: Data = Data.EMPTY,
        tags: Set<String> = emptySet()
    ): WorkInfo {
        val workInfo: WorkInfo = mockk(relaxed = true)
        every { workInfo.state } returns state
        every { workInfo.progress } returns progress
        every { workInfo.tags } returns tags
        return workInfo
    }
}
