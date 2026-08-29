package com.uncaan.imit.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.network.exception.NoConnectivityException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val allVideos = mutableListOf<CourseVideo>()
    private var currentPage = 1
    private var searchJob: Job? = null

    init {
        loadVideos(page = 1)
    }

    fun onEvent(event: CatalogUiEvent) {
        when (event) {
            is CatalogUiEvent.Refresh -> refresh()
            is CatalogUiEvent.LoadMore -> loadMore()
            is CatalogUiEvent.Retry -> retry()
            is CatalogUiEvent.SearchQueryChanged -> onSearchQueryChanged(event.query)
            is CatalogUiEvent.ClearSearch -> clearSearch()
        }
    }

    private fun loadVideos(page: Int, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = (_uiState.value as? CatalogUiState.Success)
                    ?.copy(isRefreshing = true) ?: CatalogUiState.Loading
            } else if (page == 1) {
                _uiState.value = CatalogUiState.Loading
            }

            videoRepository.getMitOcwVideos(page).first().fold(
                onSuccess = { videos ->
                    if (isRefresh || page == 1) allVideos.clear()
                    allVideos.addAll(videos)
                    currentPage = page
                    _uiState.value = CatalogUiState.Success(
                        videos = allVideos.toList(),
                        isRefreshing = false,
                        currentPage = currentPage,
                        canLoadMore = videos.size >= 20,
                        isLoadingMore = false,
                        isOffline = false
                    )
                },
                onFailure = { error ->
                    if (allVideos.isNotEmpty()) {
                        _uiState.value = CatalogUiState.Success(
                            videos = allVideos.toList(),
                            isOffline = error is NoConnectivityException,
                            isRefreshing = false,
                            isLoadingMore = false,
                            currentPage = currentPage,
                            canLoadMore = false
                        )
                    } else {
                        _uiState.value = CatalogUiState.Error(
                            message = when (error) {
                                is NoConnectivityException -> "No internet connection."
                                else -> "Failed to load videos: ${error.localizedMessage ?: "Unknown error"}"
                            }
                        )
                    }
                }
            )
        }
    }

    private fun refresh() {
        currentPage = 1
        loadVideos(page = 1, isRefresh = true)
    }

    private fun loadMore() {
        val current = _uiState.value
        if (current is CatalogUiState.Success && current.canLoadMore && !current.isLoadingMore) {
            _uiState.value = current.copy(isLoadingMore = true)
            loadVideos(page = currentPage + 1)
        }
    }

    private fun retry() = loadVideos(page = 1)

    private fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                clearSearch()
                return@launch
            }
            _uiState.value = CatalogUiState.Loading
            allVideos.clear()
            videoRepository.searchVideos(query, page = 1).first().fold(
                onSuccess = { videos ->
                    allVideos.addAll(videos)
                    _uiState.value = CatalogUiState.Success(
                        videos = videos,
                        searchQuery = query,
                        canLoadMore = videos.size >= 20
                    )
                },
                onFailure = { error ->
                    _uiState.value = CatalogUiState.Error(
                        "Search failed: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            )
        }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        allVideos.clear()
        currentPage = 1
        loadVideos(page = 1)
    }
}
