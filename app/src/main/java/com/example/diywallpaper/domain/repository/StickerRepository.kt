package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.StickerItem
import kotlinx.coroutines.flow.Flow

interface StickerRepository {
    fun observeStickers(): Flow<List<StickerItem>>

    suspend fun refreshStickers(): AppResult<Unit>
}
