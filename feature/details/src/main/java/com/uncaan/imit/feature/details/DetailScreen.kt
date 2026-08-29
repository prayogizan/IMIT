package com.uncaan.imit.feature.details

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.uncaan.imit.core.designsystem.component.DownloadProgressIndicator
import com.uncaan.imit.core.designsystem.component.ErrorMessage
import com.uncaan.imit.core.designsystem.component.LoadingShimmer
import com.uncaan.imit.core.designsystem.theme.MitOcwTheme
import com.uncaan.imit.core.designsystem.theme.spacing
import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.PlayableStream
import com.uncaan.imit.core.model.VideoDetail
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailScreen(
    identifier: String,
    onBackClick: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(identifier) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigateToPlayer by viewModel.navigateToPlayer.collectAsState()

    LaunchedEffect(navigateToPlayer) {
        navigateToPlayer?.let { videoUrl ->
            onPlayVideo(videoUrl)
            viewModel.onPlayerNavigated()
        }
    }

    DetailScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailScreenContent(
    uiState: DetailUiState,
    onEvent: (DetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? DetailUiState.Success)?.detail?.title ?: "Lecture Detail",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
            when (uiState) {
                is DetailUiState.Loading -> {
                    DetailShimmer()
                }

                is DetailUiState.Error -> {
                    ErrorMessage(
                        message = uiState.message,
                        onRetry = { onEvent(DetailUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DetailUiState.Success -> {
                    DetailSuccessContent(
                        state = uiState,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailSuccessContent(
    state: DetailUiState.Success,
    onEvent: (DetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = spacing.extraLarge)
    ) {
        // Thumbnail & Stream Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            SubcomposeAsyncImage(
                model = state.detail.thumbnailUrl,
                contentDescription = state.detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    LoadingShimmer(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(0.dp)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            )

            // Scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // Center Play Button
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    .clickable { onEvent(DetailUiEvent.StreamVideo) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Lecture",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Quality Badge overlay
            state.selectedStream?.let { stream ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(spacing.small),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = stream.qualityLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = spacing.small, vertical = 2.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Lecture Title
            Text(
                text = state.detail.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Creator / Instructor
            if (state.detail.creator.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = state.detail.creator,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Button(
                    onClick = { onEvent(DetailUiEvent.StreamVideo) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.extraSmall))
                    Text("Watch Video")
                }

                if (state.downloadStatus == null || state.downloadStatus == DownloadStatus.FAILED) {
                    OutlinedButton(
                        onClick = { onEvent(DetailUiEvent.DownloadVideo) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                        Text("Download")
                    }
                }
            }

            // Download progress indicator if downloading or pending
            if (state.downloadStatus != null && state.downloadStatus != DownloadStatus.FAILED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    DownloadProgressIndicator(
                        progress = state.downloadProgress,
                        status = state.downloadStatus,
                        formattedSize = state.selectedStream?.formattedSize,
                        modifier = Modifier.padding(spacing.medium)
                    )
                }
            }

            // Quality & Format Selection Section
            if (state.detail.streams.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = "Select Quality / Format",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        state.detail.streams.forEach { stream ->
                            val isSelected = stream == state.selectedStream
                            FilterChip(
                                selected = isSelected,
                                onClick = { onEvent(DetailUiEvent.SelectQuality(stream)) },
                                label = {
                                    Text(
                                        text = "${stream.qualityLabel} (${stream.formattedSize})"
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null,
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                }
            }

            // Description Section
            if (state.detail.description.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = state.detail.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (state.isDescriptionExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    TextButton(
                        onClick = { onEvent(DetailUiEvent.ToggleDescription) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (state.isDescriptionExpanded) "Show less" else "Show more",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = if (state.isDescriptionExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailShimmer(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = spacing.large)
    ) {
        // Thumbnail shimmer
        LoadingShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(0.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Title shimmer
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(24.dp)
            )
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
            )

            // Instructor shimmer
            LoadingShimmer(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
            )

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            // Action buttons shimmer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                LoadingShimmer(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
                LoadingShimmer(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            // Quality section title shimmer
            LoadingShimmer(
                modifier = Modifier
                    .width(160.dp)
                    .height(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                LoadingShimmer(
                    modifier = Modifier
                        .width(100.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                LoadingShimmer(
                    modifier = Modifier
                        .width(100.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            // Description lines shimmer
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(14.dp)
            )
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailScreenSuccessPreview() {
    val sampleStreams = listOf(
        PlayableStream(
            fileName = "lecture01_720p.mp4",
            format = "mp4",
            sizeBytes = 250 * 1024 * 1024L,
            durationSeconds = 3000.0,
            height = 720,
            width = 1280,
            streamUrl = "https://archive.org/download/mit-ocw-lec01/lec01_720p.mp4"
        ),
        PlayableStream(
            fileName = "lecture01_360p.mp4",
            format = "mp4",
            sizeBytes = 120 * 1024 * 1024L,
            durationSeconds = 3000.0,
            height = 360,
            width = 640,
            streamUrl = "https://archive.org/download/mit-ocw-lec01/lec01_360p.mp4"
        )
    )

    val sampleDetail = VideoDetail(
        identifier = "mit-ocw-6.0001-lec01",
        title = "Lecture 1: What is Computation? - Introduction to Computer Science",
        description = "This lecture covers the foundational concepts of computer programming, algorithms, and computational problem solving using Python 3.",
        creator = "Prof. Eric Grimson",
        streams = sampleStreams,
        thumbnailUrl = "https://archive.org/services/img/mit-ocw-6.0001"
    )

    MitOcwTheme {
        DetailScreenContent(
            uiState = DetailUiState.Success(
                detail = sampleDetail,
                selectedStream = sampleStreams.first(),
                downloadStatus = null
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}
