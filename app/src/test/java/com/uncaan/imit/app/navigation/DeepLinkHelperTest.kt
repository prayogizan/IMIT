package com.uncaan.imit.app.navigation

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.uncaan.imit.app.MainActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying [DeepLinkHelper] factory methods for constructing deep link intents
 * and synthesizing parent back stacks via [TaskStackBuilder].
 */
class DeepLinkHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockUri: Uri
    private lateinit var mockStackBuilder: TaskStackBuilder
    private lateinit var mockPendingIntent: PendingIntent

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic(TaskStackBuilder::class)

        mockContext = mockk(relaxed = true)
        mockUri = mockk(relaxed = true)
        mockPendingIntent = mockk(relaxed = true)

        every { Uri.parse(any()) } returns mockUri

        mockStackBuilder = mockk(relaxed = true) {
            every { addNextIntentWithParentStack(any()) } returns this
            every { getPendingIntent(any(), any()) } returns mockPendingIntent
        }
        every { TaskStackBuilder.create(any()) } returns mockStackBuilder
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `request code constants are distinct`() {
        assertNotEquals(DeepLinkHelper.REQUEST_CODE_DETAILS, DeepLinkHelper.REQUEST_CODE_DOWNLOADS)
        assertEquals(1001, DeepLinkHelper.REQUEST_CODE_DETAILS)
        assertEquals(1002, DeepLinkHelper.REQUEST_CODE_DOWNLOADS)
    }

    @Test
    fun `createDetailsIntent creates intent targeting MainActivity with action VIEW and details URI`() {
        val identifier = "test-video-123"
        val intent = DeepLinkHelper.createDetailsIntent(mockContext, identifier)

        assertNotNull(intent)
        verify { Uri.parse(NavRoutes.detailsDeepLinkUri(identifier)) }
    }

    @Test
    fun `createDownloadsIntent creates intent targeting MainActivity with action VIEW and downloads URI`() {
        val intent = DeepLinkHelper.createDownloadsIntent(mockContext)

        assertNotNull(intent)
        verify { Uri.parse(NavRoutes.downloadsDeepLinkUri()) }
    }

    @Test
    fun `createDetailsPendingIntent synthesizes back stack with immutable and update current flags`() {
        val identifier = "mit-ocw-physics-801"
        val intentSlot = slot<Intent>()
        val expectedFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val result = DeepLinkHelper.createDetailsPendingIntent(
            context = mockContext,
            identifier = identifier,
            requestCode = 9999
        )

        assertNotNull(result)
        assertEquals(mockPendingIntent, result)
        verify { TaskStackBuilder.create(mockContext) }
        verify { mockStackBuilder.addNextIntentWithParentStack(capture(intentSlot)) }
        verify { mockStackBuilder.getPendingIntent(9999, expectedFlags) }
        assertNotNull(intentSlot.captured)
    }

    @Test
    fun `createDownloadsPendingIntent synthesizes back stack with immutable and update current flags`() {
        val intentSlot = slot<Intent>()
        val expectedFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val result = DeepLinkHelper.createDownloadsPendingIntent(
            context = mockContext,
            requestCode = DeepLinkHelper.REQUEST_CODE_DOWNLOADS
        )

        assertNotNull(result)
        assertEquals(mockPendingIntent, result)
        verify { TaskStackBuilder.create(mockContext) }
        verify { mockStackBuilder.addNextIntentWithParentStack(capture(intentSlot)) }
        verify { mockStackBuilder.getPendingIntent(DeepLinkHelper.REQUEST_CODE_DOWNLOADS, expectedFlags) }
        assertNotNull(intentSlot.captured)
    }
}
