package com.uncaan.imit.feature.catalog.di

import com.uncaan.imit.feature.catalog.CatalogViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val catalogViewModelModule = module {
    viewModelOf(::CatalogViewModel)
}

val catalogModule = module {
    includes(catalogViewModelModule)
}
