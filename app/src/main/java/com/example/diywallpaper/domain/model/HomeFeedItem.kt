package com.example.diywallpaper.domain.model

sealed interface HomeFeedItem {
    val id: String
    val categoryId: String
    val rank: Int

    data class WallpaperEntry(
        val wallpaper: WallpaperItem
    ) : HomeFeedItem {
        override val id: String = wallpaper.id
        override val categoryId: String = wallpaper.categoryId
        override val rank: Int = wallpaper.rank
    }

    data class DiyEntry(
        val template: DiyTemplate
    ) : HomeFeedItem {
        override val id: String = template.id
        override val categoryId: String = HOME_DIY_CATEGORY_ID
        override val rank: Int = template.rank
    }

    companion object {
        const val HOME_DIY_CATEGORY_ID = "DIY"
    }
}
