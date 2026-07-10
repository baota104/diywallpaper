package com.example.diywallpaper.ui.preview.video

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun rememberVideoPreviewManager(): VideoPreviewManager {
    val context = LocalContext.current
    val manager = remember(context) { VideoPreviewManager(context) }

    DisposableEffect(manager) {
        onDispose { manager.dispose() }
    }

    return manager
}

@Composable
fun VideoPreviewSurface(
    itemId: String,
    videoUrl: String,
    manager: VideoPreviewManager,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val player = rememberManagedPlayer(
        itemId = itemId,
        videoUrl = videoUrl,
        manager = manager
    )
    var hasRenderedFirstFrame by remember(itemId, videoUrl) { mutableStateOf(false) }

    DisposableEffect(player, isActive) {
        if (!isActive) {
            hasRenderedFirstFrame = false
        }

        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                this.player = player
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(Color.TRANSPARENT)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.graphicsLayer {
            alpha = if (hasRenderedFirstFrame && isActive) 1f else 0f
        },
        update = { playerView ->
            playerView.player = player
        }
    )
}

@Composable
private fun rememberManagedPlayer(
    itemId: String,
    videoUrl: String,
    manager: VideoPreviewManager
): ExoPlayer {
    val player = remember(itemId, videoUrl, manager) {
        manager.acquirePlayer(itemId = itemId, videoUrl = videoUrl)
    }

    DisposableEffect(itemId, videoUrl, manager) {
        onDispose { manager.releasePlayer(itemId) }
    }

    return player
}
