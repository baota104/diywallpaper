package com.example.diywallpaper.ui.preview.core

import com.example.diywallpaper.domain.model.HomeFeedItem

interface PreviewPlaybackCoordinator {
    fun computeActivePreviews(
        items: List<HomeFeedItem>,
        viewportState: PreviewViewportState
    ): ActivePreviewState
}
