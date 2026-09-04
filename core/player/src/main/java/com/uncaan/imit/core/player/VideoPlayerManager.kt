package com.uncaan.imit.core.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

private const val MIN_BUFFER_MS = 15_000
private const val MAX_BUFFER_MS = 50_000
private const val BUFFER_FOR_PLAYBACK_MS = 1_500
private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000

/**
 * Manages the lifecycle and playback controls of an [ExoPlayer] instance.
 *
 * Provides lazy player initialization with conservative [DefaultLoadControl] buffer sizing,
 * automatic exponential backoff retry logic on network connection failures,
 * and common playback operations (play, pause, seek, release).
 *
 * @param context Android [Context] used to construct the [ExoPlayer] instance.
 */
class VideoPlayerManager(private val context: Context) {

    private var _player: ExoPlayer? = null

    /**
     * Lazily obtains or creates the underlying [ExoPlayer] instance.
     *
     * Configures a conservative [DefaultLoadControl] to limit memory consumption (<180MB RAM)
     * and a [Player.Listener] to handle network failures with exponential backoff
     * (up to 3 retries with delays: 1s, 2s, 4s), resetting retry counter on [Player.STATE_READY].
     *
     * @return The active [ExoPlayer] instance.
     */
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
        }
        return _player!!
    }

    /**
     * Sets the media source and starts video playback.
     *
     * Supports remote streaming URLs (HTTP/HTTPS) as well as local file URIs.
     *
     * @param uri The URI string of the video stream or local file to play.
     */
    fun playVideo(uri: String) {
        val player = getPlayer()
        val mediaItem = MediaItem.fromUri(Uri.parse(uri))
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
     * Releases the player resources and clears the internal reference.
     * Should be called when the player is no longer needed (e.g. screen disposed).
     */
    fun release() {
        _player?.release()
        _player = null
    }
}

