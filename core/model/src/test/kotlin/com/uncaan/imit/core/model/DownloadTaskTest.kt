package com.uncaan.imit.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadTaskTest {

    @Test
    fun isPlayable_completedWithLocalPath_returnsTrue() {
        val task = DownloadTask(
            identifier = "dl-1",
            title = "Lecture 1",
            fileName = "lec01.mp4",
            downloadUrl = "https://example.com/lec01.mp4",
            localFilePath = "/storage/emulated/0/Download/lec01.mp4",
            fileSizeBytes = 50 * 1024 * 1024L,
            progress = 100,
            status = DownloadStatus.COMPLETED
        )

        assertTrue(task.isPlayable)
    }

    @Test
    fun isPlayable_completedWithoutLocalPath_returnsFalse() {
        val task = DownloadTask(
            identifier = "dl-1",
            title = "Lecture 1",
            fileName = "lec01.mp4",
            downloadUrl = "https://example.com/lec01.mp4",
            localFilePath = null,
            fileSizeBytes = 50 * 1024 * 1024L,
            progress = 100,
            status = DownloadStatus.COMPLETED
        )

        assertFalse(task.isPlayable)
    }

    @Test
    fun isPlayable_downloadingWithLocalPath_returnsFalse() {
        val task = DownloadTask(
            identifier = "dl-1",
            title = "Lecture 1",
            fileName = "lec01.mp4",
            downloadUrl = "https://example.com/lec01.mp4",
            localFilePath = "/storage/emulated/0/Download/lec01.mp4",
            fileSizeBytes = 50 * 1024 * 1024L,
            progress = 50,
            status = DownloadStatus.DOWNLOADING
        )

        assertFalse(task.isPlayable)
    }

    @Test
    fun formattedSize_returnsCorrectSizeString() {
        val taskMb = DownloadTask(
            identifier = "dl-1",
            title = "Lecture 1",
            fileName = "lec01.mp4",
            downloadUrl = "https://example.com/lec01.mp4",
            fileSizeBytes = 128L * 1024 * 1024
        )
        assertEquals("128.0 MB", taskMb.formattedSize)

        val taskGb = DownloadTask(
            identifier = "dl-2",
            title = "Lecture 2",
            fileName = "lec02.mp4",
            downloadUrl = "https://example.com/lec02.mp4",
            fileSizeBytes = (2.5 * 1024 * 1024 * 1024).toLong()
        )
        assertEquals("2.5 GB", taskGb.formattedSize)
    }

    @Test
    fun downloadTask_serialization_roundTripSuccess() {
        val original = DownloadTask(
            identifier = "dl-1",
            title = "Lecture 1",
            fileName = "lec01.mp4",
            downloadUrl = "https://example.com/lec01.mp4",
            localFilePath = "/storage/emulated/0/Download/lec01.mp4",
            fileSizeBytes = 50 * 1024 * 1024L,
            progress = 100,
            status = DownloadStatus.COMPLETED,
            downloadedAt = 1700000000L
        )

        val json = Json.encodeToString(DownloadTask.serializer(), original)
        val deserialized = Json.decodeFromString(DownloadTask.serializer(), json)
        assertEquals(original, deserialized)
    }
}
