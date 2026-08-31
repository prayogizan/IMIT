package com.uncaan.imit.core.player

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PipHelperTest {

    @Test
    fun `default aspect ratio constants are 16 by 9`() {
        assertEquals(16, PipHelper.DEFAULT_ASPECT_RATIO_NUMERATOR)
        assertEquals(9, PipHelper.DEFAULT_ASPECT_RATIO_DENOMINATOR)
    }

    @Test
    fun `isPipSupported returns false when system feature is not available`() {
        val mockContext: Context = mockk()
        val mockPackageManager: PackageManager = mockk()
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) } returns false

        val result = PipHelper.isPipSupported(mockContext)

        assertFalse(result)
    }

    @Test
    fun `enterPip returns false when PiP is not supported`() {
        val mockActivity: Activity = mockk()
        val mockPackageManager: PackageManager = mockk()
        every { mockActivity.packageManager } returns mockPackageManager
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) } returns false

        val result = PipHelper.enterPip(mockActivity)

        assertFalse(result)
    }

    @Test
    fun `enterPip returns false when activity throws IllegalStateException`() {
        val mockActivity: Activity = mockk()
        val mockPackageManager: PackageManager = mockk()
        every { mockActivity.packageManager } returns mockPackageManager
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) } returns true
        every { mockActivity.enterPictureInPictureMode(any()) } throws IllegalStateException("Not supported")

        val result = PipHelper.enterPip(mockActivity)

        assertFalse(result)
    }
}
