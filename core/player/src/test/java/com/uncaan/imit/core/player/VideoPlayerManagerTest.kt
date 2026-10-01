package com.uncaan.imit.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class VideoPlayerManagerTest {

    private lateinit var mockContext: Context
    private lateinit var playerManager: VideoPlayerManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true) {
            every { packageName } returns "com.uncaan.imit"
        }
        playerManager = VideoPlayerManager(mockContext)
    }

    @Test
    fun `getMediaSession returns null because session is hosted in VideoPlaybackService`() {
        assertNull(playerManager.getMediaSession())
    }

    @Test
    fun `getPlayer returns null before controller is connected`() {
        assertNull(playerManager.getPlayer())
    }

    @Test
    fun `controllerFlow emits null initially`() {
        assertNull(playerManager.controllerFlow.value)
    }

    @Test
    fun `isPlaying returns false before controller is connected`() {
        assertFalse(playerManager.isPlaying())
    }

    @Test
    fun `getCurrentPosition returns zero before controller is connected`() {
        assertEquals(0L, playerManager.getCurrentPosition())
    }

    @Test
    fun `getDuration returns zero before controller is connected`() {
        assertEquals(0L, playerManager.getDuration())
    }

    @Test
    fun `release executes safely when controller is not initialized`() {
        playerManager.release()
        assertNull(playerManager.getPlayer())
        assertNull(playerManager.controllerFlow.value)
    }

    @Test
    fun `pause, play, and seekTo execute safely when controller is not initialized`() {
        playerManager.pause()
        playerManager.play()
        playerManager.seekTo(1000L)
        assertFalse(playerManager.isPlaying())
    }

    @Test
    fun `mediaMetadata builder produces valid metadata values`() {
        val title = "Test Lecture"
        val subtitle = "Prof. Test"

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .build()

        assertEquals(title, metadata.title?.toString())
        assertEquals(subtitle, metadata.artist?.toString())
    }

    @Test
    fun `mediaItem builder with mediaMetadata retains metadata and configuration`() {
        val title = "Computer Science 101"
        val artist = "MIT OpenCourseWare"

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaMetadata(metadata)
            .build()

        assertNotNull(mediaItem.mediaMetadata)
        assertEquals(title, mediaItem.mediaMetadata.title?.toString())
        assertEquals(artist, mediaItem.mediaMetadata.artist?.toString())
    }
}
