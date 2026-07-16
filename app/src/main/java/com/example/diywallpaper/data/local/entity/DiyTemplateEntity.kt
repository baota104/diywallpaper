package com.example.diywallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diy_templates")
data class DiyTemplateEntity(
    @PrimaryKey val id: String,
    val type: String,
    val rank: Int,
    val thumbUrl: String,
    val diyDataUrl: String,
    val dataZipUrl: String?,
    val diyAnimationUrl: String?,
    val isFavorite: Boolean,
    val diyDataLocalPath: String?,
    val diyAnimationLocalPath: String?,
    val rawJson: String?,
    val createdAt: Long,
    val updatedAt: Long
)
