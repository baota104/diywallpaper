package com.example.diywallpaper.domain.usecase.home

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.HomeFeedCategory
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetHomeFeedUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val diyRepository: DiyRepository
) {
    operator fun invoke(): Flow<List<HomeFeedCategory>> {
        return combine(
            wallpaperRepository.observeWallpaperCategories(),
            diyRepository.observeDiyTemplates()
        ) { wallpaperCategories, diyTemplates ->
            buildHomeFeed(wallpaperCategories, diyTemplates)
        }
    }

    suspend fun refresh(): AppResult<Unit> = wallpaperRepository.refreshWallpaperCategories()
}

private fun buildHomeFeed(
    wallpaperCategories: List<WallpaperCategory>,
    diyTemplates: List<DiyTemplate>
): List<HomeFeedCategory> {
    val wallpaperFeedCategories = wallpaperCategories
        .sortedBy { it.rank }
        .map { category ->
            HomeFeedCategory(
                id = category.id,
                title = category.name,
                iconUrl = category.iconUrl,
                rank = category.rank,
                items = category.items
                    .sortedBy { it.rank }
                    .map(HomeFeedItem::WallpaperEntry)
            )
        }

    // Temporarily hide DIY from Home until its feed data is finalized for the grid experience.
    val diyCategory = null

    return buildList {
        diyCategory?.let(::add)
        addAll(wallpaperFeedCategories)
    }
}
