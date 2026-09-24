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

    /**
     * Pauses an active download.
     *
     * @property identifier Unique Archive.org identifier of the video.
     */
    data class PauseDownload(val identifier: String) : DownloadsUiEvent

    /**
     * Resumes a paused video download.
     *
     * @property identifier Unique Archive.org identifier of the video.
     * @property title Display title shown in notification.
     * @property downloadUrl Direct URL to stream the video.
     * @property fileName Destination file name.
     */
    data class ResumeDownload(
        val identifier: String,
        val title: String,
        val downloadUrl: String,
        val fileName: String
    ) : DownloadsUiEvent

    /**
     * Retries a failed video download.
     *
     * @property identifier Unique Archive.org identifier of the video.
     * @property title Display title shown in notification.
     * @property downloadUrl Direct URL to stream the video.
     * @property fileName Destination file name.
     */
    data class RetryDownload(
        val identifier: String,
        val title: String,
        val downloadUrl: String,
        val fileName: String
    ) : DownloadsUiEvent

    /**
     * Cancels an in-progress or paused download, cleaning up partial files and database record.
     *
     * @property identifier Unique Archive.org identifier of the video.
     * @property fileName Destination file name.
     */
    data class CancelDownload(
        val identifier: String,
        val fileName: String
    ) : DownloadsUiEvent
}
