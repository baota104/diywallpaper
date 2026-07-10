package com.example.diywallpaper.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiyTemplateDataDto(
    val width: Int = 1080,
    val height: Int = 1920,
    val background: String = "#FFFFFF",
    val elements: List<DiyElementDto> = emptyList()
)
