package com.uncaan.imit.core.player.di

import com.uncaan.imit.core.player.VideoPlayerManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playerModule = module {
    single { VideoPlayerManager(androidContext()) }
}

