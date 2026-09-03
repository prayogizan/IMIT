package com.uncaan.imit.feature.downloads.di

import org.koin.dsl.module

val downloadsViewModelModule = module {
    // ViewModel to be provided by Issue #13
}

val downloadsModule = module {
    includes(downloadsViewModelModule)
}
