package com.example.diywallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_categories")
data class WallpaperCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconUrl: String?,
    val rank: Int,
    val rawJson: String?
)
