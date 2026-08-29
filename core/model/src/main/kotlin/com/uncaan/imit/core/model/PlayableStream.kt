package com.uncaan.imit.core.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class PlayableStream(
    val fileName: String,
    val format: String,
    val sizeBytes: Long,
    val durationSeconds: Double,
    val height: Int,
    val width: Int,
    val streamUrl: String
) {
    val qualityLabel: String
        get() = when {
            height >= 720 -> "HD ${height}p"
            height >= 360 -> "SD ${height}p"
            else -> "${height}p"
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format(Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

    val formattedDuration: String
        get() {
            val totalSeconds = durationSeconds.toLong()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }
}
