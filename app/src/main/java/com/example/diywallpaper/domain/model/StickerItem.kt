package com.example.diywallpaper.domain.model

data class StickerItem(
    val id: String,
    val rank: Int,
    val stickerUrl: String,
    val thumbnailUrl: String,
    val isAnimated: Boolean
)
