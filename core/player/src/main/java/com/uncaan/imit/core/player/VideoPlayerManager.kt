package com.uncaan.imit.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class VideoPlayerManager(private val context: Context) {
    private var _player: ExoPlayer? = null

    fun getPlayer(): ExoPlayer {
        if (_player == null) {
            _player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    private var retryCount = 0
                    private val maxRetries = 3

                    override fun onPlayerError(error: PlaybackException) {
                        if (retryCount < maxRetries &&
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        ) {
                            retryCount++
                            val delayMs = 1000L * (1L shl (retryCount - 1))
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                _player?.prepare()
                                _player?.play()
                            }, delayMs)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) retryCount = 0
                    }
                })
            }
        }
        return _player!!
    }

    fun playVideo(uri: String) {
        val player = getPlayer()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    fun release() {
        _player?.release()
        _player = null
    }

    fun getCurrentPosition(): Long = _player?.currentPosition ?: 0L
    fun seekTo(positionMs: Long) = _player?.seekTo(positionMs)
}
