package com.example.diywallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "background_create_items")
data class BackgroundCreateEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    val name: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val localPath: String?,
    val rawJson: String?
)
