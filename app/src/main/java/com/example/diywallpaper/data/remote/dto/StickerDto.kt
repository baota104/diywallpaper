package com.example.diywallpaper.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StickerDto(
    val id: Int = 0,
    val rank: Int = Int.MAX_VALUE,
    val stickers: String = ""
)
