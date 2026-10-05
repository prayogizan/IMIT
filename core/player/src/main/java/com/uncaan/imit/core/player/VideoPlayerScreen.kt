package com.uncaan.imit.core.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView

/**
 * Screen composable that embeds the Media3 video playback surface via [PlayerView].
 *
 * Connects asynchronously to [VideoPlaybackService] through the [VideoPlayerManager] client facade.
 * Manages video loading on URL change, surface detachment on disposal without interrupting
 * ongoing background audio playback, and top overlay controls (back navigation and PiP).
 *
 * @param videoUrl The remote URL or local file URI of the video to play.
 * @param playerManager The [VideoPlayerManager] client facade managing media playback.
 * @param onBackClick Callback invoked when the user taps the back button.
 * @param modifier Optional [Modifier] applied to the root container.
 * @param title Optional title of the video for system metadata display. Defaults to empty.
 * @param subtitle Optional subtitle, instructor, or course name. Defaults to empty.
 * @param artworkUri Optional URI pointing to video artwork or thumbnail. Defaults to null.
 * @param isBackgroundPlaybackEnabled Whether audio playback continues in background service when leaving screen. Defaults to false.
 */
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    playerManager: VideoPlayerManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String = "",
    artworkUri: String? = null,
    isBackgroundPlaybackEnabled: Boolean = false
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    val isPipSupported = remember(context) { PipHelper.isPipSupported(context) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val currentIsBackgroundPlaybackEnabled by rememberUpdatedState(isBackgroundPlaybackEnabled)

    if (!isInspectionMode) {
        DisposableEffect(Unit) {
            onDispose {
                if (!currentIsBackgroundPlaybackEnabled) {
                    playerManager.pause()
                }
            }
        }

        LaunchedEffect(Unit) {
            controller = playerManager.getController()
        }

        LaunchedEffect(videoUrl, title, subtitle, artworkUri) {
            if (videoUrl.isNotBlank()) {
                playerManager.playVideo(
                    uri = videoUrl,
                    title = title,
                    subtitle = subtitle,
                    artworkUri = artworkUri
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isInspectionMode) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                    }
                },
                update = { playerView ->
                    playerView.player = controller
                },
                onRelease = { playerView ->
                    playerView.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        VideoPlayerTopControls(
            isPipSupported = isPipSupported,
            onBackClick = onBackClick,
            onPipClick = {
                val activity = context.findActivity()
                if (activity != null) {
                    PipHelper.enterPip(activity)
                }
            },
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

/**
 * Overlay control bar displaying navigation and action buttons for the player screen.
 *
 * @param isPipSupported Whether PiP button should be visible.
 * @param onBackClick Callback for back navigation.
 * @param onPipClick Callback for triggering PiP mode.
 * @param modifier Optional modifier for the top controls layout.
 */
@Composable
private fun VideoPlayerTopControls(
    isPipSupported: Boolean,
    onBackClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (isPipSupported) {
            IconButton(onClick = onPipClick) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Picture in Picture",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Recursively traverses the [Context] hierarchy to find the enclosing [Activity].
 */
private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VideoPlayerTopControlsPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayerTopControls(
            isPipSupported = true,
            onBackClick = {},
            onPipClick = {}
        )
    }
}
