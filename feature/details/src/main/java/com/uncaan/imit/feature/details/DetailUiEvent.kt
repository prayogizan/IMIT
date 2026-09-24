package com.uncaan.imit.feature.details

import com.uncaan.imit.core.model.PlayableStream

sealed interface DetailUiEvent {
    data class SelectQuality(val stream: PlayableStream) : DetailUiEvent
    data object StreamVideo : DetailUiEvent
    data object DownloadVideo : DetailUiEvent
    data object ToggleDescription : DetailUiEvent
    data object Retry : DetailUiEvent

    /** Dismisses the active download error message without resetting lecture details. */
    data object DismissDownloadError : DetailUiEvent

    /** Pauses an active download. */
    data object PauseDownload : DetailUiEvent

    /** Resumes a paused download with HTTP Range resumption. */
    data object ResumeDownload : DetailUiEvent

    /** Retries a failed download. */
    data object RetryDownload : DetailUiEvent

    /** Cancels download, cleans up partial files, and removes database record. */
    data object CancelDownload : DetailUiEvent
}
