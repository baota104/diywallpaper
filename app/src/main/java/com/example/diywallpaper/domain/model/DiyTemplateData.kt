package com.example.diywallpaper.domain.model

data class DiyTemplateData(
    val width: Int,
    val height: Int,
    val background: String,
    val elements: List<DiyElement>,
    val placeholders: List<PhotoPlaceholder>
)
