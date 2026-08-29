package com.uncaan.imit.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.uncaan.imit.core.model.DownloadStatus

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey
    @ColumnInfo(name = "identifier")
    val identifier: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "download_url")
    val downloadUrl: String,
    @ColumnInfo(name = "local_file_path")
    val localFilePath: String?,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    @ColumnInfo(name = "progress")
    val progress: Int = 0,
    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.PENDING,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = 0L
)
