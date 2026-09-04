package com.uncaan.imit.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import com.uncaan.imit.core.database.di.databaseModule
import com.uncaan.imit.core.download.di.downloadModule
import com.uncaan.imit.core.network.di.dataModule
import com.uncaan.imit.core.network.di.networkModule
import com.uncaan.imit.core.player.di.playerModule
import com.uncaan.imit.feature.catalog.di.catalogModule
import com.uncaan.imit.feature.details.di.detailsModule
import com.uncaan.imit.feature.downloads.di.downloadsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Primary [Application] class for the IMIT app.
 *
 * Configures Koin dependency injection across all core and feature modules,
 * and acts as a [SingletonImageLoader.Factory] for Coil 3 to enforce strict memory
 * sizing (15% RAM max for memory cache and 50MB for disk cache).
 */
class IMITApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@IMITApplication)
            workManagerFactory()
            modules(
                // Core
                networkModule,
                dataModule,
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

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
