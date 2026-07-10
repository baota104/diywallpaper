package com.example.diywallpaper.domain.model

data class DiyElement(
    val type: DiyElementType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int,
    val srcName: String,
    val assetUrl: String?
)
