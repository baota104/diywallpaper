package com.example.diywallpaper.domain.model.preview

sealed interface WallpaperApplySource {
    data class StaticImage(
        val itemId: String,
        val imageUrl: String,
        val localPath: String? = null
    ) : WallpaperApplySource

    data class LiveVideo(
        val itemId: String,
        val videoUrl: String,
        val localPath: String? = null
    ) : WallpaperApplySource

    data class RenderedDiyImage(
        val designId: String,
        val exportPath: String
    ) : WallpaperApplySource
}
