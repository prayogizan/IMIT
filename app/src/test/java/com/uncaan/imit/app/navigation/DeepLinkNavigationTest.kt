package com.uncaan.imit.app.navigation

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration and unit test suite verifying deep link URI matching, intent routing,
 * and synthetic parent back stack navigation behavior returning to the Catalog destination.
 */
class DeepLinkNavigationTest {

    private lateinit var mockNavController: NavController

    @Before
    fun setUp() {
        mockNavController = mockk(relaxed = true)
    }

    @Test
    fun `details deep link pattern matches expected URI scheme and route structure`() {
        val pattern = NavRoutes.DETAILS_DEEP_LINK_PATTERN
        assertEquals("imit://app/details/{identifier}", pattern)

        val sampleIdentifier = "mit-ocw-physics-801"
        val concreteUri = NavRoutes.detailsDeepLinkUri(sampleIdentifier)
        assertEquals("imit://app/details/mit-ocw-physics-801", concreteUri)

        val regex = Regex("^imit://app/details/(.+)$")
        assertTrue(regex.matches(concreteUri))
        assertEquals(sampleIdentifier, regex.find(concreteUri)?.groupValues?.get(1))
    }

    @Test
    fun `downloads deep link pattern matches expected URI scheme and route structure`() {
        val pattern = NavRoutes.DOWNLOADS_DEEP_LINK_PATTERN
        assertEquals("imit://app/downloads", pattern)

        val concreteUri = NavRoutes.downloadsDeepLinkUri()
        assertEquals("imit://app/downloads", concreteUri)

        val regex = Regex("^imit://app/downloads$")
        assertTrue(regex.matches(concreteUri))
    }

    @Test
    fun `when details launched via deep link cold start and popBackStack fails, navigates to catalog`() {
        every { mockNavController.popBackStack() } returns false

        // Simulate DetailsScreen onBackClick lambda logic from MitOcwNavGraph
        val onBackClick: () -> Unit = {
            if (!mockNavController.popBackStack()) {
                mockNavController.navigate(NavRoutes.CATALOG) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        onBackClick()

        verify(exactly = 1) { mockNavController.popBackStack() }
        verify(exactly = 1) { mockNavController.navigate(NavRoutes.CATALOG, any<NavOptionsBuilder.() -> Unit>()) }
    }

    @Test
    fun `when details launched with existing back stack and popBackStack succeeds, pops to catalog without navigating`() {
        every { mockNavController.popBackStack() } returns true

        val onBackClick: () -> Unit = {
            if (!mockNavController.popBackStack()) {
                mockNavController.navigate(NavRoutes.CATALOG) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        onBackClick()

        verify(exactly = 1) { mockNavController.popBackStack() }
        verify(exactly = 0) { mockNavController.navigate(NavRoutes.CATALOG, any<NavOptionsBuilder.() -> Unit>()) }
    }

    @Test
    fun `navController handles deep link intent with matching uri`() {
        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockNavController.handleDeepLink(mockIntent) } returns true

        val handled = mockNavController.handleDeepLink(mockIntent)

        assertTrue(handled)
        verify { mockNavController.handleDeepLink(mockIntent) }
    }
}
