package com.example.diywallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_items")
data class WallpaperItemEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val type: String?,
    val rank: Int,
    val thumbUrl: String?,
    val imageUrl: String?,
    val videoUrl: String?,
    val isFavorite: Boolean,
    val localPath: String?,
    val rawJson: String?,
    val createdAt: Long,
    val updatedAt: Long
)
