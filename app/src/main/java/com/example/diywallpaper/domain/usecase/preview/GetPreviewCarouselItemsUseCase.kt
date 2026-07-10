package com.example.diywallpaper.domain.usecase.preview

import com.example.diywallpaper.domain.model.preview.PreviewCarouselData
import com.example.diywallpaper.domain.model.preview.PreviewCatalogItem
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.domain.model.preview.toPreviewCatalogItem
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPreviewCarouselItemsUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val diyRepository: DiyRepository,
    private val backgroundCreateRepository: BackgroundCreateRepository
) {
    operator fun invoke(
        categoryId: String,
        initialItemId: String,
        sourceType: PreviewSourceType
    ): Flow<PreviewCarouselData> {
        return when (sourceType) {
            PreviewSourceType.WALLPAPER -> wallpaperRepository.observeWallpaperCategories().map { categories ->
                val category = categories.firstOrNull { it.id == categoryId }
                val items = category
                    ?.items
                    .orEmpty()
                    .map { it.toPreviewCatalogItem(categoryTitle = category?.name.orEmpty()) }
                    .sortedPreviewItems()
                items.toCarouselData(initialItemId)
            }

            PreviewSourceType.DIY -> diyRepository.observeDiyTemplates().map { templates ->
                templates
                    .map { it.toPreviewCatalogItem() }
                    .sortedPreviewItems()
                    .toCarouselData(initialItemId)
            }

            PreviewSourceType.CREATE_FROM_SCRATCH -> backgroundCreateRepository.observeBackgrounds().map { backgrounds ->
                backgrounds
                    .map { it.toPreviewCatalogItem() }
                    .sortedPreviewItems()
                    .toCarouselData(initialItemId)
            }
        }
    }
}

private fun List<PreviewCatalogItem>.sortedPreviewItems(): List<PreviewCatalogItem> {
    return sortedWith(compareBy<PreviewCatalogItem> { it.rank }.thenBy { it.id })
}

private fun List<PreviewCatalogItem>.toCarouselData(initialItemId: String): PreviewCarouselData {
    val initialIndex = indexOfFirst { it.id == initialItemId }
        .takeIf { it >= 0 }
        ?: 0
    return PreviewCarouselData(
        items = this,
        initialIndex = initialIndex
    )
}
