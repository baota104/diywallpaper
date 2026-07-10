package com.example.diywallpaper.domain.model.preview

import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType

fun WallpaperItem.toPreviewCatalogItem(categoryTitle: String): PreviewCatalogItem {
    val kind = when (type) {
        WallpaperType.LIVE_VIDEO -> PreviewItemKind.WALLPAPER_LIVE
        WallpaperType.STATIC_2D,
        WallpaperType.UNKNOWN -> PreviewItemKind.WALLPAPER_STATIC
    }

    return PreviewCatalogItem(
        id = id,
        categoryId = categoryId,
        rank = rank,
        title = categoryTitle,
        kind = kind,
        thumbUrl = thumbUrl.ifBlank { imageUrl.orEmpty() },
        isFavorite = isFavorite,
        wallpaperSource = WallpaperSource(
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )
    )
}

fun DiyTemplate.toPreviewCatalogItem(): PreviewCatalogItem {
    val isLive = type == DiyTemplateType.DIY_LIVE
    return PreviewCatalogItem(
        id = id,
        categoryId = DIY_PREVIEW_CATEGORY_ID,
        rank = rank,
        title = DIY_PREVIEW_TITLE,
        kind = if (isLive) PreviewItemKind.DIY_LIVE else PreviewItemKind.DIY_STATIC,
        thumbUrl = thumbUrl,
        isFavorite = isFavorite,
        diySource = DiySource(
            diyDataUrl = diyDataUrl,
            diyAnimationUrl = diyAnimationUrl,
            previewMode = if (isLive) {
                DiyPreviewMode.ANIMATION_JSON
            } else {
                DiyPreviewMode.STATIC_ONLY
            }
        )
    )
}

fun BackgroundCreateItem.toPreviewCatalogItem(): PreviewCatalogItem {
    return PreviewCatalogItem(
        id = id,
        categoryId = CREATE_FROM_SCRATCH_CATEGORY_ID,
        rank = rank,
        title = name,
        kind = PreviewItemKind.CREATE_FROM_SCRATCH,
        thumbUrl = thumbnailUrl,
        isFavorite = false,
        scratchSource = ScratchSource(templateBackgroundUrl = imageUrl)
    )
}

fun PreviewCatalogItem.toPlayableSource(): PreviewPlayableSource? {
    return when (kind) {
        PreviewItemKind.WALLPAPER_STATIC -> {
            val source = wallpaperSource ?: return null
            val imageUrl = source.imageUrl ?: thumbUrl.takeIf { it.isNotBlank() } ?: return null
            PreviewPlayableSource.StaticWallpaper(
                itemId = id,
                imageUrl = imageUrl,
                thumbUrl = thumbUrl
            )
        }

        PreviewItemKind.WALLPAPER_LIVE -> {
            val source = wallpaperSource ?: return null
            val videoUrl = source.videoUrl ?: return null
            PreviewPlayableSource.LiveWallpaper(
                itemId = id,
                videoUrl = videoUrl,
                thumbUrl = thumbUrl
            )
        }

        PreviewItemKind.DIY_STATIC -> {
            val source = diySource ?: return null
            PreviewPlayableSource.DiyStatic(
                itemId = id,
                diyDataUrl = source.diyDataUrl,
                thumbUrl = thumbUrl
            )
        }

        PreviewItemKind.DIY_LIVE -> {
            val source = diySource ?: return null
            val animationUrl = source.diyAnimationUrl ?: return null
            PreviewPlayableSource.DiyLive(
                itemId = id,
                diyDataUrl = source.diyDataUrl,
                diyAnimationUrl = animationUrl,
                thumbUrl = thumbUrl
            )
        }

        PreviewItemKind.CREATE_FROM_SCRATCH -> {
            PreviewPlayableSource.Scratch(
                itemId = id,
                templateBackgroundUrl = scratchSource?.templateBackgroundUrl
            )
        }
    }
}

fun PreviewCatalogItem.primaryAction(): PreviewPrimaryAction {
    return when (kind) {
        PreviewItemKind.WALLPAPER_STATIC -> PreviewPrimaryAction.SET_WALLPAPER
        PreviewItemKind.WALLPAPER_LIVE -> PreviewPrimaryAction.SET_LIVE_WALLPAPER
        PreviewItemKind.DIY_STATIC,
        PreviewItemKind.DIY_LIVE -> PreviewPrimaryAction.EDIT_DIY
        PreviewItemKind.CREATE_FROM_SCRATCH -> PreviewPrimaryAction.CREATE_FROM_SCRATCH
    }
}

private const val DIY_PREVIEW_TITLE = "DIY"
const val DIY_PREVIEW_CATEGORY_ID = "DIY"
const val CREATE_FROM_SCRATCH_CATEGORY_ID = "create_from_scratch"
