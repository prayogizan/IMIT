package com.uncaan.imit.core.download.di

import com.uncaan.imit.core.download.DownloadManagerHelper
import com.uncaan.imit.core.download.VideoDownloadWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

/**
 * Koin dependency injection module for video download components.
 *
 * Provides:
 * - [DownloadManagerHelper] singleton for WorkManager scheduling and storage checks.
 * - [VideoDownloadWorker] injected worker for background file streaming.
 */
val downloadModule = module {
    single { DownloadManagerHelper(androidContext()) }

    worker {
        VideoDownloadWorker(
            context = get(),
            workerParams = get(),
            okHttpClient = get(),
            downloadDao = get()
        )
    }
}
