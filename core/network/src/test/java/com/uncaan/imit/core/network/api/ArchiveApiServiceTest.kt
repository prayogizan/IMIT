package com.uncaan.imit.core.network.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ArchiveApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ArchiveApiService

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        apiService = retrofit.create(ArchiveApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchMitOcwCollection sends expected query params and returns parsed response`() = runTest {
        val jsonBody = """
            {
              "response": {
                "numFound": 1,
                "start": 0,
                "docs": [
                  {
                    "identifier": "MIT6_001S08_lec01",
                    "title": "MIT 6.001 Lecture 1",
                    "description": "Intro to CS",
                    "creator": "MIT OCW",
                    "year": 2008,
                    "publicdate": "2008-01-01",
                    "downloads": 100
                  }
                ]
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonBody)
        )

        val result = apiService.searchMitOcwCollection(page = 1, rows = 20)
        assertNotNull(result.response)
        assertEquals(1, result.response?.numFound)
        assertEquals(1, result.response?.docs?.size)
        assertEquals("MIT6_001S08_lec01", result.response?.docs?.firstOrNull()?.identifier)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        val path = recordedRequest.path ?: ""
        assert(path.startsWith("/advancedsearch.php"))
        assert(path.contains("rows=20"))
        assert(path.contains("page=1"))
        assert(path.contains("output=json"))
    }

    @Test
    fun `getItemMetadata fetches metadata for specific identifier`() = runTest {
        val jsonBody = """
            {
              "metadata": {
                "identifier": "MIT6_001S08_lec01",
                "title": "Structure and Interpretation of Computer Programs",
                "creator": "Harold Abelson"
              },
              "files": [
                {
                  "name": "lecture_01.mp4",
                  "format": "512Kb MPEG4",
                  "size": "50000000",
                  "length": "3000",
                  "height": "480",
                  "width": "640"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonBody)
        )

        val result = apiService.getItemMetadata("MIT6_001S08_lec01")
        assertNotNull(result.metadata)
        assertEquals("MIT6_001S08_lec01", result.metadata?.identifier)
        assertEquals("Structure and Interpretation of Computer Programs", result.metadata?.title)
        assertEquals(1, result.files.size)
        assertEquals("lecture_01.mp4", result.files[0].name)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/metadata/MIT6_001S08_lec01", recordedRequest.path)
    }
}
