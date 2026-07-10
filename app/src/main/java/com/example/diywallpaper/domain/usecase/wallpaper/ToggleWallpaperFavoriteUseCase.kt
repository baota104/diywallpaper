package com.example.diywallpaper.domain.usecase.wallpaper

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject

class ToggleWallpaperFavoriteUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository
) {
    suspend operator fun invoke(itemId: String): AppResult<Unit> {
        return wallpaperRepository.toggleFavorite(itemId)
    }
}
