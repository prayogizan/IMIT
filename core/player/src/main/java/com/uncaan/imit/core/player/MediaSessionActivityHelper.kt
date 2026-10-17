package com.uncaan.imit.core.player

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Utility helper providing factory methods for generating [PendingIntent] instances
 * bound to Media3 [androidx.media3.session.MediaSession.setSessionActivity].
 *
 * Utilizes [TaskStackBuilder] to synthesize a full parent back stack for deep link URI
 * `imit://app/details/{identifier}`, ensuring that tapping the media notification in the
 * system notification shade or lock screen directly restores the active lecture's [DetailScreen]
 * while preserving back navigation to the catalog root without exiting to the launcher.
 *
 * All generated [PendingIntent] instances strictly enforce [PendingIntent.FLAG_UPDATE_CURRENT]
 * combined with [PendingIntent.FLAG_IMMUTABLE] in compliance with Android 12+ (API 31+) security standards.
 */
object MediaSessionActivityHelper {

    /**
     * Intent extra key holding the active lecture identifier in [androidx.media3.common.MediaMetadata.extras].
     */
    const val EXTRA_IDENTIFIER: String = "com.uncaan.imit.extra.IDENTIFIER"

    /**
     * Default request code reserved exclusively for media session activity pending intents.
     */
    const val REQUEST_CODE_SESSION_ACTIVITY: Int = 2002

    /**
     * Scheme for IMIT application deep links.
     */
    const val DEEP_LINK_SCHEME: String = "imit"

    /**
     * Host for IMIT application deep links.
     */
    const val DEEP_LINK_HOST: String = "app"

    /**
     * Path prefix for lecture details destination deep link.
     */
    const val DETAILS_PATH_PREFIX: String = "details"

    /**
     * Fallback catalog root deep link URI.
     */
    const val CATALOG_URI: String = "imit://app/catalog"

    /**
     * Builds the deep link URI string for a specific lecture identifier, or catalog fallback.
     *
     * @param identifier Unique Archive.org lecture identifier, or null/blank.
     * @return Formatted deep link URI string (e.g., `imit://app/details/ocw-18.01-lec01`).
     */
    fun detailsDeepLinkUri(identifier: String?): String {
        return if (identifier.isNullOrBlank()) {
            CATALOG_URI
        } else {
            "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$DETAILS_PATH_PREFIX/$identifier"
        }
    }

    /**
     * Creates an [Intent] configured to navigate to the active lecture via deep link.
     *
     * Sets explicit package constraint to [Context.getPackageName] and marks [Intent.FLAG_ACTIVITY_SINGLE_TOP]
     * so that existing activity instances handle incoming navigation via `onNewIntent` without recreation.
     *
     * @param context Application or service [Context].
     * @param identifier Unique Archive.org lecture identifier, or null/blank for catalog fallback.
     * @return Configured [Intent] with [Intent.ACTION_VIEW] and package restriction.
     */
    fun createDetailsIntent(context: Context, identifier: String?): Intent {
        val uri = Uri.parse(detailsDeepLinkUri(identifier))
        val targetPackage = try {
            context.packageName
        } catch (_: Throwable) {
            "com.uncaan.imit"
        }
        return Intent(Intent.ACTION_VIEW, uri).apply {
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    /**
     * Creates a [PendingIntent] targeting the active lecture's detail screen with synthesized back stack.
     *
     * @param context Application or service [Context].
     * @param identifier Unique Archive.org lecture identifier, or null/blank for catalog fallback.
     * @param requestCode Optional unique integer request code, defaults to [REQUEST_CODE_SESSION_ACTIVITY].
     * @return Immutable [PendingIntent] suitable for [androidx.media3.session.MediaSession.setSessionActivity].
     */
    fun createSessionActivityPendingIntent(
        context: Context,
        identifier: String?,
        requestCode: Int = REQUEST_CODE_SESSION_ACTIVITY
    ): PendingIntent {
        val intent = createDetailsIntent(context, identifier)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }
}
