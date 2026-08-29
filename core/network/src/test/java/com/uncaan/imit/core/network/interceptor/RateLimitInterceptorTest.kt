package com.uncaan.imit.core.network.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RateLimitInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private val delaysRecorded = mutableListOf<Long>()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        delaysRecorded.clear()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `passes through successful response without retry or delay`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val client = OkHttpClient.Builder()
            .addInterceptor(RateLimitInterceptor(sleeper = { delaysRecorded.add(it) }))
            .build()

        val response = client.newCall(Request.Builder().url(mockWebServer.url("/test")).build()).execute()

        assertEquals(200, response.code)
        assertEquals("OK", response.body?.string())
        assertEquals(0, delaysRecorded.size)
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `retries with exponential backoff on HTTP 429 and succeeds`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("Too Many Requests"))
        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("Too Many Requests"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val client = OkHttpClient.Builder()
            .addInterceptor(
                RateLimitInterceptor(
                    maxRetries = 3,
                    initialDelayMs = 1000L,
                    sleeper = { delaysRecorded.add(it) }
                )
            )
            .build()

        val response = client.newCall(Request.Builder().url(mockWebServer.url("/test")).build()).execute()

        assertEquals(200, response.code)
        assertEquals("Success", response.body?.string())
        assertEquals(3, mockWebServer.requestCount)
        assertEquals(listOf(1000L, 2000L), delaysRecorded)
    }

    @Test
    fun `retries using Retry-After header when present`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "5")
                .setBody("Rate Limited")
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val client = OkHttpClient.Builder()
            .addInterceptor(RateLimitInterceptor(sleeper = { delaysRecorded.add(it) }))
            .build()

        val response = client.newCall(Request.Builder().url(mockWebServer.url("/test")).build()).execute()

        assertEquals(200, response.code)
        assertEquals("OK", response.body?.string())
        assertEquals(2, mockWebServer.requestCount)
        assertEquals(listOf(5000L), delaysRecorded)
    }

    @Test
    fun `stops retrying and returns 429 when max retries exceeded`() {
        repeat(5) {
            mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("Too Many Requests"))
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(
                RateLimitInterceptor(
                    maxRetries = 2,
                    initialDelayMs = 500L,
                    sleeper = { delaysRecorded.add(it) }
                )
            )
            .build()

        val response = client.newCall(Request.Builder().url(mockWebServer.url("/test")).build()).execute()

        assertEquals(429, response.code)
        assertEquals(3, mockWebServer.requestCount) // initial + 2 retries
        assertEquals(listOf(500L, 1000L), delaysRecorded)
    }
}
