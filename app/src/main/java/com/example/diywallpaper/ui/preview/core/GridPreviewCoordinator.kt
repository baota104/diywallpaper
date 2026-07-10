package com.example.diywallpaper.ui.preview.core

import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.model.WallpaperType
import javax.inject.Inject

class GridPreviewCoordinator @Inject constructor() : PreviewPlaybackCoordinator {
    private val policy = HomePreviewPolicy()

    override fun computeActivePreviews(
        items: List<HomeFeedItem>,
        viewportState: PreviewViewportState
    ): ActivePreviewState {
        val minVisibleFraction = if (viewportState.isScrolling) {
            policy.minVisibleFractionScrolling
        } else {
            policy.minVisibleFractionIdle
        }

        val activeVisibleIds = viewportState.visibleItems
            .asSequence()
            .filter { (_, visibility) -> visibility.visibleFraction >= minVisibleFraction }
            .sortedByDescending { (_, visibility) -> visibility.visibleFraction }
            .map { (id, _) -> id }
            .toSet()

        val activeVideoIds = items
            .asSequence()
            .filterIsInstance<HomeFeedItem.WallpaperEntry>()
            .map { it.wallpaper }
            .filter { it.type == WallpaperType.LIVE_VIDEO && !it.videoUrl.isNullOrBlank() }
            .map { it.id }
            .filter { it in activeVisibleIds }
            .toSet()

        val activeDiyAnimationIds = items
            .asSequence()
            .filterIsInstance<HomeFeedItem.DiyEntry>()
            .map { it.template }
            .filter { it.type == DiyTemplateType.DIY_LIVE && !it.diyAnimationUrl.isNullOrBlank() }
            .map { it.id }
            .filter { it in activeVisibleIds }
            .toSet()

        return ActivePreviewState(
            activeVideoIds = activeVideoIds,
            activeDiyAnimationIds = activeDiyAnimationIds
        )
    }
}
