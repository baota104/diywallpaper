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
    val assetUrl: String?,
    val title: String = "",
    val fontSize: Float = 50f,
    val fontColor: String = "#000000",
    val fontFamilyIndex: Int = 0,
    val maskName: String = "",
    val maskUrl: String? = null
)
