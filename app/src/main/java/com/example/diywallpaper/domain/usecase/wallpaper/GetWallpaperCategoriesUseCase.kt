package com.example.diywallpaper.domain.usecase.wallpaper

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetWallpaperCategoriesUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository
) {
    operator fun invoke(): Flow<List<WallpaperCategory>> = wallpaperRepository.observeWallpaperCategories()

    suspend fun refresh(): AppResult<Unit> = wallpaperRepository.refreshWallpaperCategories()
}
