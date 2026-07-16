package com.example.diywallpaper.domain.usecase.collection

import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveFavoriteFeedItemsUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val diyRepository: DiyRepository
) {
    operator fun invoke(): Flow<List<HomeFeedItem>> {
        return combine(
            wallpaperRepository.observeWallpaperCategories(),
            diyRepository.observeDiyTemplates()
        ) { wallpaperCategories, diyTemplates ->
            val favoriteWallpapers = wallpaperCategories
                .flatMap { category -> category.items }
                .filter { item -> item.isFavorite }
                .map(HomeFeedItem::WallpaperEntry)

            val favoriteDiyTemplates = diyTemplates
                .filter { template -> template.isFavorite }
                .map(HomeFeedItem::DiyEntry)

            (favoriteWallpapers + favoriteDiyTemplates)
                .sortedWith(compareBy<HomeFeedItem> { it.rank }.thenBy { it.id })
        }
    }
}
