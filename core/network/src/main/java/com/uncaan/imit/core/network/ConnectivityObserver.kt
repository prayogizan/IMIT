package com.uncaan.imit.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Observes network connectivity state changes using Android's [ConnectivityManager].
 *
 * Emits a boolean [Flow] indicating whether an active internet connection is currently available.
 *
 * @param context Android [Context] used to access system [ConnectivityManager].
 * @param createNetworkRequest Factory providing the [NetworkRequest] to register against.
 */
class ConnectivityObserver(
    private val context: Context,
    private val createNetworkRequest: () -> NetworkRequest = {
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
    }
) {

    /**
     * Cold [Flow] that emits `true` when network has internet capability,
     * and `false` when network connection is lost or unavailable.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val request = createNetworkRequest()

        cm.registerNetworkCallback(request, callback)

        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isInitiallyOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(isInitiallyOnline)

        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }
}
