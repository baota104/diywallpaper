package com.example.diywallpaper.domain.wallpaper

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import java.io.File

interface WallpaperAssetResolver {
    suspend fun resolveStaticImage(
        source: WallpaperApplySource.StaticImage
    ): AppResult<File>

    suspend fun resolveLiveVideo(
        source: WallpaperApplySource.LiveVideo
    ): AppResult<File>
}
