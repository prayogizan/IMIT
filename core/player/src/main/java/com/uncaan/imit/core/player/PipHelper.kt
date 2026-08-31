package com.uncaan.imit.core.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational

/**
 * Helper utility for managing Android Picture-in-Picture (PiP) mode for video playback.
 *
 * Provides checks for device and OS PiP support, builds [PictureInPictureParams]
 * with default 16:9 widescreen aspect ratio, and safely triggers PiP on an [Activity].
 */
object PipHelper {

    /** Default numerator for 16:9 widescreen video aspect ratio. */
    const val DEFAULT_ASPECT_RATIO_NUMERATOR: Int = 16

    /** Default denominator for 16:9 widescreen video aspect ratio. */
    const val DEFAULT_ASPECT_RATIO_DENOMINATOR: Int = 9

    /**
     * Checks if Picture-in-Picture mode is supported on the current device and Android OS version.
     *
     * Requires Android 8.0 (API 26) or higher and hardware/system feature support.
     *
     * @param context The [Context] used to query package manager system features.
     * @return `true` if PiP is supported, `false` otherwise.
     */
    fun isPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /**
     * Builds [PictureInPictureParams] with the specified aspect ratio.
     *
     * @param numerator Aspect ratio numerator (default is 16).
     * @param denominator Aspect ratio denominator (default is 9).
     * @return Constructed [PictureInPictureParams] on Android O+, or `null` on earlier versions.
     */
    fun buildPipParams(
        numerator: Int = DEFAULT_ASPECT_RATIO_NUMERATOR,
        denominator: Int = DEFAULT_ASPECT_RATIO_DENOMINATOR
    ): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val rational = Rational(numerator, denominator)
        return PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .build()
    }

    /**
     * Attempts to enter Picture-in-Picture mode for the given [Activity].
     *
     * @param activity The target [Activity] to transition into PiP mode.
     * @param numerator Aspect ratio numerator (default is 16).
     * @param denominator Aspect ratio denominator (default is 9).
     * @return `true` if PiP mode was successfully entered, `false` otherwise.
     */
    fun enterPip(
        activity: Activity,
        numerator: Int = DEFAULT_ASPECT_RATIO_NUMERATOR,
        denominator: Int = DEFAULT_ASPECT_RATIO_DENOMINATOR
    ): Boolean {
        if (!isPipSupported(activity)) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = buildPipParams(numerator, denominator) ?: return false
                activity.enterPictureInPictureMode(params)
            } else {
                false
            }
        } catch (_: IllegalStateException) {
            false
        }
    }
}
