package com.example.diywallpaper.data.wallpaper

import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class VideoLiveWallpaperService : WallpaperService() {
    @Inject
    lateinit var liveWallpaperSourceStore: SharedPrefsLiveWallpaperSourceStore

    override fun onCreateEngine(): Engine = VideoWallpaperEngine()

    inner class VideoWallpaperEngine : Engine() {
        private var player: ExoPlayer? = null
        private var currentVideoPath: String? = null
        private var currentHolder: SurfaceHolder? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            ensurePlayer(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                currentHolder?.let(::ensurePlayer)
            } else {
                player?.playWhenReady = false
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            currentHolder = null
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            releasePlayer()
            super.onDestroy()
        }

        private fun ensurePlayer(holder: SurfaceHolder) {
            val videoPath = liveWallpaperSourceStore.getVideoPath()
                ?.takeIf { File(it).exists() }
                ?: return

            if (player != null && currentVideoPath == videoPath) {
                player?.setVideoSurfaceHolder(holder)
                player?.playWhenReady = true
                return
            }

            releasePlayer()
            val localPlayer = player ?: ExoPlayer.Builder(applicationContext).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                setVideoSurfaceHolder(holder)
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
                prepare()
                playWhenReady = true
            }.also { player = it }

            currentVideoPath = videoPath
            localPlayer.setVideoSurfaceHolder(holder)
        }

        private fun releasePlayer() {
            player?.release()
            player = null
            currentVideoPath = null
        }
    }
}
