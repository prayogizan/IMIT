package com.uncaan.imit.core.database.mapper

import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.database.entity.VideoCacheEntity
import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.DownloadTask

fun VideoCacheEntity.toCourseVideo(): CourseVideo {
    return CourseVideo(
        identifier = identifier,
        title = title,
        description = description,
        creator = creator,
        year = year,
        thumbnailUrl = thumbnailUrl,
        downloadsCount = downloadsCount
    )
}

fun CourseVideo.toEntity(cachedAt: Long = System.currentTimeMillis()): VideoCacheEntity {
    return VideoCacheEntity(
        identifier = identifier,
        title = title,
        description = description,
        creator = creator,
        year = year,
        thumbnailUrl = thumbnailUrl,
        downloadsCount = downloadsCount,
        cachedAt = cachedAt
    )
}

fun List<VideoCacheEntity>.toCourseVideos(): List<CourseVideo> {
    return map { it.toCourseVideo() }
}

fun List<CourseVideo>.toEntities(cachedAt: Long = System.currentTimeMillis()): List<VideoCacheEntity> {
    return map { it.toEntity(cachedAt) }
}

fun DownloadedVideoEntity.toDownloadTask(): DownloadTask {
    return DownloadTask(
        identifier = identifier,
        title = title,
        description = description,
        fileName = fileName,
        downloadUrl = downloadUrl,
        localFilePath = localFilePath,
        fileSizeBytes = fileSizeBytes,
        progress = progress,
        status = status,
        downloadedAt = downloadedAt
    )
}

fun DownloadTask.toEntity(): DownloadedVideoEntity {
    return DownloadedVideoEntity(
        identifier = identifier,
        title = title,
        description = description,
        fileName = fileName,
        downloadUrl = downloadUrl,
        localFilePath = localFilePath,
        fileSizeBytes = fileSizeBytes,
        progress = progress,
        status = status,
        downloadedAt = downloadedAt
    )
}

fun List<DownloadedVideoEntity>.toDownloadTasks(): List<DownloadTask> {
    return map { it.toDownloadTask() }
}

fun List<DownloadTask>.toEntities(): List<DownloadedVideoEntity> {
    return map { it.toEntity() }
}
