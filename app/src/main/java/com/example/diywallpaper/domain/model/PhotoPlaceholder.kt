package com.example.diywallpaper.domain.model

data class PhotoPlaceholder(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int,
    val maskName: String = "",
    val maskUrl: String? = null,
    val previewName: String = "",
    val previewUrl: String? = null
)
