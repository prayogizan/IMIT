package com.uncaan.imit.core.network.di

import android.content.Context
import com.uncaan.imit.core.network.ConnectivityObserver
import com.uncaan.imit.core.network.api.ArchiveApiService
import com.uncaan.imit.core.network.interceptor.ConnectivityInterceptor
import com.uncaan.imit.core.network.interceptor.RateLimitInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val ARCHIVE_BASE_URL = "https://archive.org/"
private const val TIMEOUT_SECONDS = 30L

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    single { RateLimitInterceptor() }

    single { ConnectivityInterceptor(getOrNull<Context>()) }
    single { ConnectivityObserver(androidContext()) }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<ConnectivityInterceptor>())
            .addInterceptor(get<RateLimitInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    single {
        val json = get<Json>()
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(ARCHIVE_BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single<ArchiveApiService> {
        get<Retrofit>().create(ArchiveApiService::class.java)
    }
}
