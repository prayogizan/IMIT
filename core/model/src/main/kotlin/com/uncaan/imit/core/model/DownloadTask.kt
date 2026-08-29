package com.uncaan.imit.core.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class DownloadTask(
    val identifier: String,
    val title: String,
    val description: String? = null,
    val fileName: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val fileSizeBytes: Long,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val downloadedAt: Long = 0L
) {
    val isPlayable: Boolean
        get() = status == DownloadStatus.COMPLETED && localFilePath != null

    val formattedSize: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format(Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
}
