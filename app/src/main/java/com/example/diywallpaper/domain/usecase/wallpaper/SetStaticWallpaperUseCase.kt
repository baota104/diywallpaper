package com.example.diywallpaper.domain.usecase.wallpaper

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.model.preview.WallpaperTarget
import com.example.diywallpaper.domain.wallpaper.SystemWallpaperManager
import com.example.diywallpaper.domain.wallpaper.WallpaperAssetResolver
import javax.inject.Inject

class SetStaticWallpaperUseCase @Inject constructor(
    private val wallpaperAssetResolver: WallpaperAssetResolver,
    private val systemWallpaperManager: SystemWallpaperManager
) {
    suspend operator fun invoke(
        source: WallpaperApplySource.StaticImage,
        target: WallpaperTarget = WallpaperTarget.BOTH
    ): AppResult<Unit> {
        return when (val resolvedFile = wallpaperAssetResolver.resolveStaticImage(source)) {
            is AppResult.Success -> systemWallpaperManager.setStaticWallpaper(
                file = resolvedFile.data,
                target = target
            )

            is AppResult.Error -> resolvedFile
        }
    }
}
