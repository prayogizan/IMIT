package com.uncaan.imit.core.player.di

import android.content.Context
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class PlayerModuleTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `verify playerModule can be loaded by Koin`() {
        val mockContext: Context = mockk(relaxed = true)
        val koinApp = startKoin {
            androidContext(mockContext)
            modules(playerModule)
        }

        assertNotNull(koinApp)
        assertNotNull(koinApp.koin)
    }
}
