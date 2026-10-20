package com.uncaan.imit.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLDecoder

class NavRoutesTest {

    @Test
    fun `verify route constants`() {
        assertEquals("catalog", NavRoutes.CATALOG)
        assertEquals("details/{identifier}", NavRoutes.DETAILS)
        assertEquals("downloads", NavRoutes.DOWNLOADS)
        assertEquals("player?url={videoUrl}", NavRoutes.PLAYER)
    }

    @Test
    fun `detailsRoute builds expected route path`() {
        val identifier = "mit-ocw-6.0001-lec01"
        val expected = "details/mit-ocw-6.0001-lec01"
        assertEquals(expected, NavRoutes.detailsRoute(identifier))
    }

    @Test
    fun `playerRoute encodes videoUrl properly and is decodable`() {
        val rawUrl = "https://archive.org/download/MIT6.0001F16/MIT6_0001F16_lec01_300k.mp4?query=1&foo=bar#seg"
        val generatedRoute = NavRoutes.playerRoute(rawUrl)

        val prefix = "player?url="
        assert(generatedRoute.startsWith(prefix))

        val encodedUrl = generatedRoute.removePrefix(prefix)
        val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")

        assertEquals(rawUrl, decodedUrl)
    }

    @Test
    fun `playerRoute handles blank or empty url`() {
        val emptyRoute = NavRoutes.playerRoute("")
        assertEquals("player?url=", emptyRoute)
    }

    @Test
    fun `verify deep link constants`() {
        assertEquals("imit", NavRoutes.DEEP_LINK_SCHEME)
        assertEquals("app", NavRoutes.DEEP_LINK_HOST)
        assertEquals("imit://app", NavRoutes.DEEP_LINK_BASE_URI)
        assertEquals("imit://app/details/{identifier}", NavRoutes.DETAILS_DEEP_LINK_PATTERN)
        assertEquals("imit://app/downloads", NavRoutes.DOWNLOADS_DEEP_LINK_PATTERN)
    }

    @Test
    fun `detailsDeepLinkUri builds expected URI`() {
        val identifier = "mit-ocw-18.01-lec01"
        val expected = "imit://app/details/mit-ocw-18.01-lec01"
        assertEquals(expected, NavRoutes.detailsDeepLinkUri(identifier))
    }

    @Test
    fun `downloadsDeepLinkUri builds expected URI`() {
        val expected = "imit://app/downloads"
        assertEquals(expected, NavRoutes.downloadsDeepLinkUri())
    }
}
