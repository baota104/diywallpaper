package com.example.diywallpaper.domain.usecase.wallpaper

import android.content.Intent
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperLauncher
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperSourceStore
import com.example.diywallpaper.domain.wallpaper.WallpaperAssetResolver
import javax.inject.Inject

class SetLiveWallpaperUseCase @Inject constructor(
    private val wallpaperAssetResolver: WallpaperAssetResolver,
    private val liveWallpaperSourceStore: LiveWallpaperSourceStore,
    private val liveWallpaperLauncher: LiveWallpaperLauncher
) {
    suspend operator fun invoke(
        source: WallpaperApplySource.LiveVideo
    ): AppResult<Intent> {
        return when (val resolvedFile = wallpaperAssetResolver.resolveLiveVideo(source)) {
            is AppResult.Success -> {
                when (val stored = liveWallpaperSourceStore.saveVideoPath(resolvedFile.data.absolutePath)) {
                    is AppResult.Success -> liveWallpaperLauncher.createLaunchIntent()
                    is AppResult.Error -> stored
                }
            }

            is AppResult.Error -> resolvedFile
        }
    }
}
