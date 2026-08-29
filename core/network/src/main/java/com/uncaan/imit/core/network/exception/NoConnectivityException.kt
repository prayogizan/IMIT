package com.uncaan.imit.core.network.exception

import java.io.IOException

class NoConnectivityException(
    message: String = "No internet connection available"
) : IOException(message)
