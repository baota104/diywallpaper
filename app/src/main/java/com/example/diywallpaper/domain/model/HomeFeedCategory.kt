package com.example.diywallpaper.domain.model

data class HomeFeedCategory(
    val id: String,
    val title: String,
    val iconUrl: String?,
    val rank: Int,
    val items: List<HomeFeedItem>
)
