package com.uncaan.imit.core.player

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IMITMediaSessionCallbackTest {

    private lateinit var callback: IMITMediaSessionCallback
    private val session: MediaSession = mockk(relaxed = true)
    private val controller: MediaSession.ControllerInfo = mockk(relaxed = true)

    @Before
    fun setUp() {
        io.mockk.mockkStatic(android.text.TextUtils::class)
        every { android.text.TextUtils.equals(any(), any()) } answers {
            val a = firstArg<CharSequence?>()
            val b = secondArg<CharSequence?>()
            if (a === b) true
            else if (a != null && b != null) a.toString() == b.toString()
            else false
        }
        callback = IMITMediaSessionCallback()
    }

    @Test
    fun `onConnect returns accepted connection result with available commands`() {
        val result = callback.onConnect(session, controller)
        assertNotNull(result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onPlayerCommandRequest handles play pause command`() {
        val result = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_PLAY_PAUSE)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onPlayerCommandRequest handles stop command`() {
        val result = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_STOP)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onPlayerCommandRequest handles prepare command`() {
        val result = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_PREPARE)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onPlayerCommandRequest handles seek in current media item command`() {
        val result = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onPlayerCommandRequest handles seek back and forward commands`() {
        val backResult = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_SEEK_BACK)
        assertEquals(SessionResult.RESULT_SUCCESS, backResult)

        val forwardResult = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_SEEK_FORWARD)
        assertEquals(SessionResult.RESULT_SUCCESS, forwardResult)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onPlayerCommandRequest handles seek to default position command`() {
        val result = callback.onPlayerCommandRequest(session, controller, Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
    }

    @Test
    fun `onConnect registers custom session commands for skip back and forward`() {
        val result = callback.onConnect(session, controller)
        assertNotNull(result)
        val availableCommands = result.availableSessionCommands
        assertNotNull(availableCommands)
        val skipBackCommand = SessionCommand(
            IMITMediaNotificationProvider.ACTION_SKIP_BACK,
            Bundle()
        )
        val skipForwardCommand = SessionCommand(
            IMITMediaNotificationProvider.ACTION_SKIP_FORWARD,
            Bundle()
        )
        assertTrue(availableCommands.contains(skipBackCommand))
        assertTrue(availableCommands.contains(skipForwardCommand))
    }

    @Test
    fun `onCustomCommand with ACTION_SKIP_BACK seeks player back 10 seconds`() {
        val mockPlayer = mockk<Player>(relaxed = true)
        every { session.player } returns mockPlayer
        every { mockPlayer.currentPosition } returns 30_000L

        val command = SessionCommand(
            IMITMediaNotificationProvider.ACTION_SKIP_BACK,
            Bundle()
        )
        val future = callback.onCustomCommand(session, controller, command, Bundle())

        assertNotNull(future)
        val sessionResult = future.get()
        assertEquals(SessionResult.RESULT_SUCCESS, sessionResult.resultCode)
        verify(exactly = 1) { mockPlayer.seekTo(20_000L) }
    }

    @Test
    fun `onCustomCommand with ACTION_SKIP_FORWARD seeks player forward 10 seconds`() {
        val mockPlayer = mockk<Player>(relaxed = true)
        every { session.player } returns mockPlayer
        every { mockPlayer.currentPosition } returns 30_000L
        every { mockPlayer.duration } returns 100_000L

        val command = SessionCommand(
            IMITMediaNotificationProvider.ACTION_SKIP_FORWARD,
            Bundle()
        )
        val future = callback.onCustomCommand(session, controller, command, Bundle())

        assertNotNull(future)
        val sessionResult = future.get()
        assertEquals(SessionResult.RESULT_SUCCESS, sessionResult.resultCode)
        verify(exactly = 1) { mockPlayer.seekTo(40_000L) }
    }
}


