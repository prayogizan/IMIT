package com.uncaan.imit.core.player.di

import com.uncaan.imit.core.player.VideoPlaybackService
import com.uncaan.imit.core.player.VideoPlayerManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection module for core player components.
 *
 * Provides a singleton client facade [VideoPlayerManager] initialized with the application [android.content.Context].
 *
 * Note: [VideoPlaybackService] is an Android [androidx.media3.session.MediaSessionService] whose lifecycle
 * is instantiated and managed by the Android operating system when a client binds via [androidx.media3.session.SessionToken].
 */
val playerModule = module {
    single { VideoPlayerManager(androidContext()) }
}
