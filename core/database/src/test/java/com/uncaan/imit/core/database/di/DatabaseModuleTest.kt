package com.uncaan.imit.core.database.di

import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class DatabaseModuleTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `verify databaseModule can be loaded by Koin`() {
        val koinApp = startKoin {
            modules(databaseModule)
        }

        assertNotNull(koinApp)
        assertNotNull(koinApp.koin)
    }
}
