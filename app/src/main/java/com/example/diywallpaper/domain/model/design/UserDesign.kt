package com.example.diywallpaper.domain.model.design

data class UserDesign(
    val id: String,
    val sourceType: DesignSourceType,
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

enum class DesignSourceType {
    DIY_TEMPLATE,
    SCRATCH
}
