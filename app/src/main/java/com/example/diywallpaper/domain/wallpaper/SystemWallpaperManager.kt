package com.example.diywallpaper.domain.wallpaper

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperTarget
import java.io.File

interface SystemWallpaperManager {
    suspend fun setStaticWallpaper(
        file: File,
        target: WallpaperTarget = WallpaperTarget.BOTH
    ): AppResult<Unit>
}
