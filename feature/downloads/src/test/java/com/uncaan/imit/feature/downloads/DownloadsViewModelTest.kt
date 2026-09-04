package com.uncaan.imit.feature.downloads

import app.cash.turbine.test
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.download.DownloadManagerHelper
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
}
