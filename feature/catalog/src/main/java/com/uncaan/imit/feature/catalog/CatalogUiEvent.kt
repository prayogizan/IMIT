package com.uncaan.imit.feature.catalog

sealed interface CatalogUiEvent {
    data object Refresh : CatalogUiEvent
    data object LoadMore : CatalogUiEvent
    data object Retry : CatalogUiEvent
    data class SearchQueryChanged(val query: String) : CatalogUiEvent
    data object ClearSearch : CatalogUiEvent
}
