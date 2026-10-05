package com.uncaan.imit.core.player

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit test suite verifying [PlaybackServiceHelper] and top-level [shouldUseBackgroundService].
 */
class PlaybackServiceHelperTest {

    @Test
    fun `PlaybackServiceHelper object is instantiable and non null`() {
        assertNotNull(PlaybackServiceHelper)
    }

    @Test
    fun `shouldUseBackgroundService returns boolean consistent with Build VERSION SDK_INT`() {
        val expected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        assertEquals(expected, PlaybackServiceHelper.shouldUseBackgroundService())
        assertEquals(expected, shouldUseBackgroundService())
    }

    @Test
    fun `top-level shouldUseBackgroundService delegates directly to PlaybackServiceHelper`() {
        val helperResult = PlaybackServiceHelper.shouldUseBackgroundService()
        val topLevelResult = shouldUseBackgroundService()
        assertEquals(helperResult, topLevelResult)
    }
}
