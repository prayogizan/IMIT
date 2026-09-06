package com.uncaan.imit.feature.details.di

import com.uncaan.imit.feature.details.DetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for Detail screen presentation components.
 *
 * Provides [DetailViewModel] with assisted injection of the video identifier.
 *
 * @see detailsModule For parent module composition.
 */
val detailsViewModelModule = module {
    viewModel { (identifier: String) ->
        DetailViewModel(
            identifier = identifier,
            videoRepository = get(),
            downloadedVideoDao = get(),
            downloadManagerHelper = get()
        )
    }
}
