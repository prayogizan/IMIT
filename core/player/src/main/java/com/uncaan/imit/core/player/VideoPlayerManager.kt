package com.uncaan.imit.core.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Client facade managing interaction with [VideoPlaybackService] via [MediaController].
 *
 * Rather than holding direct ownership of [androidx.media3.exoplayer.ExoPlayer] and
 * [androidx.media3.session.MediaSession], this facade acts as a client that establishes
 * an asynchronous connection to the background [VideoPlaybackService] using a [SessionToken].
 *
 * Provides media playback triggers, metadata population, transport controls (play, pause, seek),
 * and lifecycle detachment without stopping background audio.
 *
 * @param context Android [Context] used to create the [SessionToken] and [MediaController].
 */
class VideoPlayerManager(private val context: Context) {

    private val sessionToken by lazy {
        SessionToken(
            context,
            ComponentName(context, VideoPlaybackService::class.java)
        )
    }

    private val mutex = Mutex()
    private var mediaController: MediaController? = null

    private val _controllerFlow = MutableStateFlow<MediaController?>(null)

    /**
     * Observable [StateFlow] emitting the active [MediaController] once connected, or `null`.
     */
    val controllerFlow: StateFlow<MediaController?> = _controllerFlow.asStateFlow()

    /**
     * Lazily connects to or retrieves the active [MediaController] bound to [VideoPlaybackService].
     *
     * Ensures thread-safe single initialization on the main thread via [Mutex].
     *
     * @return The connected [MediaController] instance.
     */
    suspend fun getController(): MediaController = withContext(Dispatchers.Main) {
        mutex.withLock {
            val current = mediaController
            if (current != null && current.isConnected) {
                return@withContext current
            }

            val future = MediaController.Builder(context, sessionToken).buildAsync()
            val controller = future.await()
            mediaController = controller
            _controllerFlow.value = controller
            controller
        }
    }

    /**
     * Returns the currently connected [Player] instance if available, or `null` if not yet connected.
     *
     * @return The active [Player] instance or `null`.
     */
    fun getPlayer(): Player? = mediaController

    /**
     * Legacy getter preserved for backward compatibility.
     *
     * The [androidx.media3.session.MediaSession] is now owned exclusively by [VideoPlaybackService].
     *
     * @return Always returns `null` as the session is hosted within the remote service.
     */
    fun getMediaSession(): androidx.media3.session.MediaSession? = null

    /**
     * Sets the media source and starts video playback with metadata.
     *
     * Asynchronously connects to [VideoPlaybackService] via [getController],
     * builds the [MediaItem] with [MediaMetadata], prepares playback, and starts playing.
     *
     * @param uri The URI string of the video stream or local file to play.
     * @param title Title of the video or lecture. Defaults to empty string.
     * @param subtitle Subtitle, artist, or course department name. Defaults to empty string.
     * @param artworkUri Optional URI string pointing to artwork or thumbnail image.
     */
    suspend fun playVideo(
        uri: String,
        title: String = "",
        subtitle: String = "",
        artworkUri: String? = null
    ) {
        val controller = getController()
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

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    /**
     * Pauses current media playback via the connected controller.
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * Resumes media playback via the connected controller.
     */
    fun play() {
        mediaController?.play()
    }

    /**
     * Checks if the connected controller is currently playing content.
     *
     * @return `true` if playing, `false` otherwise.
     */
    fun isPlaying(): Boolean = mediaController?.isPlaying == true

    /**
     * Returns the current playback position in milliseconds.
     *
     * @return Current playback position in milliseconds, or 0 if controller is not connected.
     */
    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L

    /**
     * Returns the total duration of the current media in milliseconds.
     *
     * @return Total duration in milliseconds, or 0 if not available or uninitialized.
     */
    fun getDuration(): Long = mediaController?.duration ?: 0L

    /**
     * Seeks to a specific playback position.
     *
     * @param positionMs The target position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    /**
     * Releases the [MediaController] connection and resets internal controller state.
     *
     * Note: Releasing the controller does not terminate the background [VideoPlaybackService]
     * if active playback is ongoing.
     */
    fun release() {
        mediaController?.release()
        mediaController = null
        _controllerFlow.value = null
    }

    /**
     * Awaits completion of a [ListenableFuture] in a coroutine-friendly cancellable manner.
     */
    private suspend fun <T> ListenableFuture<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    try {
                        continuation.resume(get())
                    } catch (e: ExecutionException) {
                        continuation.resumeWithException(e.cause ?: e)
                    } catch (e: CancellationException) {
                        continuation.cancel(e)
                    } catch (e: Throwable) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
            continuation.invokeOnCancellation {
                cancel(true)
            }
        }
}
