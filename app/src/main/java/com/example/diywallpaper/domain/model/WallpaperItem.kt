package com.example.diywallpaper.domain.model

data class WallpaperItem(
    val id: String,
    val categoryId: String,
    val type: WallpaperType,
    val rank: Int,
    val thumbUrl: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val isFavorite: Boolean
)
