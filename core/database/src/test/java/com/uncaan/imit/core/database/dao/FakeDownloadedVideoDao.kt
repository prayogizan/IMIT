package com.uncaan.imit.core.database.dao

import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDownloadedVideoDao : DownloadedVideoDao {

    private val downloadsMap = mutableMapOf<String, DownloadedVideoEntity>()
    private val downloadsFlow = MutableStateFlow<List<DownloadedVideoEntity>>(emptyList())

    private fun emitUpdate() {
        downloadsFlow.value = downloadsMap.values.sortedByDescending { it.downloadedAt }
    }

    override suspend fun insert(downloadedVideo: DownloadedVideoEntity) {
        downloadsMap[downloadedVideo.identifier] = downloadedVideo
        emitUpdate()
    }

    override suspend fun insertAll(downloadedVideos: List<DownloadedVideoEntity>) {
        downloadedVideos.forEach { downloadsMap[it.identifier] = it }
        emitUpdate()
    }

    override fun getAllDownloadedVideos(): Flow<List<DownloadedVideoEntity>> {
        return downloadsFlow
    }

    override fun getAllDownloads(): Flow<List<DownloadedVideoEntity>> {
        return downloadsFlow
    }

    override suspend fun getTotalDownloadedSize(): Long {
        return downloadsMap.values
            .filter { it.status == DownloadStatus.COMPLETED }
            .sumOf { it.fileSizeBytes }
    }

    override fun getDownloadedVideoById(identifier: String): Flow<DownloadedVideoEntity?> {
        return downloadsFlow.map { list -> list.find { it.identifier == identifier } }
    }

    override suspend fun getDownloadedVideoByIdSync(identifier: String): DownloadedVideoEntity? {
        return downloadsMap[identifier]
    }

    override fun getDownloadedVideosByStatus(status: DownloadStatus): Flow<List<DownloadedVideoEntity>> {
        return downloadsFlow.map { list -> list.filter { it.status == status } }
    }

    override suspend fun updateProgress(identifier: String, progress: Int, status: DownloadStatus): Int {
        val existing = downloadsMap[identifier] ?: return 0
        downloadsMap[identifier] = existing.copy(progress = progress, status = status)
        emitUpdate()
        return 1
    }

    override suspend fun updateStatus(identifier: String, status: DownloadStatus): Int {
        val existing = downloadsMap[identifier] ?: return 0
        downloadsMap[identifier] = existing.copy(status = status)
        emitUpdate()
        return 1
    }

    override suspend fun markCompleted(
        identifier: String,
        localFilePath: String,
        status: DownloadStatus,
        downloadedAt: Long
    ): Int {
        val existing = downloadsMap[identifier] ?: return 0
        downloadsMap[identifier] = existing.copy(
            localFilePath = localFilePath,
            status = status,
            downloadedAt = downloadedAt,
            progress = 100
        )
        emitUpdate()
        return 1
    }

    override suspend fun deleteById(identifier: String): Int {
        val removed = downloadsMap.remove(identifier) != null
        if (removed) emitUpdate()
        return if (removed) 1 else 0
    }

    override suspend fun deleteDownload(identifier: String): Int {
        return deleteById(identifier)
    }

    override suspend fun delete(downloadedVideo: DownloadedVideoEntity): Int {
        return deleteById(downloadedVideo.identifier)
    }

    override suspend fun clearAll(): Int {
        val count = downloadsMap.size
        downloadsMap.clear()
        emitUpdate()
        return count
    }
}
