package com.uncaan.imit.app.navigation

import java.net.URLEncoder

/**
 * Route definitions, deep link URI templates, and navigation argument helpers for the IMIT application.
 *
 * Centralizes all internal navigation string routes and external deep link URI structures
 * used by `MitOcwNavGraph` and `DeepLinkHelper`.
 */
object NavRoutes {
    /** Root catalog destination displaying the searchable video grid. */
    const val CATALOG = "catalog"

    /** Course detail route template requiring an Archive.org item identifier path argument. */
    const val DETAILS = "details/{identifier}"

    /** Offline downloaded videos destination. */
    const val DOWNLOADS = "downloads"

    /** Media3 video player destination with optional URL-encoded video stream query parameter. */
    const val PLAYER = "player?url={videoUrl}"

    /** Deep link URI scheme for internal app deep links. */
    const val DEEP_LINK_SCHEME = "imit"

    /** Deep link authority/host representing the application namespace. */
    const val DEEP_LINK_HOST = "app"

    /** Base URI for constructing deep link patterns (`imit://app`). */
    const val DEEP_LINK_BASE_URI = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST"

    /** Deep link pattern for the details screen (`imit://app/details/{identifier}`). */
    const val DETAILS_DEEP_LINK_PATTERN = "$DEEP_LINK_BASE_URI/details/{identifier}"

    /** Deep link pattern for the downloads screen (`imit://app/downloads`). */
    const val DOWNLOADS_DEEP_LINK_PATTERN = "$DEEP_LINK_BASE_URI/downloads"

    /**
     * Builds the internal route path for navigating to a specific video's detail screen.
     *
     * @param identifier Unique Archive.org identifier.
     * @return Formatted route string (`details/$identifier`).
     */
    fun detailsRoute(identifier: String): String = "details/$identifier"

    /**
     * Builds the full deep link URI for navigating to a specific video's detail screen.
     *
     * @param identifier Unique Archive.org identifier.
     * @return Formatted deep link URI string (`imit://app/details/$identifier`).
     */
    fun detailsDeepLinkUri(identifier: String): String = "$DEEP_LINK_BASE_URI/details/$identifier"

    /**
     * Builds the full deep link URI for navigating to the downloads screen.
     *
     * @return Formatted deep link URI string (`imit://app/downloads`).
     */
    fun downloadsDeepLinkUri(): String = "$DEEP_LINK_BASE_URI/downloads"

    /**
     * Builds the internal route path for navigating to the video player screen.
     *
     * Encodes the raw video stream URL into UTF-8 to ensure safe URL parsing across query parameters.
     *
     * @param videoUrl Direct media stream URL to play.
     * @return Formatted route string with encoded video URL query parameter.
     */
    fun playerRoute(videoUrl: String): String = "player?url=${URLEncoder.encode(videoUrl, "UTF-8")}"
}
