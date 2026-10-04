package com.uncaan.imit.feature.details

import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.PlayableStream
import com.uncaan.imit.core.model.VideoDetail

sealed interface DetailUiState {
    data object Loading : DetailUiState

    /**
     * Successful state containing loaded lecture metadata and user interaction state.
     *
     * @property detail Full video metadata with playable streams.
     * @property selectedStream Currently selected stream for playback or download.
     * @property downloadStatus Current download lifecycle status, or null if not downloaded.
     * @property downloadProgress Download completion percentage (0–100).
     * @property downloadError User-facing error message from a failed download, or null.
     * @property isDescriptionExpanded Whether the description section is fully expanded.
     * @property isBackgroundPlaybackEnabled Whether audio continues playing when navigating away.
     */
    data class Success(
        val detail: VideoDetail,
        val selectedStream: PlayableStream? = null,
        val downloadStatus: DownloadStatus? = null,
        val downloadProgress: Int = 0,
        val downloadError: String? = null,
        val isDescriptionExpanded: Boolean = false,
        val isBackgroundPlaybackEnabled: Boolean = false,
    ) : DetailUiState

    data class Error(
        val message: String
    ) : DetailUiState
}
