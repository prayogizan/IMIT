package com.uncaan.imit.core.download

import android.content.Context
import androidx.work.WorkManager
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadManagerHelperTest {

    @Test
    fun `getUniqueWorkName returns prefixed work name`() {
        val identifier = "test-video-123"
        val expected = "download_test-video-123"
        val actual = DownloadManagerHelper.getUniqueWorkName(identifier)
        assertEquals(expected, actual)
    }

    @Test
    fun `worker constants match expected keys and configuration`() {
        assertEquals("KEY_IDENTIFIER", VideoDownloadWorker.KEY_IDENTIFIER)
        assertEquals("KEY_DOWNLOAD_URL", VideoDownloadWorker.KEY_DOWNLOAD_URL)
        assertEquals("KEY_FILE_NAME", VideoDownloadWorker.KEY_FILE_NAME)
        assertEquals("KEY_TITLE", VideoDownloadWorker.KEY_TITLE)
        assertEquals("KEY_PROGRESS", VideoDownloadWorker.KEY_PROGRESS)
        assertEquals("KEY_ERROR", VideoDownloadWorker.KEY_ERROR)
        assertEquals("video_downloads", VideoDownloadWorker.CHANNEL_ID)
        assertEquals(1001, VideoDownloadWorker.NOTIFICATION_ID)
        assertEquals(524288000L, VideoDownloadWorker.MIN_STORAGE_BYTES)
        assertEquals("mit_ocw_videos", VideoDownloadWorker.DOWNLOAD_DIR_NAME)
    }

    @Test
    fun `enqueueDownload fails when storage check returns false`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)

        // Helper that overrides hasEnoughStorage to simulate low disk space
        val helper = object : DownloadManagerHelper(mockContext, mockWorkManager) {
            override fun hasEnoughStorage(): Boolean = false
        }

        val result = helper.enqueueDownload(
            identifier = "lecture-1",
            title = "Intro to Algorithms",
            downloadUrl = "https://archive.org/download/sample.mp4"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Insufficient storage") == true)
    }

    @Test
    fun `hasEnoughStorage returns false when statFs throws or directory invalid`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)

        val helper = object : DownloadManagerHelper(mockContext, mockWorkManager) {
            override fun getDownloadDirectory(): File = File("/non_existent_path_xyz_123")
        }

        assertFalse(helper.hasEnoughStorage())
    }

    @Test
    fun `getAvailableStorageMb converts bytes to megabytes`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)

        val helper = object : DownloadManagerHelper(mockContext, mockWorkManager) {
            override fun getAvailableStorageBytes(): Long = 200L * 1024L * 1024L
        }

        assertEquals(200L, helper.getAvailableStorageMb())
    }

    @Test
    fun `deleteDownloadedFile deletes existing file and returns true`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)
        val helper = DownloadManagerHelper(mockContext, mockWorkManager)

        val tempFile = File.createTempFile("test_video_", ".mp4")
        assertTrue(tempFile.exists())

        val result = helper.deleteDownloadedFile(tempFile.absolutePath)

        assertTrue(result)
        assertFalse(tempFile.exists())
    }

    @Test
    fun `deleteDownloadedFile returns false for nonexistent file`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)
        val helper = DownloadManagerHelper(mockContext, mockWorkManager)

        val result = helper.deleteDownloadedFile("/path/to/nonexistent_file_xyz_123.mp4")

        assertFalse(result)
    }

    @Test
    fun `resumeDownload fails when storage check returns false`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)

        val helper = object : DownloadManagerHelper(mockContext, mockWorkManager) {
            override fun hasEnoughStorage(): Boolean = false
        }

        val result = helper.resumeDownload(
            identifier = "lecture-1",
            title = "Intro to Algorithms",
            downloadUrl = "https://archive.org/download/sample.mp4"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Insufficient storage") == true)
    }

    @Test
    fun `resumeDownload enqueues work with REPLACE policy when storage sufficient`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)

        val helper = object : DownloadManagerHelper(mockContext, mockWorkManager) {
            override fun hasEnoughStorage(): Boolean = true
        }

        val result = helper.resumeDownload(
            identifier = "lecture-1",
            title = "Intro to Algorithms",
            downloadUrl = "https://archive.org/download/sample.mp4"
        )

        assertTrue(result.isSuccess)
        io.mockk.verify {
            mockWorkManager.enqueueUniqueWork(
                "download_lecture-1",
                androidx.work.ExistingWorkPolicy.REPLACE,
                any<androidx.work.OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `pauseDownload cancels unique work`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)
        val helper = DownloadManagerHelper(mockContext, mockWorkManager)

        helper.pauseDownload("lecture-1")

        io.mockk.verify {
            mockWorkManager.cancelUniqueWork("download_lecture-1")
        }
    }

    @Test
    fun `cancelDownload cancels work and deletes both tmp and final files`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)

        val tempDir = java.nio.file.Files.createTempDirectory("test_download_dir").toFile()
        val tmpFile = File(tempDir, "lecture-1.mp4.tmp").apply { writeText("partial data") }
        val finalFile = File(tempDir, "lecture-1.mp4").apply { writeText("final data") }

        assertTrue(tmpFile.exists())
        assertTrue(finalFile.exists())

        val helper = object : DownloadManagerHelper(mockContext, mockWorkManager) {
            override fun getDownloadDirectory(): File = tempDir
        }

        helper.cancelDownload("lecture-1", "lecture-1.mp4")

        io.mockk.verify {
            mockWorkManager.cancelUniqueWork("download_lecture-1")
        }
        assertFalse(tmpFile.exists())
        assertFalse(finalFile.exists())
    }

    @Test
    fun `TAG_ALL_DOWNLOADS matches expected constant value`() {
        assertEquals("tag_video_downloads", DownloadManagerHelper.TAG_ALL_DOWNLOADS)
    }

    @Test
    fun `getIdentifierFromWorkInfo extracts identifier from download tag`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)
        val helper = DownloadManagerHelper(mockContext, mockWorkManager)

        val workInfo: androidx.work.WorkInfo = io.mockk.mockk(relaxed = true)
        io.mockk.every { workInfo.tags } returns setOf(
            DownloadManagerHelper.TAG_ALL_DOWNLOADS,
            "${DownloadManagerHelper.TAG_DOWNLOAD_PREFIX}mit-ocw-6.0001-lec01"
        )

        val result = helper.getIdentifierFromWorkInfo(workInfo)

        assertEquals("mit-ocw-6.0001-lec01", result)
    }

    @Test
    fun `getIdentifierFromWorkInfo returns null when no download tag present`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)
        val helper = DownloadManagerHelper(mockContext, mockWorkManager)

        val workInfo: androidx.work.WorkInfo = io.mockk.mockk(relaxed = true)
        io.mockk.every { workInfo.tags } returns setOf(
            DownloadManagerHelper.TAG_ALL_DOWNLOADS,
            "some_unrelated_tag"
        )

        val result = helper.getIdentifierFromWorkInfo(workInfo)

        assertNull(result)
    }

    @Test
    fun `getIdentifierFromWorkInfo returns null for empty tags`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockWorkManager: WorkManager = mockk(relaxed = true)
        val helper = DownloadManagerHelper(mockContext, mockWorkManager)

        val workInfo: androidx.work.WorkInfo = io.mockk.mockk(relaxed = true)
        io.mockk.every { workInfo.tags } returns emptySet()

        val result = helper.getIdentifierFromWorkInfo(workInfo)

        assertNull(result)
    }
}
