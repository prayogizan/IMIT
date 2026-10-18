package com.uncaan.imit.core.download

import android.app.PendingIntent
import android.content.Context

/**
 * Interface providing deep-link [PendingIntent] instances for video download notifications.
 *
 * Inverts the dependency between the core download engine and application-level navigation,
 * allowing background workers to configure notification click actions without introducing
 * circular dependencies between `:core:download` and `:app`.
 */
interface DownloadNotificationNavigator {

    /**
     * Creates a [PendingIntent] directing the user to the active downloads overview screen.
     *
     * @param context Application or worker context.
     * @return Immutable [PendingIntent] with synthesized back stack.
     */
    fun createDownloadsPendingIntent(context: Context): PendingIntent

    /**
     * Creates a [PendingIntent] directing the user to the detail screen for a specific video.
     *
     * @param context Application or worker context.
     * @param identifier Unique Archive.org item identifier.
     * @return Immutable [PendingIntent] with synthesized back stack.
     */
    fun createDetailsPendingIntent(context: Context, identifier: String): PendingIntent
}
