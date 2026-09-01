package com.uncaan.imit.core.download

import android.content.Context
import androidx.work.WorkManager
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
