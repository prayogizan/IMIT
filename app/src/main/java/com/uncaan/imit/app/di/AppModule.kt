package com.uncaan.imit.app.di

import com.uncaan.imit.app.navigation.DeepLinkHelper
import com.uncaan.imit.core.download.DownloadNotificationNavigator
import org.koin.dsl.module

/**
 * Primary dependency injection module for the application layer.
 *
 * Provides:
 * - [DownloadNotificationNavigator] binding to [DeepLinkHelper] for background download notifications.
 */
val appModule = module {
    single<DownloadNotificationNavigator> { DeepLinkHelper }
}
