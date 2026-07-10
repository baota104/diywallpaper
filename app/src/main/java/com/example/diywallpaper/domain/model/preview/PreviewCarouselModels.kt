package com.example.diywallpaper.domain.model.preview

data class PreviewCarouselData(
    val items: List<PreviewCatalogItem>,
    val initialIndex: Int
)

data class PreviewPlaybackWindow(
    val primaryItemId: String?,
    val activeItemIds: Set<String>,
    val neighborItemIds: Set<String>
)
