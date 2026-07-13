package com.example.diywallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_designs")
data class UserDesignEntity(
    @PrimaryKey val id: String,
    val sourceType: String,
    val title: String?,
    val thumbnailPath: String?,
    val previewPath: String?,
    val templateId: String?,
    val projectFilePath: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val exportedImagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long,
    val isDeleted: Boolean,
    val schemaVersion: Int
)
