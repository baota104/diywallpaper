package com.example.diywallpaper.domain.model

data class WallpaperCategory(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val rank: Int,
    val items: List<WallpaperItem>
)
