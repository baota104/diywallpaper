package com.example.diywallpaper.domain.model.preview

data class PreviewCatalogItem(
    val id: String,
    val categoryId: String,
    val rank: Int,
    val title: String,
    val kind: PreviewItemKind,
    val thumbUrl: String,
    val isFavorite: Boolean,
    val wallpaperSource: WallpaperSource? = null,
    val diySource: DiySource? = null,
    val scratchSource: ScratchSource? = null
)
