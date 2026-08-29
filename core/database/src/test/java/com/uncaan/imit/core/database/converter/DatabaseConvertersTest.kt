package com.uncaan.imit.core.database.converter

import com.uncaan.imit.core.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DatabaseConvertersTest {

    private lateinit var converters: DatabaseConverters

    @Before
    fun setUp() {
        converters = DatabaseConverters()
    }

    @Test
    fun `fromDownloadStatus converts all enum values to correct string`() {
        assertEquals("PENDING", converters.fromDownloadStatus(DownloadStatus.PENDING))
        assertEquals("DOWNLOADING", converters.fromDownloadStatus(DownloadStatus.DOWNLOADING))
        assertEquals("COMPLETED", converters.fromDownloadStatus(DownloadStatus.COMPLETED))
        assertEquals("FAILED", converters.fromDownloadStatus(DownloadStatus.FAILED))
        assertEquals("PAUSED", converters.fromDownloadStatus(DownloadStatus.PAUSED))
    }

    @Test
    fun `fromDownloadStatus returns null for null status`() {
        assertNull(converters.fromDownloadStatus(null))
    }

    @Test
    fun `toDownloadStatus converts valid string to enum`() {
        assertEquals(DownloadStatus.PENDING, converters.toDownloadStatus("PENDING"))
        assertEquals(DownloadStatus.DOWNLOADING, converters.toDownloadStatus("DOWNLOADING"))
        assertEquals(DownloadStatus.COMPLETED, converters.toDownloadStatus("COMPLETED"))
        assertEquals(DownloadStatus.FAILED, converters.toDownloadStatus("FAILED"))
        assertEquals(DownloadStatus.PAUSED, converters.toDownloadStatus("PAUSED"))
    }

    @Test
    fun `toDownloadStatus handles lowercase string conversion`() {
        assertEquals(DownloadStatus.COMPLETED, converters.toDownloadStatus("completed"))
        assertEquals(DownloadStatus.DOWNLOADING, converters.toDownloadStatus("downloading"))
    }

    @Test
    fun `toDownloadStatus falls back to PENDING for unknown string`() {
        assertEquals(DownloadStatus.PENDING, converters.toDownloadStatus("UNKNOWN_STATUS"))
    }

    @Test
    fun `toDownloadStatus returns null for null string`() {
        assertNull(converters.toDownloadStatus(null))
    }
}
