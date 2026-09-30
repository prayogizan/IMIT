package com.uncaan.imit.core.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

private const val MIN_BUFFER_MS = 15_000
private const val MAX_BUFFER_MS = 50_000
private const val BUFFER_FOR_PLAYBACK_MS = 1_500
private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000

/**
 * Manages the lifecycle and playback controls of an [ExoPlayer] instance and its associated [MediaSession].
 *
 * Provides lazy player initialization with conservative [DefaultLoadControl] buffer sizing,
 * automatic exponential backoff retry logic on network connection failures,
 * [MediaSession] lifecycle integration for system transport controls and metadata publishing,
 * and common playback operations (play, pause, seek, release).
 *
 * @param context Android [Context] used to construct the [ExoPlayer] and [MediaSession] instances.
 */
class VideoPlayerManager(private val context: Context) {

    private var _player: ExoPlayer? = null
    private var _mediaSession: MediaSession? = null

    /**
     * Lazily obtains or creates the underlying [ExoPlayer] instance.
     *
     * Configures a conservative [DefaultLoadControl] to limit memory consumption (<180MB RAM)
     * and a [Player.Listener] to handle network failures with exponential backoff
     * (up to 3 retries with delays: 1s, 2s, 4s), resetting retry counter on [Player.STATE_READY].
     *
     * @return The active [ExoPlayer] instance.
     */
    @OptIn(UnstableApi::class)
    fun getPlayer(): ExoPlayer {
        if (_player == null) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            _player = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
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
                                _player?.prepare()
                                _player?.play()
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

            _mediaSession = MediaSession.Builder(context, _player!!)
                .setCallback(IMITMediaSessionCallback())
                .build()
        }
        return _player!!
    }

    /**
     * Returns the active [MediaSession] instance, if initialized.
     *
     * @return The active [MediaSession] instance, or `null` if the player has not been initialized.
     */
    fun getMediaSession(): MediaSession? = _mediaSession

    /**
     * Sets the media source and starts video playback with optional metadata.
     *
     * Supports remote streaming URLs (HTTP/HTTPS) as well as local file URIs.
     * Attaches [MediaMetadata] (title, artist/course subtitle, and artwork URI)
     * which publishes across connected [MediaSession] controllers such as Bluetooth,
     * lockscreen, and media notifications.
     *
     * @param uri The URI string of the video stream or local file to play.
     * @param title Title of the video or lecture. Defaults to empty string.
     * @param subtitle Subtitle, artist, or course department name. Defaults to empty string.
     * @param artworkUri Optional URI string pointing to artwork or thumbnail image.
     */
    fun playVideo(
        uri: String,
        title: String = "",
        subtitle: String = "",
        artworkUri: String? = null
    ) {
        val player = getPlayer()
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title.takeIf { it.isNotBlank() })
            .setArtist(subtitle.takeIf { it.isNotBlank() })
            .apply {
                artworkUri?.takeIf { it.isNotBlank() }?.let { setArtworkUri(it.toUri()) }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri.toUri())
            .setMediaMetadata(mediaMetadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Pauses current video playback.
     */
    fun pause() {
        _player?.pause()
    }

    /**
     * Resumes video playback if paused.
     */
    fun play() {
        _player?.play()
    }

    /**
     * Checks if the player is currently playing content.
     *
     * @return `true` if playing, `false` otherwise.
     */
    fun isPlaying(): Boolean = _player?.isPlaying == true

    /**
     * Returns the current playback position in milliseconds.
     *
     * @return Current playback position in milliseconds, or 0 if player is not initialized.
     */
    fun getCurrentPosition(): Long = _player?.currentPosition ?: 0L

    /**
     * Returns the total duration of the current media in milliseconds.
     *
     * @return Total duration in milliseconds, or 0 if not available or uninitialized.
     */
    fun getDuration(): Long = _player?.duration ?: 0L

    /**
     * Seeks to a specific playback position.
     *
     * @param positionMs The target position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        _player?.seekTo(positionMs)
    }

    /**
     * Releases the player and [MediaSession] resources and clears internal references.
     * Should be called when the player is no longer needed (e.g. screen disposed).
     */
    fun release() {
        _mediaSession?.release()
        _mediaSession = null
        _player?.release()
        _player = null
    }
}

