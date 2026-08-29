package com.uncaan.imit.core.network.mapper

import com.uncaan.imit.core.network.model.ArchiveFileDto
import com.uncaan.imit.core.network.model.ArchiveItemMetadataDto
import com.uncaan.imit.core.network.model.ArchiveMetadataResponseDto
import com.uncaan.imit.core.network.model.ArchiveSearchDocDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMappersTest {

    @Test
    fun `toCourseVideo maps ArchiveSearchDocDto with all fields present`() {
        val doc = ArchiveSearchDocDto(
            identifier = "MIT6_001",
            title = "SICP Lecture 1",
            description = "Intro to Lisp",
            creator = "Prof. Sussman",
            year = 2008,
            publicdate = "2008-01-01",
            downloads = 1000L
        )

        val model = doc.toCourseVideo()
        assertEquals("MIT6_001", model.identifier)
        assertEquals("SICP Lecture 1", model.title)
        assertEquals("Intro to Lisp", model.description)
        assertEquals("Prof. Sussman", model.creator)
        assertEquals(2008, model.year)
        assertEquals("https://archive.org/download/MIT6_001/MIT6_001.png", model.thumbnailUrl)
        assertEquals(1000L, model.downloadsCount)
    }

    @Test
    fun `toCourseVideo maps ArchiveSearchDocDto with null fields to default values`() {
        val doc = ArchiveSearchDocDto(
            identifier = "MIT6_002",
            title = null,
            description = null,
            creator = null,
            year = null,
            publicdate = null,
            downloads = null
        )

        val model = doc.toCourseVideo()
        assertEquals("MIT6_002", model.identifier)
        assertEquals("Untitled Lecture", model.title)
        assertEquals("", model.description)
        assertEquals("MIT OpenCourseWare", model.creator)
        assertEquals(0, model.year)
        assertEquals("https://archive.org/download/MIT6_002/MIT6_002.png", model.thumbnailUrl)
        assertEquals(0L, model.downloadsCount)
    }

    @Test
    fun `isMp4Video correctly identifies valid MP4 video formats`() {
        val mp4Mpeg4 = ArchiveFileDto(name = "video.mp4", format = "MPEG4")
        val mp4H264 = ArchiveFileDto(name = "video.mp4", format = "h.264 HD")
        val mp4Format = ArchiveFileDto(name = "video.MP4", format = "512Kb mp4")
        val notMp4Ext = ArchiveFileDto(name = "video.mkv", format = "MPEG4")
        val mp4WithoutValidFormat = ArchiveFileDto(name = "video.mp4", format = "UnknownFormat")
        val nonVideo = ArchiveFileDto(name = "video.jpg", format = "JPEG")

        assertTrue(mp4Mpeg4.isMp4Video())
        assertTrue(mp4H264.isMp4Video())
        assertTrue(mp4Format.isMp4Video())
        assertFalse(notMp4Ext.isMp4Video())
        assertFalse(mp4WithoutValidFormat.isMp4Video())
        assertFalse(nonVideo.isMp4Video())
    }

    @Test
    fun `toPlayableStream maps ArchiveFileDto safely with numeric string parsing`() {
        val fileDto = ArchiveFileDto(
            name = "lecture_720p.mp4",
            format = "h.264",
            size = "524288000",
            length = "3600.5",
            height = "720",
            width = "1280"
        )

        val stream = fileDto.toPlayableStream("MIT6_001")
        assertEquals("lecture_720p.mp4", stream.fileName)
        assertEquals("h.264", stream.format)
        assertEquals(524288000L, stream.sizeBytes)
        assertEquals(3600.5, stream.durationSeconds, 0.001)
        assertEquals(720, stream.height)
        assertEquals(1280, stream.width)
        assertEquals("https://archive.org/download/MIT6_001/lecture_720p.mp4", stream.streamUrl)
    }

    @Test
    fun `toPlayableStream handles invalid numeric strings safely`() {
        val fileDto = ArchiveFileDto(
            name = "lecture.mp4",
            format = null,
            size = "not_a_number",
            length = "invalid_duration",
            height = "invalid_height",
            width = "invalid_width"
        )

        val stream = fileDto.toPlayableStream("MIT6_001")
        assertEquals("lecture.mp4", stream.fileName)
        assertEquals("Unknown", stream.format)
        assertEquals(0L, stream.sizeBytes)
        assertEquals(0.0, stream.durationSeconds, 0.001)
        assertEquals(0, stream.height)
        assertEquals(0, stream.width)
    }

    @Test
    fun `toVideoDetail filters MP4s, sorts by height descending, and maps metadata`() {
        val metadataResponse = ArchiveMetadataResponseDto(
            metadata = ArchiveItemMetadataDto(
                identifier = "MIT6_001",
                title = "Structure and Interpretation",
                description = "Full lecture series",
                creator = "Harold Abelson"
            ),
            files = listOf(
                ArchiveFileDto(name = "thumb.jpg", format = "JPEG", height = "100"),
                ArchiveFileDto(name = "sd.mp4", format = "MPEG4", height = "360", size = "100", length = "100"),
                ArchiveFileDto(name = "hd.mp4", format = "h.264", height = "720", size = "200", length = "100"),
                ArchiveFileDto(name = "fhd.mp4", format = "MPEG4", height = "1080", size = "300", length = "100"),
                ArchiveFileDto(name = "audio.mp3", format = "VBR MP3")
            )
        )

        val detail = metadataResponse.toVideoDetail()
        assertEquals("MIT6_001", detail.identifier)
        assertEquals("Structure and Interpretation", detail.title)
        assertEquals("Full lecture series", detail.description)
        assertEquals("Harold Abelson", detail.creator)
        assertEquals("https://archive.org/download/MIT6_001/MIT6_001.png", detail.thumbnailUrl)

        assertEquals(3, detail.streams.size)
        assertEquals(1080, detail.streams[0].height)
        assertEquals(720, detail.streams[1].height)
        assertEquals(360, detail.streams[2].height)
        assertEquals("fhd.mp4", detail.streams[0].fileName)
    }
}
