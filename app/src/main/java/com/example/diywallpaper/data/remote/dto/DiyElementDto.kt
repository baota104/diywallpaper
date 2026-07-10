package com.example.diywallpaper.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiyElementDto(
    val type: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val angle: Float = 0f,
    val layoutIndex: Int = Int.MAX_VALUE,
    val srcName: String = ""
)
