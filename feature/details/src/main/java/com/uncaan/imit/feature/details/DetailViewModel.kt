package com.uncaan.imit.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.PlayableStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DetailViewModel(
    private val identifier: String,
    private val videoRepository: VideoRepository,
    private val downloadedVideoDao: DownloadedVideoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _navigateToPlayer = MutableStateFlow<String?>(null)
    val navigateToPlayer: StateFlow<String?> = _navigateToPlayer.asStateFlow()

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
            _uiState.value = current.copy(
                downloadStatus = DownloadStatus.PENDING,
                downloadProgress = 0
            )
        }
    }

    private fun toggleDescription() {
        val current = _uiState.value as? DetailUiState.Success ?: return
        _uiState.value = current.copy(isDescriptionExpanded = !current.isDescriptionExpanded)
    }
}
