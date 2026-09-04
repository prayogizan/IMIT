package com.uncaan.imit.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConnectivityObserverTest {

    private val context: Context = mockk(relaxed = true)
    private val connectivityManager: ConnectivityManager = mockk(relaxed = true)
    private val mockRequest: NetworkRequest = mockk(relaxed = true)
    private val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setUp() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot)) } returns Unit
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } returns Unit
    }

    @Test
    fun `isOnline emits true initially when active network has internet capability`() = runTest {
        val network: Network = mockk()
        val capabilities: NetworkCapabilities = mockk()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val observer = ConnectivityObserver(context, createNetworkRequest = { mockRequest })

        observer.isOnline.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline emits false initially when no active network`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        val observer = ConnectivityObserver(context, createNetworkRequest = { mockRequest })

        observer.isOnline.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline emits true on network available and false on network lost`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        val observer = ConnectivityObserver(context, createNetworkRequest = { mockRequest })

        observer.isOnline.test {
            assertFalse(awaitItem())

            val dummyNetwork: Network = mockk()
            callbackSlot.captured.onAvailable(dummyNetwork)
            assertTrue(awaitItem())

            callbackSlot.captured.onLost(dummyNetwork)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline unregisters callback when flow is cancelled`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        val observer = ConnectivityObserver(context, createNetworkRequest = { mockRequest })

        observer.isOnline.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify { connectivityManager.unregisterNetworkCallback(callbackSlot.captured) }
    }
}
