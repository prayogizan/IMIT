package com.uncaan.imit.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavVisibilityTest {

    @Test
    fun `shouldShowBottomBar returns true for top-level destinations`() {
        assertTrue(shouldShowBottomBar(NavRoutes.CATALOG))
        assertTrue(shouldShowBottomBar(NavRoutes.DOWNLOADS))
    }

    @Test
    fun `shouldShowBottomBar returns false for detail, player and unknown routes`() {
        assertFalse(shouldShowBottomBar(NavRoutes.DETAILS))
        assertFalse(shouldShowBottomBar("details/mit-ocw-6.0001"))
        assertFalse(shouldShowBottomBar(NavRoutes.PLAYER))
        assertFalse(shouldShowBottomBar("player?url=https%3A%2F%2Fexample.com"))
        assertFalse(shouldShowBottomBar(null))
        assertFalse(shouldShowBottomBar("settings"))
    }

    @Test
    fun `BottomNavItem contains catalog and downloads tabs`() {
        val items = BottomNavItem.items
        assertEquals(2, items.size)
        assertEquals(NavRoutes.CATALOG, items[0].route)
        assertEquals("Catalog", items[0].title)
        assertEquals(NavRoutes.DOWNLOADS, items[1].route)
        assertEquals("Downloads", items[1].title)
    }
}
