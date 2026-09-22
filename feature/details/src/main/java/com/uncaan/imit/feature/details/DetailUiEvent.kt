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
}
