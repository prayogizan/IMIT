package com.uncaan.imit.feature.downloads

import com.uncaan.imit.core.model.DownloadTask

/**
 * Represents the possible UI states for the Downloads (Offline Library) screen.
 *
 * Follows the Unidirectional Data Flow (UDF) pattern where [DownloadsViewModel]
 * emits state transitions via a [kotlinx.coroutines.flow.StateFlow].
 *
 * @see DownloadsUiEvent For user actions triggering state transitions.
 * @see DownloadsViewModel For state production logic.
 */
sealed interface DownloadsUiState {

    /** Initial loading state displayed while database queries or storage telemetry resolve. */
    data object Loading : DownloadsUiState

    /** State indicating no downloaded videos exist in the offline library. */
    data object Empty : DownloadsUiState

    /**
     * Successful state containing the active offline library and device storage metrics.
     *
     * @property downloads Current list of downloaded or in-progress video tasks.
     * @property totalStorageUsedBytes Total disk space occupied by completed video downloads in bytes.
     * @property availableStorageMb Free disk space available on the download partition in megabytes.
     */
    data class Success(
        val downloads: List<DownloadTask> = emptyList(),
        val totalStorageUsedBytes: Long = 0L,
        val availableStorageMb: Long = 0L
    ) : DownloadsUiState

    /**
     * Error state when reading or updating offline download records fails.
     *
     * @property message User-facing explanation of the failure.
     */
    data class Error(
        val message: String
    ) : DownloadsUiState
}
