package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.data.local.entity.WallpaperCategoryEntity
import com.example.diywallpaper.data.local.entity.WallpaperItemEntity
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType

internal const val METADATA_TTL_MS = 24L * 60L * 60L * 1000L

internal fun isStale(lastSyncedAt: Long): Boolean {
    if (lastSyncedAt <= 0L) return true
    return System.currentTimeMillis() - lastSyncedAt >= METADATA_TTL_MS
}

internal fun WallpaperCategoryEntity.toDomain(items: List<WallpaperItemEntity>): WallpaperCategory {
    return WallpaperCategory(
        id = id,
        name = name,
        iconUrl = iconUrl,
        rank = rank,
        items = items
            .sortedBy { it.rank }
            .map { item ->
                WallpaperItem(
                    id = item.id,
                    categoryId = item.categoryId,
                    type = item.type.toWallpaperType(),
                    rank = item.rank,
                    thumbUrl = item.thumbUrl.orEmpty(),
                    imageUrl = item.imageUrl,
                    videoUrl = item.videoUrl,
                    isFavorite = item.isFavorite
                )
            }
    )
}

internal fun WallpaperType.toStorageValue(): String = when (this) {
    WallpaperType.STATIC_2D -> "2d"
    WallpaperType.LIVE_VIDEO -> "live"
    WallpaperType.UNKNOWN -> "unknown"
}

internal fun String?.toWallpaperType(): WallpaperType = when (this) {
    "2d" -> WallpaperType.STATIC_2D
    "live" -> WallpaperType.LIVE_VIDEO
    else -> WallpaperType.UNKNOWN
}

internal fun Throwable.toAppError(source: String): AppError = AppError.Unknown(this)
