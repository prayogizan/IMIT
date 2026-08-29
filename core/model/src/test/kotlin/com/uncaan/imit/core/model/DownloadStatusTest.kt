package com.uncaan.imit.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatusTest {

    @Test
    fun fromString_validValues_returnsCorrectEnum() {
        assertEquals(DownloadStatus.PENDING, DownloadStatus.fromString("PENDING"))
        assertEquals(DownloadStatus.DOWNLOADING, DownloadStatus.fromString("DOWNLOADING"))
        assertEquals(DownloadStatus.COMPLETED, DownloadStatus.fromString("COMPLETED"))
        assertEquals(DownloadStatus.FAILED, DownloadStatus.fromString("FAILED"))
        assertEquals(DownloadStatus.PAUSED, DownloadStatus.fromString("PAUSED"))
    }

    @Test
    fun fromString_caseInsensitive_returnsCorrectEnum() {
        assertEquals(DownloadStatus.PENDING, DownloadStatus.fromString("pending"))
        assertEquals(DownloadStatus.DOWNLOADING, DownloadStatus.fromString("Downloading"))
        assertEquals(DownloadStatus.COMPLETED, DownloadStatus.fromString("completed"))
        assertEquals(DownloadStatus.FAILED, DownloadStatus.fromString("failed"))
        assertEquals(DownloadStatus.PAUSED, DownloadStatus.fromString("paused"))
    }

    @Test
    fun fromString_invalidValue_fallbackToPending() {
        assertEquals(DownloadStatus.PENDING, DownloadStatus.fromString("UNKNOWN"))
        assertEquals(DownloadStatus.PENDING, DownloadStatus.fromString(""))
        assertEquals(DownloadStatus.PENDING, DownloadStatus.fromString("invalid_status"))
    }
}
