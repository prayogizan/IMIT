package com.uncaan.imit.core.download.di

import android.content.Context
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class DownloadModuleTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `verify downloadModule can be loaded by Koin`() {
        val mockContext: Context = mockk(relaxed = true)
        val mockOkHttpClient: OkHttpClient = mockk(relaxed = true)
        val mockDownloadDao: DownloadedVideoDao = mockk(relaxed = true)

        val testDependenciesModule = module {
            single { mockOkHttpClient }
            single { mockDownloadDao }
        }

        val koinApp = startKoin {
            androidContext(mockContext)
            modules(testDependenciesModule, downloadModule)
        }

        assertNotNull(koinApp)
        assertNotNull(koinApp.koin)
    }
}
