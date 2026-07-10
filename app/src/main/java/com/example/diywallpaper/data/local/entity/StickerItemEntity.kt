package com.example.diywallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sticker_items")
data class StickerItemEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    val stickerUrl: String,
    val thumbnailUrl: String,
    val localPath: String?,
    val isAnimated: Boolean,
    val rawJson: String?
)
