package com.uncaan.imit.core.network.mapper

import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.PlayableStream
import com.uncaan.imit.core.model.VideoDetail
import com.uncaan.imit.core.network.model.ArchiveFileDto
import com.uncaan.imit.core.network.model.ArchiveMetadataResponseDto
import com.uncaan.imit.core.network.model.ArchiveSearchDocDto

private const val ARCHIVE_DOWNLOAD_BASE = "https://archive.org/download"

fun ArchiveSearchDocDto.toCourseVideo(): CourseVideo {
    return CourseVideo(
        identifier = identifier,
        title = title ?: "Untitled Lecture",
        description = description ?: "",
        creator = creator ?: "MIT OpenCourseWare",
        year = year ?: 0,
        thumbnailUrl = "$ARCHIVE_DOWNLOAD_BASE/$identifier/$identifier.png",
        downloadsCount = downloads ?: 0L
    )
}

fun ArchiveFileDto.isMp4Video(): Boolean {
    return name.endsWith(".mp4", ignoreCase = true) &&
            (format?.contains("MPEG4", ignoreCase = true) == true ||
             format?.contains("h.264", ignoreCase = true) == true ||
             format?.contains("mp4", ignoreCase = true) == true)
}

fun ArchiveFileDto.toPlayableStream(identifier: String): PlayableStream {
    return PlayableStream(
        fileName = name,
        format = format ?: "Unknown",
        sizeBytes = size?.toLongOrNull() ?: 0L,
        durationSeconds = length?.toDoubleOrNull() ?: 0.0,
        height = height?.toIntOrNull() ?: 0,
        width = width?.toIntOrNull() ?: 0,
        streamUrl = "$ARCHIVE_DOWNLOAD_BASE/$identifier/$name"
    )
}

fun ArchiveMetadataResponseDto.toVideoDetail(): VideoDetail {
    val identifier = metadata?.identifier ?: ""
    val mp4Streams = files
        .filter { it.isMp4Video() }
        .map { it.toPlayableStream(identifier) }
        .sortedByDescending { it.height }

    return VideoDetail(
        identifier = identifier,
        title = metadata?.title ?: "Untitled",
        description = metadata?.description ?: "",
        creator = metadata?.creator ?: "MIT OpenCourseWare",
        streams = mp4Streams,
        thumbnailUrl = "$ARCHIVE_DOWNLOAD_BASE/$identifier/$identifier.png"
    )
}
