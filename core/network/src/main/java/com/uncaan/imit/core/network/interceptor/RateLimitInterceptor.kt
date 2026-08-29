package com.uncaan.imit.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class RateLimitInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val sleeper: (Long) -> Unit = { delayMs ->
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        while (response.code == HTTP_TOO_MANY_REQUESTS && attempt < maxRetries) {
            attempt++
            val retryAfterHeader = response.header("Retry-After")
            val delayMs = if (retryAfterHeader != null) {
                (retryAfterHeader.toLongOrNull() ?: 1L) * 1000L
            } else {
                initialDelayMs * (1L shl (attempt - 1))
            }

            response.close()
            sleeper(delayMs)
            response = chain.proceed(request)
        }

        return response
    }

    companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
