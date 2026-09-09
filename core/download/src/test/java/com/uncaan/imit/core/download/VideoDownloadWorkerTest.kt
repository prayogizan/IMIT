package com.uncaan.imit.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VideoDownloadWorkerTest {

    @Test
    fun `getNotificationId with null identifier returns DEFAULT_NOTIFICATION_ID`() {
        val result = VideoDownloadWorker.getNotificationId(null)
        assertEquals(VideoDownloadWorker.DEFAULT_NOTIFICATION_ID, result)
        assertEquals(1001, result)
    }

    @Test
    fun `getNotificationId with same identifier produces deterministic identical id`() {
        val identifier = "mit-ocw-lecture-quantum-mechanics-01"
        val id1 = VideoDownloadWorker.getNotificationId(identifier)
        val id2 = VideoDownloadWorker.getNotificationId(identifier)
        assertEquals(id1, id2)
    }

    @Test
    fun `getNotificationId with different identifiers produces distinct ids`() {
        val id1 = VideoDownloadWorker.getNotificationId("mit-ocw-lec01")
        val id2 = VideoDownloadWorker.getNotificationId("mit-ocw-lec02")
        val id3 = VideoDownloadWorker.getNotificationId("physics-8.01-class")
        val id4 = VideoDownloadWorker.getNotificationId("linear-algebra-18.06")

        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id3, id4)
    }

    @Test
    fun `getNotificationId result is strictly positive and within valid range`() {
        val identifiers = listOf(
            "a",
            "test-id",
            "very-long-identifier-string-with-many-characters-and-symbols-1234567890",
            "",
            "course_video_lec_99"
        )

        for (id in identifiers) {
            val notificationId = VideoDownloadWorker.getNotificationId(id)
            assertTrue(
                "Notification ID $notificationId must be >= 1000",
                notificationId >= 1000
            )
            assertTrue(
                "Notification ID $notificationId must be < 101000",
                notificationId < 101000
            )
        }
    }

    @Test
    fun `getNotificationId handles edge cases where hashCode is Int MIN_VALUE without negative overflow`() {
        // "polygenelubricants" is a known Java string whose hashCode is Integer.MIN_VALUE (-2147483648)
        val minHashString = "polygenelubricants"
        assertEquals(Int.MIN_VALUE, minHashString.hashCode())

        val result = VideoDownloadWorker.getNotificationId(minHashString)
        assertTrue(
            "Notification ID must be strictly positive even when hashCode is Int.MIN_VALUE",
            result >= 1000
        )
        val expected = (abs(Int.MIN_VALUE.toLong()) % 100_000L + 1000L).toInt()
        assertEquals(expected, result)
    }

    @Test
    fun `notification constants retain backwards compatibility`() {
        assertEquals(1001, VideoDownloadWorker.NOTIFICATION_ID)
        assertEquals(1001, VideoDownloadWorker.DEFAULT_NOTIFICATION_ID)
        assertEquals("video_downloads", VideoDownloadWorker.CHANNEL_ID)
    }
}
