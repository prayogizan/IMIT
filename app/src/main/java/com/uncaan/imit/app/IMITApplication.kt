package com.uncaan.imit.app

import android.app.Application
import com.uncaan.imit.core.database.di.databaseModule
import com.uncaan.imit.core.download.di.downloadModule
import com.uncaan.imit.core.network.di.networkModule
import com.uncaan.imit.core.player.di.playerModule
import com.uncaan.imit.feature.catalog.di.catalogModule
import com.uncaan.imit.feature.details.di.detailsModule
import com.uncaan.imit.feature.downloads.di.downloadsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class IMITApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@IMITApplication)
            workManagerFactory()
            modules(
                // Core
                networkModule,
                databaseModule,
                downloadModule,
                playerModule,
                // Features
                catalogModule,
                detailsModule,
                downloadsModule,
            )
        }
    }
}
