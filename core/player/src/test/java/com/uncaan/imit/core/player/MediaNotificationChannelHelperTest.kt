package com.uncaan.imit.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit test suite verifying [MediaNotificationChannelHelper] channel constants,
 * Oreo (API 26+) channel registration, and pre-Oreo bypass.
 */
class MediaNotificationChannelHelperTest {

    @Test
    fun `channel constants should match required acceptance criteria`() {
        assertEquals("imit_media_playback", MediaNotificationChannelHelper.CHANNEL_ID)
        assertEquals("Video Playback", MediaNotificationChannelHelper.CHANNEL_NAME)
        assertEquals("Controls for video playback", MediaNotificationChannelHelper.CHANNEL_DESCRIPTION)
    }

    @Test
    fun `createChannel should register notification channel with correct configuration on API 26+`() {
        val mockManager = mockk<NotificationManager>(relaxed = true)
        val mockContext = mockk<Context>()
        every { mockContext.getSystemService(NotificationManager::class.java) } returns mockManager

        val channelSlot = slot<NotificationChannel>()
        every { mockManager.createNotificationChannel(capture(channelSlot)) } returns Unit

        MediaNotificationChannelHelper.createChannel(mockContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verify(exactly = 1) { mockManager.createNotificationChannel(any()) }
            val captured = channelSlot.captured
            assertEquals(MediaNotificationChannelHelper.CHANNEL_ID, captured.id)
            assertEquals(MediaNotificationChannelHelper.CHANNEL_NAME, captured.name)
            assertEquals(NotificationManager.IMPORTANCE_LOW, captured.importance)
            assertEquals(MediaNotificationChannelHelper.CHANNEL_DESCRIPTION, captured.description)
            assertFalse(captured.canShowBadge())
            assertEquals(Notification.VISIBILITY_PUBLIC, captured.lockscreenVisibility)
        }
    }

    @Test
    fun `createChannel should skip channel registration on pre-API-26 platforms when SDK_INT is below Oreo`() {
        val mockContext = mockk<Context>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            MediaNotificationChannelHelper.createChannel(mockContext)
            verify(exactly = 0) { mockContext.getSystemService(any()) }
        }
    }
}
