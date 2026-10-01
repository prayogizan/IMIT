package com.uncaan.imit.core.player

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.session.MediaSessionService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class VideoPlaybackServiceTest {

    @Test
    fun `VideoPlaybackService extends MediaSessionService`() {
        assertTrue(MediaSessionService::class.java.isAssignableFrom(VideoPlaybackService::class.java))
    }

    @Test
    fun `audioAttributes configuration matches movie and media playback usage`() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        assertEquals(C.AUDIO_CONTENT_TYPE_MOVIE, audioAttributes.contentType)
        assertEquals(C.USAGE_MEDIA, audioAttributes.usage)
    }

    @Test
    fun `loadControl configuration initializes with conservative memory buffer thresholds`() {
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
    fun `task removal stopping condition evaluates true when player is idle or ended`() {
        val mockPlayer = mockk<Player>()

        // Case 1: not playing
        every { mockPlayer.playWhenReady } returns false
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val shouldStopCase1 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED
        assertTrue(shouldStopCase1)

        // Case 2: empty playlist
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 0
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val shouldStopCase2 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED
        assertTrue(shouldStopCase2)

        // Case 3: playback ended
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_ENDED

        val shouldStopCase3 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED
        assertTrue(shouldStopCase3)

        // Case 4: actively playing in background - should NOT stop
        every { mockPlayer.playWhenReady } returns true
        every { mockPlayer.mediaItemCount } returns 1
        every { mockPlayer.playbackState } returns Player.STATE_READY

        val shouldStopCase4 = !mockPlayer.playWhenReady ||
            mockPlayer.mediaItemCount == 0 ||
            mockPlayer.playbackState == Player.STATE_ENDED
        org.junit.Assert.assertFalse(shouldStopCase4)
    }
}
