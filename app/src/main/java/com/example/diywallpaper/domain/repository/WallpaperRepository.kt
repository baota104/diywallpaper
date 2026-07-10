package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.WallpaperCategory
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    fun observeWallpaperCategories(): Flow<List<WallpaperCategory>>

    suspend fun refreshWallpaperCategories(): AppResult<Unit>

    suspend fun toggleFavorite(itemId: String): AppResult<Unit>
}
