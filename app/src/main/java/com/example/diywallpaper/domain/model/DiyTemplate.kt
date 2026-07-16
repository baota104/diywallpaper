package com.example.diywallpaper.domain.model

data class DiyTemplate(
    val id: String,
    val type: DiyTemplateType,
    val rank: Int,
    val thumbUrl: String,
    val diyDataUrl: String,
    val dataZipUrl: String? = null,
    val diyAnimationUrl: String?,
    val isFavorite: Boolean
)
