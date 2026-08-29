package com.uncaan.imit.core.network.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun `deserializes archive search response json correctly`() {
        val searchJson = """
            {
              "responseHeader": {
                "status": 0,
                "QTime": 25
              },
              "response": {
                "numFound": 42,
                "start": 0,
                "docs": [
                  {
                    "identifier": "MIT6_001S08_lec01",
                    "title": "MIT 6.001 Lecture 1",
                    "description": "Introduction to computer science",
                    "creator": "MIT OpenCourseWare",
                    "year": 2008,
                    "publicdate": "2008-01-15 12:00:00",
                    "downloads": 54321
                  },
                  {
                    "identifier": "MIT18_06_lec01",
                    "title": null,
                    "description": null,
                    "creator": null,
                    "year": null,
                    "publicdate": null,
                    "downloads": null
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ArchiveSearchResponseDto>(searchJson)
        val body = response.response
        assertNotNull(body)
        assertEquals(42, body?.numFound)
        assertEquals(0, body?.start)
        assertEquals(2, body?.docs?.size)

        val firstDoc = body?.docs?.get(0)
        assertNotNull(firstDoc)
        assertEquals("MIT6_001S08_lec01", firstDoc?.identifier)
        assertEquals("MIT 6.001 Lecture 1", firstDoc?.title)
        assertEquals("Introduction to computer science", firstDoc?.description)
        assertEquals("MIT OpenCourseWare", firstDoc?.creator)
        assertEquals(2008, firstDoc?.year)
        assertEquals("2008-01-15 12:00:00", firstDoc?.publicdate)
        assertEquals(54321L, firstDoc?.downloads)

        val secondDoc = body?.docs?.get(1)
        assertNotNull(secondDoc)
        assertEquals("MIT18_06_lec01", secondDoc?.identifier)
        assertNull(secondDoc?.title)
        assertNull(secondDoc?.year)
    }

    @Test
    fun `deserializes archive metadata response json correctly`() {
        val metadataJson = """
            {
              "created": 1580000000,
              "d1": "ia600100.us.archive.org",
              "d2": "ia800100.us.archive.org",
              "dir": "/10/items/MIT6_001S08_lec01",
              "files": [
                {
                  "name": "MIT6_001S08_lec01_300k.mp4",
                  "source": "derivative",
                  "format": "512Kb MPEG4",
                  "size": "104857600",
                  "length": "3060.50",
                  "height": "480",
                  "width": "640",
                  "title": "Lecture 1 480p"
                },
                {
                  "name": "MIT6_001S08_lec01_720p.mp4",
                  "source": "original",
                  "format": "h.264 HD",
                  "size": "524288000",
                  "length": "3060.50",
                  "height": "720",
                  "width": "1280",
                  "title": "Lecture 1 720p"
                },
                {
                  "name": "MIT6_001S08_lec01_thumb.jpg",
                  "source": "derivative",
                  "format": "JPEG",
                  "size": "50000"
                }
              ],
              "metadata": {
                "identifier": "MIT6_001S08_lec01",
                "title": "Structure and Interpretation of Computer Programs",
                "description": "Full lecture recording",
                "creator": "Harold Abelson",
                "date": "2008-01-15",
                "year": "2008",
                "mediatype": "movies"
              },
              "server": "ia600100.us.archive.org"
            }
        """.trimIndent()

        val response = json.decodeFromString<ArchiveMetadataResponseDto>(metadataJson)
        assertEquals(3, response.files.size)
        assertEquals("MIT6_001S08_lec01", response.metadata?.identifier)
        assertEquals("Structure and Interpretation of Computer Programs", response.metadata?.title)
        assertEquals("Harold Abelson", response.metadata?.creator)

        val mp4File = response.files[0]
        assertEquals("MIT6_001S08_lec01_300k.mp4", mp4File.name)
        assertEquals("512Kb MPEG4", mp4File.format)
        assertEquals("104857600", mp4File.size)
        assertEquals("3060.50", mp4File.length)
        assertEquals("480", mp4File.height)
        assertEquals("640", mp4File.width)
    }
}
