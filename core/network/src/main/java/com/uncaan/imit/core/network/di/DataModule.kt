package com.uncaan.imit.core.network.di

import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.data.repository.VideoRepositoryImpl
import org.koin.dsl.module

val dataModule = module {
    single<VideoRepository> {
        VideoRepositoryImpl(apiService = get(), cacheDao = get())
    }
}
