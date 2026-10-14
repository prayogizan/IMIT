package com.uncaan.imit.app.navigation

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.uncaan.imit.app.MainActivity

/**
 * Helper object providing factory methods for deep link [Intent]s and [PendingIntent]s.
 *
 * Utilizes [TaskStackBuilder] to synthesize a full parent back stack, ensuring that
 * launching a deep link from external sources (e.g., system notifications) preserves
 * predictable back navigation to the Catalog root destination rather than exiting directly to home.
 *
 * All generated [PendingIntent] instances enforce [PendingIntent.FLAG_UPDATE_CURRENT] combined
 * with [PendingIntent.FLAG_IMMUTABLE] in compliance with Android 12+ (API 31+) security requirements.
 */
object DeepLinkHelper {

    /** Default request code for details destination PendingIntent. */
    const val REQUEST_CODE_DETAILS = 1001

    /** Default request code for downloads destination PendingIntent. */
    const val REQUEST_CODE_DOWNLOADS = 1002

    /**
     * Creates an [Intent] configured to navigate to the video detail destination via deep link.
     *
     * @param context Application or activity context.
     * @param identifier Unique Archive.org item identifier.
     * @return Configured [Intent] with [Intent.ACTION_VIEW] targeting [MainActivity].
     */
    fun createDetailsIntent(context: Context, identifier: String): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse(NavRoutes.detailsDeepLinkUri(identifier)),
            context,
            MainActivity::class.java
        )
    }

    /**
     * Creates a [PendingIntent] targeting the video detail destination with a synthesized parent stack.
     *
     * @param context Application or activity context.
     * @param identifier Unique Archive.org item identifier.
     * @param requestCode Optional unique integer request code, defaults to [REQUEST_CODE_DETAILS].
     * @return Immutable [PendingIntent] containing the synthesized navigation back stack.
     */
    fun createDetailsPendingIntent(
        context: Context,
        identifier: String,
        requestCode: Int = REQUEST_CODE_DETAILS
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

    /**
     * Creates an [Intent] configured to navigate to the downloads destination via deep link.
     *
     * @param context Application or activity context.
     * @return Configured [Intent] with [Intent.ACTION_VIEW] targeting [MainActivity].
     */
    fun createDownloadsIntent(context: Context): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse(NavRoutes.downloadsDeepLinkUri()),
            context,
            MainActivity::class.java
        )
    }

    /**
     * Creates a [PendingIntent] targeting the downloads destination with a synthesized parent stack.
     *
     * @param context Application or activity context.
     * @param requestCode Optional unique integer request code, defaults to [REQUEST_CODE_DOWNLOADS].
     * @return Immutable [PendingIntent] containing the synthesized navigation back stack.
     */
    fun createDownloadsPendingIntent(
        context: Context,
        requestCode: Int = REQUEST_CODE_DOWNLOADS
    ): PendingIntent {
        val intent = createDownloadsIntent(context)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }
}
