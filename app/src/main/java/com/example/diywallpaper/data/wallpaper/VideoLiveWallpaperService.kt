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

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            ensurePlayer(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            player?.playWhenReady = visible
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
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

            val localPlayer = player ?: ExoPlayer.Builder(applicationContext).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                setVideoSurfaceHolder(holder)
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
                prepare()
                playWhenReady = true
            }.also { player = it }

            localPlayer.setVideoSurfaceHolder(holder)
        }

        private fun releasePlayer() {
            player?.release()
            player = null
        }
    }
}
