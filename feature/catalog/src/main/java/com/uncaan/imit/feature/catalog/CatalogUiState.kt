package com.uncaan.imit.feature.catalog

import com.uncaan.imit.core.model.CourseVideo

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Success(
        val videos: List<CourseVideo> = emptyList(),
        val isRefreshing: Boolean = false,
        val currentPage: Int = 1,
        val canLoadMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isOffline: Boolean = false,
        val searchQuery: String = ""
    ) : CatalogUiState

    data class Error(
        val message: String
    ) : CatalogUiState
}
