package com.example.diywallpaper.domain.usecase.sticker

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.repository.StickerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetStickersUseCase @Inject constructor(
    private val stickerRepository: StickerRepository
) {
    operator fun invoke(): Flow<List<StickerItem>> = stickerRepository.observeStickers()

    suspend fun refresh(): AppResult<Unit> = stickerRepository.refreshStickers()
}
