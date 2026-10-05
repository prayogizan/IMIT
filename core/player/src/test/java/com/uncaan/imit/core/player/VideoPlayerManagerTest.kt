package com.uncaan.imit.core.player

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying [VideoPlayerManager] client facade behaviors, metadata publishing,
 * and safe transport control handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var playerManager: VideoPlayerManager
    private val mockUri: Uri = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri

        mockContext = mockk(relaxed = true) {
            every { packageName } returns "com.uncaan.imit"
        }
        playerManager = VideoPlayerManager(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Uri::class)
    }

    @Test
    fun `getMediaSession should return null when session is hosted remotely in VideoPlaybackService`() {
        assertNull(playerManager.getMediaSession())
    }

    @Test
    fun `getPlayer should return null when controller is not yet connected`() {
        assertNull(playerManager.getPlayer())
    }

    @Test
    fun `controllerFlow should emit null initially when uninitialized`() {
        assertNull(playerManager.controllerFlow.value)
    }

    @Test
    fun `isPlaying should return false when controller is not connected`() {
        assertFalse(playerManager.isPlaying())
    }

    @Test
    fun `getCurrentPosition should return zero when controller is not connected`() {
        assertEquals(0L, playerManager.getCurrentPosition())
    }

    @Test
    fun `getDuration should return zero when controller is not connected`() {
        assertEquals(0L, playerManager.getDuration())
    }

    @Test
    fun `release should execute safely and reset references when controller is uninitialized`() {
        playerManager.release()
        assertNull(playerManager.getPlayer())
        assertNull(playerManager.controllerFlow.value)
    }

    @Test
    fun `transport controls should execute safely without exceptions when controller is uninitialized`() {
        playerManager.pause()
        playerManager.play()
        playerManager.seekTo(1000L)
        assertFalse(playerManager.isPlaying())
    }

    @Test
    fun `mediaMetadata builder should produce valid metadata values when title and subtitle are provided`() {
        val title = "Test Lecture"
        val subtitle = "Prof. Test"
        val artworkUriStr = "https://archive.org/download/test/thumb.png"

        val metadata = MediaMetadata.Builder()
            .setTitle(title.takeIf { it.isNotBlank() })
            .setArtist(subtitle.takeIf { it.isNotBlank() })
            .apply {
                artworkUriStr.takeIf { it.isNotBlank() }?.let { setArtworkUri(it.toUri()) }
            }
            .build()

        assertEquals(title, metadata.title?.toString())
        assertEquals(subtitle, metadata.artist?.toString())
        assertEquals(mockUri, metadata.artworkUri)
    }

    @Test
    fun `mediaMetadata builder should map blank strings to null fields when empty or blank`() {
        val metadata = MediaMetadata.Builder()
            .setTitle("   ".takeIf { it.isNotBlank() })
            .setArtist("".takeIf { it.isNotBlank() })
            .apply {
                "".takeIf { it.isNotBlank() }?.let { setArtworkUri(it.toUri()) }
            }
            .build()

        assertNull(metadata.title)
        assertNull(metadata.artist)
        assertNull(metadata.artworkUri)
    }

    @Test
    fun `mediaItem builder should encapsulate mediaMetadata and uri when valid parameters provided`() {
        val uri = "https://archive.org/download/test/stream.mp4"
        val title = "Computer Science 101"
        val artist = "MIT OpenCourseWare"

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri.toUri())
            .setMediaMetadata(metadata)
            .build()

        assertNotNull(mediaItem.mediaMetadata)
        assertEquals(title, mediaItem.mediaMetadata.title?.toString())
        assertEquals(artist, mediaItem.mediaMetadata.artist?.toString())
    }

    @Test
    fun `getPlayer should return active controller when controller is connected`() {
        val mockController = mockk<MediaController>(relaxed = true)
        val field = VideoPlayerManager::class.java.getDeclaredField("mediaController")
        field.isAccessible = true
        field.set(playerManager, mockController)

        assertEquals(mockController, playerManager.getPlayer())
    }

    @Test
    fun `playVideo should publish correct MediaMetadata and start playback when controller is connected`() = runTest(testDispatcher) {
        val mockController = mockk<MediaController>(relaxed = true)
        every { mockController.isConnected } returns true
        val field = VideoPlayerManager::class.java.getDeclaredField("mediaController")
        field.isAccessible = true
        field.set(playerManager, mockController)

        val mediaItemSlot = slot<MediaItem>()
        every { mockController.setMediaItem(capture(mediaItemSlot)) } returns Unit

        playerManager.playVideo(
            uri = "https://archive.org/download/test/stream.mp4",
            title = "Introduction to Computer Science",
            subtitle = "Prof. John Doe",
            artworkUri = "https://archive.org/download/test/thumb.png"
        )

        verify(exactly = 1) { mockController.setMediaItem(any()) }
        verify(exactly = 1) { mockController.prepare() }
        verify(exactly = 1) { mockController.play() }

        val captured = mediaItemSlot.captured
        assertEquals("Introduction to Computer Science", captured.mediaMetadata.title?.toString())
        assertEquals("Prof. John Doe", captured.mediaMetadata.artist?.toString())
        assertEquals(mockUri, captured.mediaMetadata.artworkUri)
    }

    @Test
    fun `release should release controller and clear references when controller is connected`() {
        val mockController = mockk<MediaController>(relaxed = true)
        every { mockController.isConnected } returns true
        val field = VideoPlayerManager::class.java.getDeclaredField("mediaController")
        field.isAccessible = true
        field.set(playerManager, mockController)

        playerManager.release()

        verify(exactly = 1) { mockController.release() }
        assertNull(playerManager.getPlayer())
        assertNull(playerManager.controllerFlow.value)
    }

    @Test
    fun `transport controls should delegate to connected controller when controller is active`() {
        val mockController = mockk<MediaController>(relaxed = true)
        every { mockController.isConnected } returns true
        every { mockController.isPlaying } returns true
        every { mockController.currentPosition } returns 5000L
        every { mockController.duration } returns 60000L

        val field = VideoPlayerManager::class.java.getDeclaredField("mediaController")
        field.isAccessible = true
        field.set(playerManager, mockController)

        playerManager.pause()
        verify(exactly = 1) { mockController.pause() }

        playerManager.play()
        verify(exactly = 1) { mockController.play() }

        playerManager.seekTo(12345L)
        verify(exactly = 1) { mockController.seekTo(12345L) }

        assertTrue(playerManager.isPlaying())
        assertEquals(5000L, playerManager.getCurrentPosition())
        assertEquals(60000L, playerManager.getDuration())
    }
}

