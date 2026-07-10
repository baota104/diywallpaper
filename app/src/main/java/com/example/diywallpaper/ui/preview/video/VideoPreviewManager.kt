package com.example.diywallpaper.ui.preview.video

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoPreviewManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = ConcurrentHashMap<String, VideoPreviewSession>()

    fun acquirePlayer(itemId: String, videoUrl: String): ExoPlayer {
        val existing = sessions[itemId]
        if (existing != null && existing.videoUrl == videoUrl) {
            existing.releaseJob?.cancel()
            existing.releaseJob = null
            existing.attachCount += 1
            existing.player.playWhenReady = true
            return existing.player
        }

        existing?.dispose()

        val player = ExoPlayer.Builder(appContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }

        sessions[itemId] = VideoPreviewSession(
            videoUrl = videoUrl,
            player = player,
            attachCount = 1
        )
        return player
    }

    fun releasePlayer(itemId: String) {
        val session = sessions[itemId] ?: return
        session.attachCount = (session.attachCount - 1).coerceAtLeast(0)
        if (session.attachCount > 0) return

        session.player.playWhenReady = false
        session.releaseJob?.cancel()
        session.releaseJob = scope.launch {
            delay(IDLE_RELEASE_DELAY_MS)
            sessions.remove(itemId)?.dispose()
        }
    }

    fun dispose() {
        sessions.values.forEach { it.dispose() }
        sessions.clear()
        scope.cancel()
    }

    private data class VideoPreviewSession(
        val videoUrl: String,
        val player: ExoPlayer,
        var attachCount: Int,
        var releaseJob: Job? = null
    ) {
        fun dispose() {
            releaseJob?.cancel()
            player.release()
        }
    }

    private companion object {
        const val IDLE_RELEASE_DELAY_MS = 1_500L
    }
}
