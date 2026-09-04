package com.uncaan.imit.feature.downloads

/**
 * User-initiated actions and interactions on the Downloads screen.
 *
 * Dispatched via [DownloadsViewModel.onEvent] to trigger state mutations
 * or background IO operations.
 */
sealed interface DownloadsUiEvent {

    /**
     * Requests initiation of a delete confirmation dialog for a specific download.
     *
     * @property identifier Unique Archive.org identifier of the video.
     * @property localFilePath Path to the on-disk file, if downloaded.
     */
    data class DeleteDownload(
        val identifier: String,
        val localFilePath: String?
    ) : DownloadsUiEvent

    /**
     * Confirms permanent removal of the downloaded video file and its database record.
     *
     * @property identifier Unique Archive.org identifier of the video.
     * @property localFilePath Path to the on-disk file, if downloaded.
     */
    data class ConfirmDelete(
        val identifier: String,
        val localFilePath: String?
    ) : DownloadsUiEvent

    /** Dismisses the active delete confirmation dialog without making changes. */
    data object DismissDeleteDialog : DownloadsUiEvent

    /**
     * Requests video playback for a downloaded item.
     *
     * @property localFilePath Path to the offline media file, preferred if available.
     * @property videoUrl Fallback remote streaming URL if local file is missing.
     */
    data class PlayVideo(
        val localFilePath: String?,
        val videoUrl: String
    ) : DownloadsUiEvent
}
