package com.uncaan.imit.core.network.interceptor

import com.uncaan.imit.core.network.exception.NoConnectivityException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConnectivityInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `proceeds when network is available`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val client = OkHttpClient.Builder()
            .addInterceptor(ConnectivityInterceptor(isNetworkAvailable = { true }))
            .build()

        val response = client.newCall(Request.Builder().url(mockWebServer.url("/ping")).build()).execute()

        assertEquals(200, response.code)
        assertEquals("OK", response.body?.string())
    }

    @Test(expected = NoConnectivityException::class)
    fun `throws NoConnectivityException when network is not available`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(ConnectivityInterceptor(isNetworkAvailable = { false }))
            .build()

        client.newCall(Request.Builder().url(mockWebServer.url("/ping")).build()).execute()
    }

    @Test
    fun `proceeds when context is null as default fallback`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val client = OkHttpClient.Builder()
            .addInterceptor(ConnectivityInterceptor(context = null))
            .build()

        val response = client.newCall(Request.Builder().url(mockWebServer.url("/ping")).build()).execute()

        assertEquals(200, response.code)
    }
}
