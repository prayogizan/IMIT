package com.uncaan.imit.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Utility helper configuring and creating the notification channel for media playback.
 *
 * Media playback notifications on Android 8.0+ (API 26+) require a registered [NotificationChannel].
 * This helper registers a channel configured with [NotificationManager.IMPORTANCE_LOW] to prevent
 * intrusive auditory or vibrational alerts during playback state transitions.
 */
object MediaNotificationChannelHelper {

    /**
     * Unique identifier for the media playback notification channel.
     */
    const val CHANNEL_ID: String = "imit_media_playback"

    /**
     * User-visible name for the media playback notification channel.
     */
    const val CHANNEL_NAME: String = "Video Playback"

    /**
     * User-visible description explaining channel usage in system settings.
     */
    const val CHANNEL_DESCRIPTION: String = "Controls for video playback"

    /**
     * Creates and registers the media playback [NotificationChannel] with the system.
     *
     * Configures:
     * - Importance: [NotificationManager.IMPORTANCE_LOW] (silent playback controls)
     * - Badge display: disabled ([NotificationChannel.setShowBadge] set to `false`)
     * - Lockscreen visibility: [Notification.VISIBILITY_PUBLIC] (full transport controls visible)
     *
     * This operation is idempotent and safe to call on service creation or application startup.
     *
     * @param context The [Context] used to access [NotificationManager].
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
