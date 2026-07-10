package com.example.diywallpaper.domain.usecase.preview

import com.example.diywallpaper.domain.model.preview.PreviewCatalogItem
import com.example.diywallpaper.domain.model.preview.PreviewPlaybackWindow
import kotlin.math.max
import kotlin.math.min

class PreviewCarouselPlaybackPolicy(
    private val neighborWindow: Int = 1
) {
    fun resolve(
        items: List<PreviewCatalogItem>,
        currentIndex: Int
    ): PreviewPlaybackWindow {
        if (items.isEmpty()) {
            return PreviewPlaybackWindow(
                primaryItemId = null,
                activeItemIds = emptySet(),
                neighborItemIds = emptySet()
            )
        }

        val safeIndex = currentIndex.coerceIn(items.indices)
        val activeIndices = (max(0, safeIndex - neighborWindow)..min(items.lastIndex, safeIndex + neighborWindow))
        val activeItemIds = activeIndices.map { items[it].id }.toSet()
        val primaryItemId = items[safeIndex].id
        return PreviewPlaybackWindow(
            primaryItemId = primaryItemId,
            activeItemIds = activeItemIds,
            neighborItemIds = activeItemIds - primaryItemId
        )
    }
}
