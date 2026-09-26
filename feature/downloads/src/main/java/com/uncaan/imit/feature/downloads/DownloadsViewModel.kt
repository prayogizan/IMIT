package com.uncaan.imit.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.mapper.toDownloadTask
import com.uncaan.imit.core.download.DownloadManagerHelper
import com.uncaan.imit.core.download.VideoDownloadWorker
import com.uncaan.imit.core.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the Offline Library screen managing video downloads,
 * disk storage calculation, and delete confirmation flows.
 *
 * Combines reactive Room database observation with live WorkManager progress
 * streaming. Active download progress is sourced directly from [WorkInfo.progress]
 * via [DownloadManagerHelper.getAllDownloadsWorkInfoFlow], bypassing database write
 * bottlenecks. Storage telemetry recalculates on every combined emission.
 *
 * Exposes reactive UI states through [uiState] and deletion dialog state through [showDeleteDialog].
 * Processes user actions through the single [onEvent] dispatch method.
 *
 * @param downloadedVideoDao Room DAO providing reactive observation and deletion of downloaded items.
 * @param downloadManager Helper managing WorkManager download tasks and local file deletions.
 * @see DownloadsUiState For the complete state hierarchy.
 * @see DownloadsUiEvent For supported user interactions.
 */
class DownloadsViewModel(
    private val downloadedVideoDao: DownloadedVideoDao,
    private val downloadManager: DownloadManagerHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadsUiState>(DownloadsUiState.Loading)
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow<Pair<String, String?>?>(null)
    val showDeleteDialog: StateFlow<Pair<String, String?>?> = _showDeleteDialog.asStateFlow()

    init {
        observeDownloads()
    }

    /**
     * Single entry point for all user-initiated interactions on the Downloads screen.
     *
     * @param event The [DownloadsUiEvent] to process.
     */
    fun onEvent(event: DownloadsUiEvent) {
        when (event) {
            is DownloadsUiEvent.DeleteDownload -> {
                _showDeleteDialog.value = Pair(event.identifier, event.localFilePath)
            }
            is DownloadsUiEvent.ConfirmDelete -> confirmDelete(event.identifier, event.localFilePath)
            is DownloadsUiEvent.DismissDeleteDialog -> _showDeleteDialog.value = null
            is DownloadsUiEvent.PlayVideo -> {
                // Playback routing handled by screen navigation callback
            }
            is DownloadsUiEvent.PauseDownload -> pauseDownload(event.identifier)
            is DownloadsUiEvent.ResumeDownload -> resumeDownload(
                identifier = event.identifier,
                title = event.title,
                downloadUrl = event.downloadUrl,
                fileName = event.fileName
            )
            is DownloadsUiEvent.RetryDownload -> retryDownload(
                identifier = event.identifier,
                title = event.title,
                downloadUrl = event.downloadUrl,
                fileName = event.fileName
            )
            is DownloadsUiEvent.CancelDownload -> cancelDownload(
                identifier = event.identifier,
                fileName = event.fileName
            )
        }
    }

    private fun pauseDownload(identifier: String) {
        downloadManager.pauseDownload(identifier)
        viewModelScope.launch {
            downloadedVideoDao.updateStatus(identifier, DownloadStatus.PAUSED)
        }
    }

    private fun resumeDownload(
        identifier: String,
        title: String,
        downloadUrl: String,
        fileName: String
    ) {
        val result = downloadManager.resumeDownload(
            identifier = identifier,
            title = title,
            downloadUrl = downloadUrl,
            fileName = fileName
        )
        if (result.isFailure) {
            viewModelScope.launch {
                downloadedVideoDao.updateStatus(identifier, DownloadStatus.FAILED)
            }
        }
    }

    private fun retryDownload(
        identifier: String,
        title: String,
        downloadUrl: String,
        fileName: String
    ) {
        resumeDownload(identifier, title, downloadUrl, fileName)
    }

    private fun cancelDownload(identifier: String, fileName: String) {
        downloadManager.cancelDownload(identifier, fileName)
        viewModelScope.launch {
            downloadedVideoDao.deleteDownload(identifier)
        }
    }

    /**
     * Combines Room database download records with live WorkManager progress updates.
     *
     * Room Flow provides the canonical download list (status, file size, local path).
     * WorkManager Flow provides real-time progress for active downloads without
     * requiring database writes from the worker for each progress tick.
     *
     * On each combined emission:
     * 1. Maps Room entities to [com.uncaan.imit.core.model.DownloadTask] domain objects
     * 2. Builds a live progress map from [WorkInfo] items (identifier to 0..100)
     * 3. Recalculates storage telemetry (total used + available free space)
     * 4. Emits [DownloadsUiState.Success] or [DownloadsUiState.Empty]
     */
    private fun observeDownloads() {
        combine(
            downloadedVideoDao.getAllDownloads(),
            downloadManager.getAllDownloadsWorkInfoFlow()
        ) { entities, workInfoList ->
            val downloads = entities.map { it.toDownloadTask() }

            if (downloads.isEmpty()) {
                _uiState.value = DownloadsUiState.Empty
                return@combine
            }

            val liveProgressMap = buildLiveProgressMap(workInfoList)

            val totalSize = try {
                downloadedVideoDao.getTotalDownloadedSize()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                0L
            }

            val availableMb = downloadManager.getAvailableStorageMb()

            _uiState.value = DownloadsUiState.Success(
                downloads = downloads,
                totalStorageUsedBytes = totalSize,
                availableStorageMb = availableMb,
                liveProgressMap = liveProgressMap
            )
        }.launchIn(viewModelScope)
    }

    /**
     * Builds a map of video identifier to live download progress percentage
     * from the current list of [WorkInfo] items.
     *
     * Only includes entries for actively tracked states (ENQUEUED, BLOCKED, RUNNING, SUCCEEDED).
     * CANCELLED and FAILED states are excluded since their progress is not meaningful
     * for the live overlay.
     *
     * @param workInfoList Current list of all download [WorkInfo] items.
     * @return Map of identifier to progress (0..100).
     */
    private fun buildLiveProgressMap(workInfoList: List<WorkInfo>): Map<String, Int> {
        return workInfoList.mapNotNull { info ->
            val identifier = downloadManager.getIdentifierFromWorkInfo(info)
                ?: return@mapNotNull null

            val progress = when (info.state) {
                WorkInfo.State.RUNNING -> info.progress.getInt(
                    VideoDownloadWorker.KEY_PROGRESS, 0
                )
                WorkInfo.State.SUCCEEDED -> 100
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> 0
                else -> return@mapNotNull null
            }
            identifier to progress
        }.toMap()
    }

    private fun confirmDelete(identifier: String, localFilePath: String?) {
        viewModelScope.launch {
            localFilePath?.let { downloadManager.deleteDownloadedFile(it) }
            downloadedVideoDao.deleteDownload(identifier)
            downloadManager.cancelDownload(identifier)
            _showDeleteDialog.value = null
        }
    }
}
