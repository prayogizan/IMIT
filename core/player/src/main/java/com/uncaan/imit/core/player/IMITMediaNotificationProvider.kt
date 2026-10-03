package com.uncaan.imit.core.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

/**
 * Custom [MediaNotification.Provider] configuring rich notification media controls for IMIT video playback.
 *
 * Implements Android media notification standards:
 * - Posts to [MediaNotificationChannelHelper.CHANNEL_ID]
 * - Uses reserved [NOTIFICATION_ID] of 2001 to prevent collision with background download notifications
 * - Provides 4 custom and media action buttons: Skip Back 10s, Play/Pause toggle, Skip Forward 10s, Stop
 * - Binds compact view actions (indices 0, 1, 2) via [MediaStyle.setShowActionsInCompactView]
 * - Displays lecture title, course/instructor subtitle, and asynchronously loaded thumbnail artwork
 * - Toggles ongoing notification status (non-dismissible during active playback, dismissible when paused)
 * - Wires swipe dismissal intent to stop playback and service lifecycle
 * - Intercepts and executes custom skip commands through [handleCustomCommand]
 *
 * @param context Android [Context] used to resolve resources, channels, and main executor.
 */
@OptIn(UnstableApi::class)
class IMITMediaNotificationProvider(
    private val context: Context
) : MediaNotification.Provider {

    companion object {
        /**
         * Notification ID dedicated exclusively to the media playback foreground service.
         */
        const val NOTIFICATION_ID: Int = 2001

        /**
         * Custom action identifier for rewinding playback by 10 seconds.
         */
        const val ACTION_SKIP_BACK: String = "com.uncaan.imit.SKIP_BACK_10"

        /**
         * Custom action identifier for advancing playback by 10 seconds.
         */
        const val ACTION_SKIP_FORWARD: String = "com.uncaan.imit.SKIP_FORWARD_10"

        /**
         * Increment step in milliseconds for skip back and skip forward transport actions.
         */
        const val SKIP_INCREMENT_MS: Long = 10_000L
    }

    /**
     * Builds and returns a [MediaNotification] reflecting the active player state.
     *
     * @param mediaSession The active [MediaSession] instance.
     * @param customLayout Immutable list of custom command buttons.
     * @param actionFactory Factory used to generate standard media and custom notification actions.
     * @param onNotificationChangedCallback Callback notified when asynchronous resources (e.g. artwork) resolve.
     * @return Fully configured [MediaNotification] instance with ID [NOTIFICATION_ID].
     */
    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        MediaNotificationChannelHelper.createChannel(context)
        val player = mediaSession.player

        val builder = NotificationCompat.Builder(context, MediaNotificationChannelHelper.CHANNEL_ID)

        // 1. Skip Back 10s
        val skipBackAction = actionFactory.createCustomAction(
            mediaSession,
            IconCompat.createWithResource(context, R.drawable.ic_replay_10),
            "Skip Back 10s",
            ACTION_SKIP_BACK,
            Bundle()
        )
        builder.addAction(skipBackAction)

        // 2. Play / Pause Toggle
        val isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val playPauseAction = actionFactory.createMediaAction(
            mediaSession,
            IconCompat.createWithResource(context, playPauseIcon),
            playPauseTitle,
            Player.COMMAND_PLAY_PAUSE
        )
        builder.addAction(playPauseAction)

        // 3. Skip Forward 10s
        val skipForwardAction = actionFactory.createCustomAction(
            mediaSession,
            IconCompat.createWithResource(context, R.drawable.ic_forward_10),
            "Skip Forward 10s",
            ACTION_SKIP_FORWARD,
            Bundle()
        )
        builder.addAction(skipForwardAction)

        // 4. Stop Action
        val stopAction = actionFactory.createMediaAction(
            mediaSession,
            IconCompat.createWithResource(context, R.drawable.ic_stop),
            "Stop",
            Player.COMMAND_STOP
        )
        builder.addAction(stopAction)

        // MediaStyle configuration with compact view indices (Skip Back, Play/Pause, Skip Forward)
        val mediaStyle = MediaStyle(mediaSession)
            .setShowActionsInCompactView(0, 1, 2)
            .setCancelButtonIntent(actionFactory.createNotificationDismissalIntent(mediaSession))

        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "MIT OpenCourseWare"
        val subtitle = metadata.artist?.toString() ?: metadata.subtitle?.toString() ?: ""

        builder
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_play_arrow)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(mediaSession.sessionActivity)
            .setDeleteIntent(actionFactory.createNotificationDismissalIntent(mediaSession))
            .setOngoing(isPlaying)

        // Progress chronometer during active playback
        if (isPlaying && player.currentPosition >= 0) {
            builder.setShowWhen(true)
                .setUsesChronometer(true)
        }

        // Asynchronous thumbnail artwork extraction
        val bitmapFuture = mediaSession.bitmapLoader.loadBitmapFromMetadata(metadata)
        if (bitmapFuture != null) {
            if (bitmapFuture.isDone) {
                try {
                    builder.setLargeIcon(Futures.getDone(bitmapFuture))
                } catch (e: Exception) {
                    // Graceful fallback to default icon
                }
            } else {
                Futures.addCallback(
                    bitmapFuture,
                    object : FutureCallback<Bitmap> {
                        override fun onSuccess(result: Bitmap?) {
                            result?.let {
                                builder.setLargeIcon(it)
                                onNotificationChangedCallback.onNotificationChanged(
                                    MediaNotification(NOTIFICATION_ID, builder.build())
                                )
                            }
                        }

                        override fun onFailure(t: Throwable) {
                            // Ignored: notification retains default layout without large icon
                        }
                    },
                    ContextCompat.getMainExecutor(context)
                )
            }
        }

        return MediaNotification(NOTIFICATION_ID, builder.build())
    }

    /**
     * Intercepts and executes custom transport action commands sent by notification buttons.
     *
     * Handles:
     * - [ACTION_SKIP_BACK]: Replays playback by 10 seconds (bounded at 0ms).
     * - [ACTION_SKIP_FORWARD]: Advances playback by 10 seconds (bounded at player duration).
     *
     * @param session The active [MediaSession].
     * @param action The custom action string.
     * @param extras Additional parameter bundle.
     * @return `true` if the action was handled, `false` otherwise.
     */
    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        when (action) {
            ACTION_SKIP_BACK -> {
                val current = session.player.currentPosition
                session.player.seekTo(maxOf(0L, current - SKIP_INCREMENT_MS))
                return true
            }
            ACTION_SKIP_FORWARD -> {
                val current = session.player.currentPosition
                val duration = session.player.duration
                val target = if (duration > 0) {
                    minOf(duration, current + SKIP_INCREMENT_MS)
                } else {
                    current + SKIP_INCREMENT_MS
                }
                session.player.seekTo(target)
                return true
            }
        }
        return false
    }

    /**
     * Returns notification channel properties used by the system when posting notifications.
     *
     * @return [MediaNotification.Provider.NotificationChannelInfo] with channel ID and channel name.
     */
    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return MediaNotification.Provider.NotificationChannelInfo(
            MediaNotificationChannelHelper.CHANNEL_ID,
            MediaNotificationChannelHelper.CHANNEL_NAME
        )
    }
}
