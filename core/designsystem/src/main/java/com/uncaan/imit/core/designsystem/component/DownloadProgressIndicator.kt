package com.uncaan.imit.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uncaan.imit.core.designsystem.theme.MitOcwTheme
import com.uncaan.imit.core.designsystem.theme.spacing
import com.uncaan.imit.core.model.DownloadStatus

/**
 * Reusable Download Progress Indicator component displaying progress bar,
 * status badge, byte size, and action trigger.
 */
@Composable
fun DownloadProgressIndicator(
    progress: Int,
    status: DownloadStatus,
    modifier: Modifier = Modifier,
    formattedSize: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
    ) {
        // Status header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status text and icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                StatusIcon(status = status)
                Text(
                    text = when (status) {
                        DownloadStatus.PENDING -> "Waiting in queue..."
                        DownloadStatus.DOWNLOADING -> "Downloading $progress%"
                        DownloadStatus.PAUSED -> "Download paused ($progress%)"
                        DownloadStatus.COMPLETED -> "Download completed"
                        DownloadStatus.FAILED -> "Download failed"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (status) {
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Size / Action trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                if (formattedSize != null) {
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onActionClick != null) {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = when (status) {
                                DownloadStatus.DOWNLOADING -> Icons.Default.Warning
                                DownloadStatus.PAUSED -> Icons.Default.PlayArrow
                                DownloadStatus.FAILED -> Icons.Default.Refresh
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = "Download action",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Progress bar indicator
        when (status) {
            DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED -> {
                LinearProgressIndicator(
                    progress = { (progress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (status == DownloadStatus.PAUSED) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            DownloadStatus.PENDING -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            DownloadStatus.COMPLETED -> {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            DownloadStatus.FAILED -> {
                LinearProgressIndicator(
                    progress = { (progress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.errorContainer,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(status: DownloadStatus) {
    val icon = when (status) {
        DownloadStatus.PENDING -> Icons.Default.Refresh
        DownloadStatus.DOWNLOADING -> Icons.Default.PlayArrow
        DownloadStatus.PAUSED -> Icons.Default.Warning
        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
        DownloadStatus.FAILED -> Icons.Default.Warning
    }
    val tint = when (status) {
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Preview(name = "Light Mode - Downloading", showBackground = true)
@Preview(name = "Dark Mode - Downloading", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DownloadProgressIndicatorDownloadingPreview() {
    MitOcwTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            DownloadProgressIndicator(
                progress = 65,
                status = DownloadStatus.DOWNLOADING,
                formattedSize = "142.5 MB / 220.0 MB",
                onActionClick = {}
            )
        }
    }
}

@Preview(name = "Light Mode - Failed", showBackground = true)
@Composable
private fun DownloadProgressIndicatorFailedPreview() {
    MitOcwTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            DownloadProgressIndicator(
                progress = 30,
                status = DownloadStatus.FAILED,
                formattedSize = "45.0 MB",
                onActionClick = {}
            )
        }
    }
}
