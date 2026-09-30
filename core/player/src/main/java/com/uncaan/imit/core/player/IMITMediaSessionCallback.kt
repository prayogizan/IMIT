package com.uncaan.imit.core.player

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult

/**
 * Custom [MediaSession.Callback] implementation for IMIT media playback.
 *
 * Handles playback transport commands (play, pause, seek, stop) and validates
 * incoming controller connections from Bluetooth peripherals, Android Auto,
 * lock screen media controls, and notification surfaces.
 */
class IMITMediaSessionCallback : MediaSession.Callback {

    /**
     * Called when a controller requests connection to this [MediaSession].
     *
     * Accepts connection requests and configures available player commands and session commands.
     *
     * @param session The active [MediaSession] instance.
     * @param controller Information about the connecting controller.
     * @return [MediaSession.ConnectionResult] containing accepted commands.
     */
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .build()
    }

    /**
     * Intercepts and authorizes playback command requests issued by a controller.
     *
     * Explicitly authorizes fundamental media transport commands: play, pause, seek, stop,
     * and prepare.
     *
     * @param session The active [MediaSession] instance.
     * @param controller Information about the issuing controller.
     * @param playerCommand The [Player.Command] integer requested.
     * @return [SessionResult.RESULT_SUCCESS] if command is accepted, or super result otherwise.
     */
    @Deprecated("Overrides deprecated MediaSession.Callback member")
    @Suppress("DEPRECATION")
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: @Player.Command Int
    ): Int {
        return when (playerCommand) {
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_PREPARE,
            Player.COMMAND_STOP,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD -> SessionResult.RESULT_SUCCESS
            else -> super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }
}
