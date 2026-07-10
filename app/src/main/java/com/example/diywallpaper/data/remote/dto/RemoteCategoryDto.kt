package com.example.diywallpaper.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RemoteCategoryDto(
    val category: String = "",
    val rank: Int = Int.MAX_VALUE,
    val icon: String? = null,
    val items: List<RemoteItemDto> = emptyList()
)
