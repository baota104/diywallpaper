package com.example.diywallpaper.domain.model.preview

data class WallpaperSource(
    val imageUrl: String? = null,
    val videoUrl: String? = null
)

data class DiySource(
    val diyDataUrl: String,
    val diyAnimationUrl: String? = null,
    val previewMode: DiyPreviewMode
)

data class ScratchSource(
    val templateBackgroundUrl: String? = null
)

enum class DiyPreviewMode {
    STATIC_ONLY,
    ANIMATION_JSON
}
