package com.uncaan.imit.app

import com.uncaan.imit.core.database.di.databaseModule
import com.uncaan.imit.core.download.di.downloadModule
import com.uncaan.imit.core.network.di.dataModule
import com.uncaan.imit.core.network.di.networkModule
import com.uncaan.imit.core.player.di.playerModule
import com.uncaan.imit.feature.catalog.di.catalogModule
import com.uncaan.imit.feature.details.di.detailsModule
import com.uncaan.imit.feature.downloads.di.downloadsModule
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class KoinSetupTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `verify all Koin modules can be initialized together`() {
        val koinApp = startKoin {
            modules(
                networkModule,
                dataModule,
                databaseModule,
                downloadModule,
                playerModule,
                catalogModule,
                detailsModule,
                downloadsModule,
            )
        }

        assertNotNull(koinApp)
        assertNotNull(koinApp.koin)
    }
}
