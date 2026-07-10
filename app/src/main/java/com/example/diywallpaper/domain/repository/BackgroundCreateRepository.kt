package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import kotlinx.coroutines.flow.Flow

interface BackgroundCreateRepository {
    fun observeBackgrounds(): Flow<List<BackgroundCreateItem>>

    suspend fun refreshBackgrounds(): AppResult<Unit>
}
