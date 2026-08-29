package com.uncaan.imit.app.navigation

import java.net.URLEncoder

object NavRoutes {
    const val CATALOG = "catalog"
    const val DETAILS = "details/{identifier}"
    const val DOWNLOADS = "downloads"
    const val PLAYER = "player?url={videoUrl}"

    fun detailsRoute(identifier: String): String = "details/$identifier"
    fun playerRoute(videoUrl: String): String = "player?url=${URLEncoder.encode(videoUrl, "UTF-8")}"
}
