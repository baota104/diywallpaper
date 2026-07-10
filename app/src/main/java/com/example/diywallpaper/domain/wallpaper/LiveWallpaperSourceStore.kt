package com.example.diywallpaper.domain.wallpaper

import com.example.diywallpaper.core.result.AppResult

interface LiveWallpaperSourceStore {
    suspend fun saveVideoPath(path: String): AppResult<Unit>

    fun getVideoPath(): String?
}
