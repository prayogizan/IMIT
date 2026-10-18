package com.uncaan.imit.core.download

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.WorkerParameters
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit test suite verifying [VideoDownloadWorker] notification construction, deterministic IDs,
 * deep-link navigation [PendingIntent] binding, and auto-cancel flags.
 */
class VideoDownloadWorkerTest {

    private lateinit var mockContext: Context
    private lateinit var mockWorkerParams: WorkerParameters
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockDownloadDao: DownloadedVideoDao
    private lateinit var mockNavigator: DownloadNotificationNavigator
    private lateinit var mockDownloadsPendingIntent: PendingIntent
    private lateinit var mockDetailsPendingIntent: PendingIntent
    private lateinit var worker: VideoDownloadWorker

    private val testIdentifier = "mit-ocw-physics-801"
    private val testTitle = "Classical Mechanics Lec 01"

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockWorkerParams = mockk(relaxed = true)
        mockOkHttpClient = mockk(relaxed = true)
        mockDownloadDao = mockk(relaxed = true)
        mockNavigator = mockk(relaxed = true)
        mockDownloadsPendingIntent = mockk(relaxed = true)
        mockDetailsPendingIntent = mockk(relaxed = true)

        val inputData = Data.Builder()
            .putString(VideoDownloadWorker.KEY_IDENTIFIER, testIdentifier)
            .putString(VideoDownloadWorker.KEY_TITLE, testTitle)
            .build()
        every { mockWorkerParams.inputData } returns inputData
        every { mockNavigator.createDownloadsPendingIntent(any()) } returns mockDownloadsPendingIntent
        every { mockNavigator.createDetailsPendingIntent(any(), any()) } returns mockDetailsPendingIntent

        worker = VideoDownloadWorker(
            context = mockContext,
            workerParams = mockWorkerParams,
            okHttpClient = mockOkHttpClient,
            downloadDao = mockDownloadDao,
            downloadNavigator = mockNavigator
        )

        // Intercept factory in pure JVM tests to simulate built notification properties without android.jar stubs
        worker.notificationFactory = { builder ->
            val notif = mockk<Notification>(relaxed = true)
            notif.contentIntent = builder.getContentIntent()
            notif.flags = builder.getNotificationFlags()
            notif
        }
    }

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

    @Test
    fun `createForegroundNotification configures ongoing flag and downloads PendingIntent`() {
        val builder = worker.createForegroundNotificationBuilder(testTitle, 50)
        assertEquals(mockDownloadsPendingIntent, builder.getContentIntent())
        assertTrue(
            "Foreground notification builder must have FLAG_ONGOING_EVENT set",
            (builder.getNotificationFlags() and Notification.FLAG_ONGOING_EVENT) != 0
        )

        val notification = worker.createForegroundNotification(testTitle, 50)
        assertNotNull(notification)
        assertEquals(mockDownloadsPendingIntent, notification.contentIntent)
        assertTrue(
            "Foreground notification must have FLAG_ONGOING_EVENT set",
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        )
        verify(atLeast = 1) { mockNavigator.createDownloadsPendingIntent(mockContext) }
    }

    @Test
    fun `createCompletionNotification configures autoCancel flag and details PendingIntent`() {
        val builder = worker.createCompletionNotificationBuilder(testTitle, testIdentifier)
        assertEquals(mockDetailsPendingIntent, builder.getContentIntent())
        assertTrue(
            "Completion notification builder must have FLAG_AUTO_CANCEL set",
            (builder.getNotificationFlags() and Notification.FLAG_AUTO_CANCEL) != 0
        )

        val notification = worker.createCompletionNotification(testTitle, testIdentifier)
        assertNotNull(notification)
        assertEquals(mockDetailsPendingIntent, notification.contentIntent)
        assertTrue(
            "Completion notification must have FLAG_AUTO_CANCEL set",
            (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        )
        verify(atLeast = 1) { mockNavigator.createDetailsPendingIntent(mockContext, testIdentifier) }
    }

    @Test
    fun `createFailureNotification configures autoCancel flag and details PendingIntent`() {
        val reason = "Insufficient storage space (<500MB)"
        val builder = worker.createFailureNotificationBuilder(testTitle, testIdentifier, reason)
        assertEquals(mockDetailsPendingIntent, builder.getContentIntent())
        assertTrue(
            "Failure notification builder must have FLAG_AUTO_CANCEL set",
            (builder.getNotificationFlags() and Notification.FLAG_AUTO_CANCEL) != 0
        )

        val notification = worker.createFailureNotification(testTitle, testIdentifier, reason)
        assertNotNull(notification)
        assertEquals(mockDetailsPendingIntent, notification.contentIntent)
        assertTrue(
            "Failure notification must have FLAG_AUTO_CANCEL set",
            (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        )
        verify(atLeast = 1) { mockNavigator.createDetailsPendingIntent(mockContext, testIdentifier) }
    }

    @Test
    fun `getForegroundInfo returns ForegroundInfo with deterministic notificationId and navigation PendingIntent`() = runTest {
        val foregroundInfo = worker.getForegroundInfo()

        assertNotNull(foregroundInfo)
        assertEquals(worker.notificationId, foregroundInfo.notificationId)
        assertEquals(mockDownloadsPendingIntent, foregroundInfo.notification.contentIntent)
        assertTrue(
            "Foreground notification must have FLAG_ONGOING_EVENT set",
            (foregroundInfo.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        )
    }

    @Test
    fun `deterministic unique notification ID matches worker notificationId property`() {
        val expectedId = VideoDownloadWorker.getNotificationId(testIdentifier)
        assertEquals(expectedId, worker.notificationId)
    }

    private fun NotificationCompat.Builder.getContentIntent(): PendingIntent? {
        val field = NotificationCompat.Builder::class.java.getDeclaredField("mContentIntent")
        field.isAccessible = true
        return field.get(this) as? PendingIntent
    }

    private fun NotificationCompat.Builder.getNotificationFlags(): Int {
        val field = NotificationCompat.Builder::class.java.getDeclaredField("mNotification")
        field.isAccessible = true
        val notification = field.get(this) as Notification
        return notification.flags
    }
}
