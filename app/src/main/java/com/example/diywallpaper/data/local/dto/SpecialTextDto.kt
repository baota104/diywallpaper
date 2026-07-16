package com.example.diywallpaper.data.local.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpecialTextDto(
    @SerialName("id")
    val id: Int,
    @SerialName("rank")
    val rank: Int,
    @SerialName("data")
    val data: String
)
