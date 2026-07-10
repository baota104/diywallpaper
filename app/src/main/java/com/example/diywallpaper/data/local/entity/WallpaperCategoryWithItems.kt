package com.example.diywallpaper.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WallpaperCategoryWithItems(
    @Embedded val category: WallpaperCategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val items: List<WallpaperItemEntity>
)
