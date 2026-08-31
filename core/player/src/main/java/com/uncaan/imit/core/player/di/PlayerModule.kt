package com.uncaan.imit.core.player.di

import com.uncaan.imit.core.player.VideoPlayerManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection module for core player components.
 *
 * Provides a singleton instance of [VideoPlayerManager] initialized with the application [android.content.Context].
 */
val playerModule = module {
    single { VideoPlayerManager(androidContext()) }
}


