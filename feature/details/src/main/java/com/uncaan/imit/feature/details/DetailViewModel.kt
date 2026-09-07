package com.uncaan.imit.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.download.DownloadManagerHelper
import com.uncaan.imit.core.download.VideoDownloadWorker
import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.PlayableStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the Detail screen managing lecture metadata, quality selection, video streaming,
 * download triggers, and real-time WorkManager progress observation.
 *
 * Exposes UI states via [uiState] and navigation events via [navigateToPlayer].
 * Processes user interactions through the single [onEvent] dispatch method.
 *
 * @param identifier Unique Archive.org identifier for the lecture.
 * @param videoRepository Repository providing video metadata from network.
 * @param downloadedVideoDao Room DAO for tracking download state and local file paths.
 * @param downloadManagerHelper Helper managing background WorkManager download jobs and storage validation.
 * @see DetailUiState For the complete state hierarchy.
 * @see DetailUiEvent For supported user interactions.
 */
class DetailViewModel(
    private val identifier: String,
    private val videoRepository: VideoRepository,
    private val downloadedVideoDao: DownloadedVideoDao,
    private val downloadManagerHelper: DownloadManagerHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _navigateToPlayer = MutableStateFlow<String?>(null)
    val navigateToPlayer: StateFlow<String?> = _navigateToPlayer.asStateFlow()

    private var downloadProgressJob: Job? = null

    init {
        loadDetail()
    }

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            is DetailUiEvent.SelectQuality -> selectQuality(event.stream)
            is DetailUiEvent.StreamVideo -> streamVideo()
            is DetailUiEvent.DownloadVideo -> initiateDownload()
            is DetailUiEvent.ToggleDescription -> toggleDescription()
            is DetailUiEvent.Retry -> loadDetail()
        }
    }

    fun onPlayerNavigated() {
        _navigateToPlayer.value = null
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            videoRepository.getVideoDetail(identifier).first().fold(
                onSuccess = { detail ->
                    val downloaded = downloadedVideoDao.getDownloadedVideoByIdSync(identifier)
                    _uiState.value = DetailUiState.Success(
                        detail = detail,
                        selectedStream = detail.bestStream,
                        downloadStatus = downloaded?.status,
                        downloadProgress = downloaded?.progress ?: 0
                    )
                    if (downloaded?.status == DownloadStatus.DOWNLOADING || downloaded?.status == DownloadStatus.PENDING) {
                        observeDownloadProgress(identifier)
                    }
                },
                onFailure = { error ->
                    _uiState.value = DetailUiState.Error(
                        message = error.localizedMessage ?: "Failed to load lecture details"
                    )
                }
            )
        }
    }

    private fun selectQuality(stream: PlayableStream) {
        val current = _uiState.value as? DetailUiState.Success ?: return
        _uiState.value = current.copy(selectedStream = stream)
    }

    private fun streamVideo() {
        val current = _uiState.value as? DetailUiState.Success ?: return
        val stream = current.selectedStream ?: return

        if (current.downloadStatus == DownloadStatus.COMPLETED) {
            viewModelScope.launch {
                val downloaded = downloadedVideoDao.getDownloadedVideoByIdSync(identifier)
                _navigateToPlayer.value = downloaded?.localFilePath ?: stream.streamUrl
            }
        } else {
            _navigateToPlayer.value = stream.streamUrl
        }
    }

    private fun initiateDownload() {
        val current = _uiState.value as? DetailUiState.Success ?: return
        val stream = current.selectedStream ?: return

        viewModelScope.launch {
            downloadedVideoDao.insert(
                DownloadedVideoEntity(
                    identifier = identifier,
                    title = current.detail.title,
                    description = current.detail.description,
                    fileName = stream.fileName,
                    downloadUrl = stream.streamUrl,
                    localFilePath = null,
                    fileSizeBytes = stream.sizeBytes,
                    progress = 0,
                    status = DownloadStatus.PENDING
                )
            )

            val enqueueResult = downloadManagerHelper.enqueueDownload(
                identifier = identifier,
                title = current.detail.title,
                downloadUrl = stream.streamUrl,
                fileName = stream.fileName
            )

            enqueueResult.fold(
                onSuccess = {
                    _uiState.value = current.copy(
                        downloadStatus = DownloadStatus.PENDING,
                        downloadProgress = 0,
                        downloadError = null
                    )
                    observeDownloadProgress(identifier)
                },
                onFailure = { error ->
                    downloadedVideoDao.updateStatus(identifier, DownloadStatus.FAILED)
                    _uiState.value = current.copy(
                        downloadStatus = DownloadStatus.FAILED,
                        downloadError = error.message ?: "Failed to start download"
                    )
                }
            )
        }
    }

    /**
     * Observes real-time WorkManager download progress and state transitions.
     *
     * Cancels any previously running observation job before collecting [DownloadManagerHelper.getWorkInfoFlow].
     * Continuously maps [WorkInfo.State] to [DownloadStatus] and updates [DetailUiState.Success.downloadProgress].
     *
     * @param identifier Unique Archive.org identifier for the video being downloaded.
     */
    private fun observeDownloadProgress(identifier: String) {
        downloadProgressJob?.cancel()
        downloadProgressJob = downloadManagerHelper.getWorkInfoFlow(identifier)
            .onEach { workInfo ->
                val info = workInfo ?: return@onEach
                val current = _uiState.value as? DetailUiState.Success ?: return@onEach

                val progress = info.progress.getInt(VideoDownloadWorker.KEY_PROGRESS, current.downloadProgress)
                val status = when (info.state) {
                    WorkInfo.State.ENQUEUED -> DownloadStatus.PENDING
                    WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
                    WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
                    WorkInfo.State.FAILED -> DownloadStatus.FAILED
                    WorkInfo.State.CANCELLED -> DownloadStatus.FAILED
                    WorkInfo.State.BLOCKED -> DownloadStatus.PENDING
                }

                val errorMessage = if (status == DownloadStatus.FAILED) {
                    info.outputData.getString(VideoDownloadWorker.KEY_ERROR) ?: current.downloadError
                } else {
                    null
                }

                _uiState.value = current.copy(
                    downloadStatus = status,
                    downloadProgress = if (status == DownloadStatus.COMPLETED) 100 else progress,
                    downloadError = errorMessage
                )
            }
            .launchIn(viewModelScope)
    }

    private fun toggleDescription() {
        val current = _uiState.value as? DetailUiState.Success ?: return
        _uiState.value = current.copy(isDescriptionExpanded = !current.isDescriptionExpanded)
    }
}
