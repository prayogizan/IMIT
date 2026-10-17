package com.uncaan.imit.core.player

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand

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
 * - Notification channel and custom [IMITMediaNotificationProvider] registration
 * - Custom transport actions (10s skip backward and forward) via [MediaSession.setCustomLayout]
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
     * Configures notification channel, custom media notification provider, audio attributes,
     * buffer thresholds, retry listeners, and custom command layout buttons.
     */
    override fun onCreate() {
        super.onCreate()

        MediaNotificationChannelHelper.createChannel(this)
        setMediaNotificationProvider(IMITMediaNotificationProvider(this))

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

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateSessionActivity(mediaItem)
                    }
                })
            }

        val skipBackCommand = SessionCommand(IMITMediaNotificationProvider.ACTION_SKIP_BACK, Bundle())
        val skipForwardCommand = SessionCommand(IMITMediaNotificationProvider.ACTION_SKIP_FORWARD, Bundle())

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

        val initialSessionActivity = MediaSessionActivityHelper.createSessionActivityPendingIntent(this, null)

        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(IMITMediaSessionCallback())
            .setSessionActivity(initialSessionActivity)
            .setCustomLayout(listOf(skipBackButton, skipForwardButton))
            .build()
    }

    /**
     * Extracts active lecture identifier from [mediaItem] and binds a refreshed
     * immutable deep link [android.app.PendingIntent] to [mediaSession].
     *
     * @param mediaItem The newly transitioned or active [MediaItem].
     */
    internal fun updateSessionActivity(mediaItem: MediaItem?) {
        val identifier = extractIdentifier(mediaItem)
        val pendingIntent = MediaSessionActivityHelper.createSessionActivityPendingIntent(this, identifier)
        mediaSession?.setSessionActivity(pendingIntent)
    }

    /**
     * Resolves unique lecture identifier by inspecting metadata extras first,
     * falling back to [MediaItem.mediaId].
     *
     * @param mediaItem The active [MediaItem], or null.
     * @return Extracted identifier string, or null if unresolvable.
     */
    internal fun extractIdentifier(mediaItem: MediaItem?): String? {
        if (mediaItem == null) return null
        val extraId = mediaItem.mediaMetadata.extras
            ?.getString(MediaSessionActivityHelper.EXTRA_IDENTIFIER)
            ?.takeIf { it.isNotBlank() }
        if (extraId != null) return extraId

        val mediaId = mediaItem.mediaId.takeIf { it.isNotBlank() } ?: return null
        if (mediaId.startsWith("http://") || mediaId.startsWith("https://")) {
            val archiveRegex = Regex("""archive\.org/(?:download|details)/([^/?#]+)""")
            val match = archiveRegex.find(mediaId)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return mediaId
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
     * or the player is idle or ended. If actively playing, the service stays alive for background audio.
     *
     * @param rootIntent The intent that launched the removed task.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null ||
            !currentPlayer.playWhenReady ||
            currentPlayer.mediaItemCount == 0 ||
            currentPlayer.playbackState == Player.STATE_ENDED ||
            currentPlayer.playbackState == Player.STATE_IDLE
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
