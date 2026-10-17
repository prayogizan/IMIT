package com.uncaan.imit.core.player

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying [MediaSessionActivityHelper] constants, deep link URI construction,
 * intent formatting, and synthesized back stack [PendingIntent] creation.
 */
class MediaSessionActivityHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockUri: Uri
    private lateinit var mockStackBuilder: TaskStackBuilder
    private lateinit var mockPendingIntent: PendingIntent

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic(TaskStackBuilder::class)

        mockContext = mockk(relaxed = true) {
            every { packageName } returns "com.uncaan.imit"
        }
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
    fun `constants should maintain expected request codes and action keys`() {
        assertEquals("com.uncaan.imit.extra.IDENTIFIER", MediaSessionActivityHelper.EXTRA_IDENTIFIER)
        assertEquals(2002, MediaSessionActivityHelper.REQUEST_CODE_SESSION_ACTIVITY)
        assertEquals("imit", MediaSessionActivityHelper.DEEP_LINK_SCHEME)
        assertEquals("app", MediaSessionActivityHelper.DEEP_LINK_HOST)
        assertEquals("details", MediaSessionActivityHelper.DETAILS_PATH_PREFIX)
        assertEquals("imit://app/catalog", MediaSessionActivityHelper.CATALOG_URI)
    }

    @Test
    fun `detailsDeepLinkUri should return details deep link when identifier provided`() {
        val uri = MediaSessionActivityHelper.detailsDeepLinkUri("ocw-18.01-lec01")
        assertEquals("imit://app/details/ocw-18.01-lec01", uri)
    }

    @Test
    fun `detailsDeepLinkUri should fallback to catalog URI when identifier is null or blank`() {
        assertEquals("imit://app/catalog", MediaSessionActivityHelper.detailsDeepLinkUri(null))
        assertEquals("imit://app/catalog", MediaSessionActivityHelper.detailsDeepLinkUri(""))
        assertEquals("imit://app/catalog", MediaSessionActivityHelper.detailsDeepLinkUri("   "))
    }

    @Test
    fun `createDetailsIntent should configure VIEW action, package name, and singleTop flag`() {
        val intent = MediaSessionActivityHelper.createDetailsIntent(mockContext, "ocw-18.01-lec01")

        assertNotNull(intent)
        verify { Uri.parse("imit://app/details/ocw-18.01-lec01") }
    }

    @Test
    fun `createSessionActivityPendingIntent should synthesize back stack with immutable flags`() {
        val identifier = "ocw-18.01-lec02"
        val intentSlot = slot<Intent>()
        val expectedFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val result = MediaSessionActivityHelper.createSessionActivityPendingIntent(
            context = mockContext,
            identifier = identifier,
            requestCode = 2002
        )

        assertNotNull(result)
        assertEquals(mockPendingIntent, result)
        verify { TaskStackBuilder.create(mockContext) }
        verify { mockStackBuilder.addNextIntentWithParentStack(capture(intentSlot)) }
        verify { mockStackBuilder.getPendingIntent(2002, expectedFlags) }
        assertNotNull(intentSlot.captured)
    }
}
