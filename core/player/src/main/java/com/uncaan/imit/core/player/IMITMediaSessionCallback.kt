package com.uncaan.imit.core.player

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Custom [MediaSession.Callback] implementation for IMIT media playback.
 *
 * Handles playback transport commands (play, pause, seek, stop) and validates
 * incoming controller connections from Bluetooth peripherals, Android Auto,
 * lock screen media controls, and notification surfaces.
 */
@OptIn(UnstableApi::class)
class IMITMediaSessionCallback : MediaSession.Callback {

    /**
     * Called when a controller requests connection to this [MediaSession].
     *
     * Accepts connection requests and configures available player commands and custom session
     * commands, including 10s skip backward and forward commands.
     *
     * @param session The active [MediaSession] instance.
     * @param controller Information about the connecting controller.
     * @return [MediaSession.ConnectionResult] containing accepted commands.
     */
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(SessionCommand(IMITMediaNotificationProvider.ACTION_SKIP_BACK, Bundle()))
            .add(SessionCommand(IMITMediaNotificationProvider.ACTION_SKIP_FORWARD, Bundle()))
            .build()

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailableSessionCommands(sessionCommands)
            .build()
    }

    /**
     * Intercepts and executes custom commands issued by external controllers (e.g. Android 13+ System UI).
     *
     * @param session The active [MediaSession].
     * @param controller Information about the issuing controller.
     * @param customCommand The [SessionCommand] to execute.
     * @param args Custom argument bundle.
     * @return [ListenableFuture] resolving to [SessionResult.RESULT_SUCCESS] if handled.
     */
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            IMITMediaNotificationProvider.ACTION_SKIP_BACK -> {
                val current = session.player.currentPosition
                session.player.seekTo(maxOf(0L, current - IMITMediaNotificationProvider.SKIP_INCREMENT_MS))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            IMITMediaNotificationProvider.ACTION_SKIP_FORWARD -> {
                val current = session.player.currentPosition
                val duration = session.player.duration
                val target = if (duration > 0) {
                    minOf(duration, current + IMITMediaNotificationProvider.SKIP_INCREMENT_MS)
                } else {
                    current + IMITMediaNotificationProvider.SKIP_INCREMENT_MS
                }
                session.player.seekTo(target)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }
        return super.onCustomCommand(session, controller, customCommand, args)
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
