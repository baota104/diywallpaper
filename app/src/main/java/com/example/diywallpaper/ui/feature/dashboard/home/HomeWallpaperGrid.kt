package com.example.diywallpaper.ui.feature.dashboard.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.WallpaperType
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.ui.components.wallpaper.DiyTemplatePreviewCard
import com.example.diywallpaper.ui.components.wallpaper.WallpaperPreviewCard
import com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo
import com.example.diywallpaper.ui.preview.video.rememberVideoPreviewManager
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HomeWallpaperGrid(
    feedItems: List<HomeFeedItem>,
    activeVideoIds: Set<String>,
    activeDiyAnimationIds: Set<String>,
    onViewportChanged: (Map<String, PreviewVisibilityInfo>, Boolean) -> Unit,
    onWallpaperFavoriteClick: (String) -> Unit,
    onDiyFavoriteClick: (String) -> Unit,
    onOpenPreview: (sourceType: PreviewSourceType, categoryId: String, itemId: String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val videoPreviewManager = rememberVideoPreviewManager()
    val previewableItemIds = remember(feedItems) {
        feedItems
            .asSequence()
            .filter { item ->
                when (item) {
                    is HomeFeedItem.DiyEntry -> {
                        item.template.type == DiyTemplateType.DIY_LIVE &&
                            !item.template.diyAnimationUrl.isNullOrBlank()
                    }
                    is HomeFeedItem.WallpaperEntry -> {
                        item.wallpaper.type == WallpaperType.LIVE_VIDEO &&
                            !item.wallpaper.videoUrl.isNullOrBlank()
                    }
                }
            }
            .map { it.id }
            .toSet()
    }

    DisposableEffect(feedItems) {
        onDispose { onViewportChanged(emptyMap(), false) }
    }

    LaunchedEffect(gridState, feedItems) {
        snapshotFlow {
            val viewportStart = gridState.layoutInfo.viewportStartOffset
            val viewportEnd = gridState.layoutInfo.viewportEndOffset

            val visibleItems = gridState.layoutInfo.visibleItemsInfo
                .mapNotNull { info ->
                    val item = feedItems.getOrNull(info.index) ?: return@mapNotNull null
                    if (item.id !in previewableItemIds) return@mapNotNull null
                    val visibleStart = maxOf(info.offset.y, viewportStart)
                    val visibleEnd = minOf(info.offset.y + info.size.height, viewportEnd)
                    val visibleSize = (visibleEnd - visibleStart).coerceAtLeast(0)
                    val visibleFraction = if (info.size.height == 0) 0f else {
                        visibleSize.toFloat() / info.size.height.toFloat()
                    }

                    item.id to PreviewVisibilityInfo(
                        visibleFraction = visibleFraction,
                        isFullyVisible = visibleFraction >= 0.999f
                    )
                }
                .toMap()

            visibleItems to gridState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collect { (visibleItems, isScrolling) ->
                onViewportChanged(visibleItems, isScrolling)
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        state = gridState,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = feedItems.size,
            key = { index -> feedItems[index].stableGridKey() },
            contentType = { index ->
                when (val item = feedItems[index]) {
                    is HomeFeedItem.DiyEntry -> "diy"
                    is HomeFeedItem.WallpaperEntry -> when (item.wallpaper.type) {
                        WallpaperType.LIVE_VIDEO -> "live_wallpaper"
                        WallpaperType.STATIC_2D, WallpaperType.UNKNOWN -> "static_wallpaper"
                    }
                }
            }
        ) { index ->
            when (val item = feedItems[index]) {
                is HomeFeedItem.DiyEntry -> {
                    DiyTemplatePreviewCard(
                        template = item.template,
                        isAnimationActive = item.id in activeDiyAnimationIds,
                        isFavorite = item.template.isFavorite,
                        onFavoriteClick = { onDiyFavoriteClick(item.template.id) },
                        onClick = { onOpenPreview(PreviewSourceType.DIY, item.categoryId, item.id) }
                    )
                }
                is HomeFeedItem.WallpaperEntry -> {
                    WallpaperPreviewCard(
                        item = item.wallpaper,
                        isPlaying = item.id in activeVideoIds,
                        videoPreviewManager = videoPreviewManager,
                        isFavorite = item.wallpaper.isFavorite,
                        onFavoriteClick = { onWallpaperFavoriteClick(item.wallpaper.id) },
                        onClick = {
                            onOpenPreview(
                                PreviewSourceType.WALLPAPER,
                                item.categoryId,
                                item.id
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun HomeFeedItem.stableGridKey(): String {
    return when (this) {
        is HomeFeedItem.DiyEntry -> "diy_$id"
        is HomeFeedItem.WallpaperEntry -> "wallpaper_$id"
    }
}
