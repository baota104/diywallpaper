package com.example.diywallpaper.domain.model.preview

sealed interface PreviewPlayableSource {
    data class StaticWallpaper(
        val itemId: String,
        val imageUrl: String,
        val thumbUrl: String
    ) : PreviewPlayableSource

    data class LiveWallpaper(
        val itemId: String,
        val videoUrl: String,
        val thumbUrl: String
    ) : PreviewPlayableSource

    data class DiyStatic(
        val itemId: String,
        val diyDataUrl: String,
        val thumbUrl: String
    ) : PreviewPlayableSource

    data class DiyLive(
        val itemId: String,
        val diyDataUrl: String,
        val diyAnimationUrl: String,
        val thumbUrl: String
    ) : PreviewPlayableSource

    data class Scratch(
        val itemId: String,
        val templateBackgroundUrl: String?
    ) : PreviewPlayableSource
}
