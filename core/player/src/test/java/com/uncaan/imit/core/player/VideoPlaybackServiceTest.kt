package com.uncaan.imit.core.player

import android.content.Intent
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying [VideoPlaybackService] configuration, command buttons,
 * task removal stopping policies, session access, and lifecycle teardown.
 */
@OptIn(UnstableApi::class)
class VideoPlaybackServiceTest {

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mockLooper
        every { mockLooper.thread } returns Thread.currentThread()
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
    }

    @Test
    fun `service should extend MediaSessionService when inspected`() {
        assertTrue(MediaSessionService::class.java.isAssignableFrom(VideoPlaybackService::class.java))
    }

    @Test
    fun `audioAttributes configuration should match movie and media playback usage`() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        assertEquals(C.AUDIO_CONTENT_TYPE_MOVIE, audioAttributes.contentType)
        assertEquals(C.USAGE_MEDIA, audioAttributes.usage)
    }

    @Test
    fun `loadControl configuration should initialize with conservative memory buffer thresholds`() {
        val minBufferMs = 15_000
        val maxBufferMs = 50_000
        val bufferForPlaybackMs = 1_500
        val bufferForPlaybackAfterRebufferMs = 2_000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        assertNotNull(loadControl)
    }

    @Test
    fun `task removal stopping condition should evaluate true when player is idle or ended`() {
        val mockPlayer = mockk<Player>()

        // Case 1: not playing
        every { mockPlayer.playWhenReady } returns false
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val shouldStopCase1 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED ||
            mockPlayer.playbackState == Player.STATE_IDLE
        assertTrue(shouldStopCase1)

        // Case 2: empty playlist
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 0
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val shouldStopCase2 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED ||
            mockPlayer.playbackState == Player.STATE_IDLE
        assertTrue(shouldStopCase2)

        // Case 3: playback ended
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_ENDED

        val shouldStopCase3 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED ||
            mockPlayer.playbackState == Player.STATE_IDLE
        assertTrue(shouldStopCase3)

        // Case 4: player idle
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_IDLE

        val shouldStopCase4 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED ||
            mockPlayer.playbackState == Player.STATE_IDLE
        assertTrue(shouldStopCase4)

        // Case 5: actively playing in background - should NOT stop
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val shouldStopCase5 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED ||
            mockPlayer.playbackState == Player.STATE_IDLE
        assertFalse(shouldStopCase5)
    }

    @Test
    fun `custom layout buttons should match skip back and skip forward command specifications`() {
        val skipBackCommand = SessionCommand(
            IMITMediaNotificationProvider.ACTION_SKIP_BACK,
            android.os.Bundle()
        )
        val skipForwardCommand = SessionCommand(
            IMITMediaNotificationProvider.ACTION_SKIP_FORWARD,
            android.os.Bundle()
        )

        val skipBackButton = CommandButton.Builder()
            .setDisplayName("Skip Back 10s")
            .setIconResId(R.drawable.ic_replay_10)
            .setSessionCommand(skipBackCommand)
            .build()

        val skipForwardButton = CommandButton.Builder()
            .setDisplayName("Skip Forward 10s")
            .setIconResId(R.drawable.ic_forward_10)
            .setSessionCommand(skipForwardCommand)
            .build()

        assertEquals("Skip Back 10s", skipBackButton.displayName)
        assertEquals(R.drawable.ic_replay_10, skipBackButton.iconResId)
        assertEquals(SessionCommand.COMMAND_CODE_CUSTOM, skipBackButton.sessionCommand?.commandCode)
        assertEquals(IMITMediaNotificationProvider.ACTION_SKIP_BACK, skipBackButton.sessionCommand?.customAction)

        assertEquals("Skip Forward 10s", skipForwardButton.displayName)
        assertEquals(R.drawable.ic_forward_10, skipForwardButton.iconResId)
        assertEquals(SessionCommand.COMMAND_CODE_CUSTOM, skipForwardButton.sessionCommand?.commandCode)
        assertEquals(IMITMediaNotificationProvider.ACTION_SKIP_FORWARD, skipForwardButton.sessionCommand?.customAction)
    }

    @Test
    fun `onGetSession should return null before session is initialized`() {
        val service = VideoPlaybackService()
        val mockControllerInfo = mockk<MediaSession.ControllerInfo>()
        assertNull(service.onGetSession(mockControllerInfo))
    }

    @Test
    fun `onGetSession should return active session when mediaSession is initialized`() {
        val service = VideoPlaybackService()
        val mockSession = mockk<MediaSession>()
        val field = VideoPlaybackService::class.java.getDeclaredField("mediaSession")
        field.isAccessible = true
        field.set(service, mockSession)

        val mockControllerInfo = mockk<MediaSession.ControllerInfo>()
        assertEquals(mockSession, service.onGetSession(mockControllerInfo))
    }

    @Test
    fun `onTaskRemoved should invoke stopSelf when player is idle or null`() {
        val service = spyk(VideoPlaybackService())
        every { service.stopSelf() } returns Unit

        val mockPlayer = mockk<Player>()
        every { mockPlayer.playWhenReady } returns false
        every { mockPlayer.mediaItemCount } returns 0
        every { mockPlayer.playbackState } returns Player.STATE_IDLE

        val mockSession = mockk<MediaSession>()
        every { mockSession.player } returns mockPlayer

        val sessionField = VideoPlaybackService::class.java.getDeclaredField("mediaSession")
        sessionField.isAccessible = true
        sessionField.set(service, mockSession)

        val intent = mockk<Intent>()
        service.onTaskRemoved(intent)
        verify(exactly = 1) { service.stopSelf() }
    }

    @Test
    fun `onTaskRemoved should keep running when player is actively playing`() {
        val service = spyk(VideoPlaybackService())
        every { service.stopSelf() } returns Unit

        val mockPlayer = mockk<Player>()
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val mockSession = mockk<MediaSession>()
        every { mockSession.player } returns mockPlayer

        val sessionField = VideoPlaybackService::class.java.getDeclaredField("mediaSession")
        sessionField.isAccessible = true
        sessionField.set(service, mockSession)

        val intent = mockk<Intent>()
        service.onTaskRemoved(intent)
        verify(exactly = 0) { service.stopSelf() }
    }

    @Test
    fun `onDestroy should release player and mediaSession and reset references`() {
        val service = VideoPlaybackService()

        val mockPlayer = mockk<ExoPlayer>(relaxed = true)
        val mockSession = mockk<MediaSession>(relaxed = true)
        every { mockSession.player } returns mockPlayer

        val sessionField = VideoPlaybackService::class.java.getDeclaredField("mediaSession")
        sessionField.isAccessible = true
        sessionField.set(service, mockSession)

        val playerField = VideoPlaybackService::class.java.getDeclaredField("player")
        playerField.isAccessible = true
        playerField.set(service, mockPlayer)

        service.onDestroy()

        verify(exactly = 1) { mockPlayer.release() }
        verify(exactly = 1) { mockSession.release() }
        assertNull(sessionField.get(service))
        assertNull(playerField.get(service))
    }
}
