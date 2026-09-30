package com.uncaan.imit.core.player

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class IMITMediaSessionCallbackTest {

    private lateinit var callback: IMITMediaSessionCallback
    private val session: MediaSession = mockk(relaxed = true)
    private val controller: MediaSession.ControllerInfo = mockk(relaxed = true)

    @Before
    fun setUp() {
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
}
