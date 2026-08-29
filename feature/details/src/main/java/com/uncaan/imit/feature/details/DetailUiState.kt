package com.uncaan.imit.feature.details

import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.PlayableStream
import com.uncaan.imit.core.model.VideoDetail

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Success(
        val detail: VideoDetail,
        val selectedStream: PlayableStream? = null,
        val downloadStatus: DownloadStatus? = null,
        val downloadProgress: Int = 0,
        val isDescriptionExpanded: Boolean = false
    ) : DetailUiState

    data class Error(
        val message: String
    ) : DetailUiState
}
