package com.uncaan.imit.core.player

import android.os.Build

/**
 * Helper utility for managing foreground playback service availability and lifecycle gating.
 *
 * Provides API version checks to determine if the foreground [VideoPlaybackService]
 * should be used for background media playback or if in-Activity playback fallback applies.
 */
object PlaybackServiceHelper {

    /**
     * Checks if the foreground playback service should be used on the current device.
     *
     * Android 8.0 (API 26) introduced background execution limits and required foreground
     * services with notification channels for ongoing playback. On devices running Android 8.0+
     * (API 26+), background playback uses [VideoPlaybackService]. On earlier versions (API < 26),
     * the app gracefully falls back to in-Activity playback without a foreground service.
     *
     * @return `true` if API level is 26 or higher, `false` otherwise.
     */
    fun shouldUseBackgroundService(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}

/**
 * Convenience top-level function delegating to [PlaybackServiceHelper.shouldUseBackgroundService].
 *
 * @return `true` if foreground playback service is supported (API 26+), `false` otherwise.
 */
fun shouldUseBackgroundService(): Boolean = PlaybackServiceHelper.shouldUseBackgroundService()
