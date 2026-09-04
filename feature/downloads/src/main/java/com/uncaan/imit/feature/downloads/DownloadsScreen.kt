package com.uncaan.imit.feature.downloads

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uncaan.imit.core.designsystem.component.DownloadProgressIndicator
import com.uncaan.imit.core.designsystem.component.ErrorMessage
import com.uncaan.imit.core.designsystem.component.LoadingShimmer
import com.uncaan.imit.core.designsystem.theme.MitOcwTheme
import com.uncaan.imit.core.designsystem.theme.spacing
import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.DownloadTask
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

/**
 * Main screen composable for the Offline Downloads Library.
 *
 * Displays downloaded and in-progress video lectures with reactive Room updates,
 * real-time device storage telemetry, offline playback navigation, and delete confirmation flows.
 *
 * @param onPlayVideo Callback invoked when the user selects a video for playback.
 *                    Passes the local file path or remote fallback URL.
 * @param modifier Optional [Modifier] applied to the root container.
 * @param viewModel [DownloadsViewModel] instance injected via Koin by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onPlayVideo: (videoUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DownloadsUiState.Loading -> {
                    DownloadsShimmerList()
                }

                is DownloadsUiState.Empty -> {
                    DownloadsEmptyState(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DownloadsUiState.Success -> {
                    DownloadsContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onPlayVideo = onPlayVideo,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DownloadsUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            showDeleteDialog?.let { (identifier, localFilePath) ->
                AlertDialog(
                    onDismissRequest = {
                        viewModel.onEvent(DownloadsUiEvent.DismissDeleteDialog)
                    },
                    title = {
                        Text(
                            text = "Delete Download",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to remove this video from your device storage?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.onEvent(DownloadsUiEvent.ConfirmDelete(identifier, localFilePath))
                            }
                        ) {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.onEvent(DownloadsUiEvent.DismissDeleteDialog)
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DownloadsContent(
    state: DownloadsUiState.Success,
    onEvent: (DownloadsUiEvent) -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium)
    ) {
        StorageTelemetryCard(
            totalStorageUsedBytes = state.totalStorageUsedBytes,
            availableStorageMb = state.availableStorageMb,
            modifier = Modifier.padding(vertical = spacing.small)
        )

        Spacer(modifier = Modifier.height(spacing.small))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(bottom = spacing.large)
        ) {
            items(
                items = state.downloads,
                key = { it.identifier }
            ) { task ->
                DownloadItemCard(
                    task = task,
                    onPlay = {
                        val playTarget = task.localFilePath ?: task.downloadUrl
                        onPlayVideo(playTarget)
                    },
                    onDelete = {
                        onEvent(DownloadsUiEvent.DeleteDownload(task.identifier, task.localFilePath))
                    }
                )
            }
        }
    }
}

@Composable
private fun StorageTelemetryCard(
    totalStorageUsedBytes: Long,
    availableStorageMb: Long,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val usedMb = (totalStorageUsedBytes / (1024L * 1024L)).coerceAtLeast(0L)
    val totalCapacityMb = (usedMb + availableStorageMb).coerceAtLeast(1L)
    val usedFraction = (usedMb.toFloat() / totalCapacityMb.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage Used: ${formatBytes(totalStorageUsedBytes)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatMegabytes(availableStorageMb)} free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DownloadItemCard(
    task: DownloadTask,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.fileName.isNotBlank()) {
                        Text(
                            text = task.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete download",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DownloadProgressIndicator(
                progress = task.progress,
                status = task.status,
                formattedSize = formatBytes(task.fileSizeBytes)
            )

            if (task.status == DownloadStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onPlay,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                        Text(text = "Play Offline")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsEmptyState(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier.padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = "No downloaded lectures",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(spacing.extraSmall))
        Text(
            text = "Lectures you download will appear here for offline playback without internet connection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DownloadsShimmerList(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        LoadingShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = MaterialTheme.shapes.medium
        )
        repeat(4) {
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}

private fun formatMegabytes(mb: Long): String {
    if (mb <= 0) return "0 MB"
    val gb = mb / 1024.0
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        "$mb MB"
    }
}

@Preview(name = "Light Mode - Success", showBackground = true)
@Preview(name = "Dark Mode - Success", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DownloadsScreenSuccessPreview() {
    val sampleTasks = listOf(
        DownloadTask(
            identifier = "mit-ocw-6.0001-lec01",
            title = "Lecture 1: What is Computation?",
            description = "Introduction to Computer Science and Programming in Python",
            fileName = "lec01_720p.mp4",
            downloadUrl = "https://archive.org/download/mit-ocw-6.0001/lec01.mp4",
            localFilePath = "/data/user/0/com.uncaan.imit/files/mit_ocw_videos/lec01.mp4",
            fileSizeBytes = 245 * 1024 * 1024L,
            progress = 100,
            status = DownloadStatus.COMPLETED,
            downloadedAt = System.currentTimeMillis()
        ),
        DownloadTask(
            identifier = "mit-ocw-18.06-lec02",
            title = "Lecture 2: Elimination with Matrices",
            description = "Linear Algebra with Prof. Gilbert Strang",
            fileName = "lec02_360p.mp4",
            downloadUrl = "https://archive.org/download/mit-ocw-18.06/lec02.mp4",
            localFilePath = null,
            fileSizeBytes = 180 * 1024 * 1024L,
            progress = 62,
            status = DownloadStatus.DOWNLOADING,
            downloadedAt = 0L
        )
    )

    MitOcwTheme {
        DownloadsContent(
            state = DownloadsUiState.Success(
                downloads = sampleTasks,
                totalStorageUsedBytes = 245 * 1024 * 1024L,
                availableStorageMb = 14500L
            ),
            onEvent = {},
            onPlayVideo = {}
        )
    }
}

@Preview(name = "Light Mode - Empty", showBackground = true)
@Preview(name = "Dark Mode - Empty", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DownloadsScreenEmptyPreview() {
    MitOcwTheme {
        DownloadsEmptyState(modifier = Modifier.fillMaxSize())
    }
}
