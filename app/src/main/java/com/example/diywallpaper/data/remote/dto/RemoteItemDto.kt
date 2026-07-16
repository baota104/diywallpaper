package com.example.diywallpaper.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteItemDto(
    val id: Int = 0,
    val type: String? = null,
    val rank: Int = Int.MAX_VALUE,
    val thumb: String? = null,
    val data: String? = null,
    val preview: String? = null,
    val content: String? = null,
    @SerialName("data_zip")
    val dataZip: String? = null,
    @SerialName("diy_data")
    val diyData: String? = null,
    @SerialName("diy_animation")
    val diyAnimation: String? = null
)
