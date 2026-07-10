package com.example.diywallpaper.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackgroundCreateDto(
    val id: Int = 0,
    @SerialName("category_rank")
    val categoryRank: Int = Int.MAX_VALUE,
    val name: String = "",
    val data: String = ""
)
