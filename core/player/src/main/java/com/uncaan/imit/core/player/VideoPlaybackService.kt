package com.uncaan.imit.core.player

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

private const val MIN_BUFFER_MS = 15_000
private const val MAX_BUFFER_MS = 50_000
private const val BUFFER_FOR_PLAYBACK_MS = 1_500
private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000

/**
 * Foreground [MediaSessionService] managing background media playback and [MediaSession] lifecycle.
 *
 * Hosts the [ExoPlayer] instance and handles:
 * - Background audio/video playback surviving Activity recreation and destruction
 * - Conservative [DefaultLoadControl] buffer sizing to maintain memory limit (<180MB RAM)
 * - Automatic audio focus gain/loss and "becoming noisy" headset disconnect handling
 * - Exponential backoff auto-retry on network disconnect errors (up to 3 attempts: 1s, 2s, 4s)
 * - Self-stopping lifecycle on user task removal from recent apps when idle or ended
 * - Teardown of player and session resources when the service is destroyed
 */
@OptIn(UnstableApi::class)
class VideoPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    /**
     * Initializes [ExoPlayer] and binds it to a new [MediaSession].
     *
     * Configures audio attributes, audio focus management, noise handling,
     * buffer thresholds, and network failure retry listeners.
     */
    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    private var retryCount = 0
                    private val maxRetries = 3

                    override fun onPlayerError(error: PlaybackException) {
                        if (retryCount < maxRetries &&
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        ) {
                            retryCount++
                            val delayMs = 1000L * (1L shl (retryCount - 1))
                            Handler(Looper.getMainLooper()).postDelayed({
                                player?.prepare()
                                player?.play()
                            }, delayMs)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            retryCount = 0
                        }
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(IMITMediaSessionCallback())
            .build()
    }

    /**
     * Returns the active [MediaSession] instance for connecting media controllers.
     *
     * @param controllerInfo Information about the connecting controller.
     * @return The active [MediaSession] or `null` if uninitialized.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Handles task removal from Android recents.
     *
     * Self-stops the service if playback is not currently active, the media playlist is empty,
     * or playback has completed. If actively playing, the service stays alive for background audio.
     *
     * @param rootIntent The intent that launched the removed task.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null ||
            !currentPlayer.playWhenReady ||
            currentPlayer.mediaItemCount == 0 ||
            currentPlayer.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    /**
     * Releases [ExoPlayer] and [MediaSession] resources and cleans up references.
     */
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
