package com.uncaan.imit.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.mapper.toDownloadTask
import com.uncaan.imit.core.download.DownloadManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the Offline Library screen managing video downloads,
 * disk storage calculation, and delete confirmation flows.
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
        }
    }

    private fun observeDownloads() {
        downloadedVideoDao.getAllDownloads()
            .map { entities -> entities.map { it.toDownloadTask() } }
            .onEach { downloads ->
                if (downloads.isEmpty()) {
                    _uiState.value = DownloadsUiState.Empty
                } else {
                    val totalSize = downloadedVideoDao.getTotalDownloadedSize()
                    val availableMb = downloadManager.getAvailableStorageMb()
                    _uiState.value = DownloadsUiState.Success(
                        downloads = downloads,
                        totalStorageUsedBytes = totalSize,
                        availableStorageMb = availableMb
                    )
                }
            }
            .launchIn(viewModelScope)
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
