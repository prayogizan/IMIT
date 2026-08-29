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
}
