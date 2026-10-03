package com.uncaan.imit.core.player

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(UnstableApi::class)
class IMITMediaNotificationProviderTest {

    private lateinit var mockContext: Context
    private lateinit var provider: IMITMediaNotificationProvider
    private lateinit var mockSession: MediaSession
    private lateinit var mockPlayer: Player

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        provider = IMITMediaNotificationProvider(mockContext)
        mockSession = mockk()
        mockPlayer = mockk(relaxed = true)
        every { mockSession.player } returns mockPlayer
    }

    @Test
    fun `notification ID is 2001 and constants match specifications`() {
        assertEquals(2001, IMITMediaNotificationProvider.NOTIFICATION_ID)
        assertEquals("com.uncaan.imit.SKIP_BACK_10", IMITMediaNotificationProvider.ACTION_SKIP_BACK)
        assertEquals("com.uncaan.imit.SKIP_FORWARD_10", IMITMediaNotificationProvider.ACTION_SKIP_FORWARD)
        assertEquals(10_000L, IMITMediaNotificationProvider.SKIP_INCREMENT_MS)
    }

    @Test
    fun `handleCustomCommand with ACTION_SKIP_BACK seeks back 10 seconds`() {
        every { mockPlayer.currentPosition } returns 25_000L

        val result = provider.handleCustomCommand(
            session = mockSession,
            action = IMITMediaNotificationProvider.ACTION_SKIP_BACK,
            extras = Bundle()
        )

        assertTrue(result)
        verify(exactly = 1) { mockPlayer.seekTo(15_000L) }
    }

    @Test
    fun `handleCustomCommand with ACTION_SKIP_BACK bounds seek target at zero`() {
        every { mockPlayer.currentPosition } returns 4_000L

        val result = provider.handleCustomCommand(
            session = mockSession,
            action = IMITMediaNotificationProvider.ACTION_SKIP_BACK,
            extras = Bundle()
        )

        assertTrue(result)
        verify(exactly = 1) { mockPlayer.seekTo(0L) }
    }

    @Test
    fun `handleCustomCommand with ACTION_SKIP_FORWARD seeks forward 10 seconds`() {
        every { mockPlayer.currentPosition } returns 20_000L
        every { mockPlayer.duration } returns 120_000L

        val result = provider.handleCustomCommand(
            session = mockSession,
            action = IMITMediaNotificationProvider.ACTION_SKIP_FORWARD,
            extras = Bundle()
        )

        assertTrue(result)
        verify(exactly = 1) { mockPlayer.seekTo(30_000L) }
    }

    @Test
    fun `handleCustomCommand with ACTION_SKIP_FORWARD bounds seek target at duration`() {
        every { mockPlayer.currentPosition } returns 115_000L
        every { mockPlayer.duration } returns 120_000L

        val result = provider.handleCustomCommand(
            session = mockSession,
            action = IMITMediaNotificationProvider.ACTION_SKIP_FORWARD,
            extras = Bundle()
        )

        assertTrue(result)
        verify(exactly = 1) { mockPlayer.seekTo(120_000L) }
    }

    @Test
    fun `handleCustomCommand with ACTION_SKIP_FORWARD handles unknown duration gracefully`() {
        every { mockPlayer.currentPosition } returns 10_000L
        every { mockPlayer.duration } returns -1L

        val result = provider.handleCustomCommand(
            session = mockSession,
            action = IMITMediaNotificationProvider.ACTION_SKIP_FORWARD,
            extras = Bundle()
        )

        assertTrue(result)
        verify(exactly = 1) { mockPlayer.seekTo(20_000L) }
    }

    @Test
    fun `handleCustomCommand with unrecognized action returns false`() {
        val result = provider.handleCustomCommand(
            session = mockSession,
            action = "unrecognized_custom_action",
            extras = Bundle()
        )

        assertFalse(result)
        verify(exactly = 0) { mockPlayer.seekTo(any()) }
    }

    @Test
    fun `getNotificationChannelInfo returns configured channel id and name`() {
        val channelInfo = provider.notificationChannelInfo
        assertEquals(MediaNotificationChannelHelper.CHANNEL_ID, channelInfo.id)
        assertEquals(MediaNotificationChannelHelper.CHANNEL_NAME, channelInfo.name)
    }
}
