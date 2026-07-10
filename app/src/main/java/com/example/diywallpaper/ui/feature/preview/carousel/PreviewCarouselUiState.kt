package com.example.diywallpaper.ui.feature.preview.carousel

import com.example.diywallpaper.domain.model.preview.PreviewCatalogItem
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction

data class PreviewCarouselUiState(
    val isLoading: Boolean = true,
    val categoryId: String = "",
    val currentIndex: Int = 0,
    val initialIndex: Int = 0,
    val items: List<PreviewCatalogItem> = emptyList(),
    val primaryItemId: String? = null,
    val activePlaybackIds: Set<String> = emptySet(),
    val neighborPlaybackIds: Set<String> = emptySet(),
    val currentAction: PreviewPrimaryAction? = null,
    val errorMessage: String? = null
) {
    val currentItem: PreviewCatalogItem?
        get() = items.getOrNull(currentIndex)
}
