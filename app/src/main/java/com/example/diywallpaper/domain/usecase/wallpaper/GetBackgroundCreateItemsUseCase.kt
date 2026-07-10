package com.example.diywallpaper.domain.usecase.wallpaper

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetBackgroundCreateItemsUseCase @Inject constructor(
    private val backgroundCreateRepository: BackgroundCreateRepository
) {
    operator fun invoke(): Flow<List<BackgroundCreateItem>> = backgroundCreateRepository.observeBackgrounds()

    suspend fun refresh(): AppResult<Unit> = backgroundCreateRepository.refreshBackgrounds()
}
