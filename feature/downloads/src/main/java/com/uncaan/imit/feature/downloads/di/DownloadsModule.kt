package com.uncaan.imit.feature.downloads.di

import com.uncaan.imit.feature.downloads.DownloadsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val downloadsViewModelModule = module {
    viewModelOf(::DownloadsViewModel)
}

val downloadsModule = module {
    includes(downloadsViewModelModule)
}
