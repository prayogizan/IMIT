package com.uncaan.imit.feature.details.di

import com.uncaan.imit.feature.details.DetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val detailsViewModelModule = module {
    viewModel { (identifier: String) ->
        DetailViewModel(
            identifier = identifier,
            videoRepository = get(),
            downloadedVideoDao = get()
        )
    }
}
